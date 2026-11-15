package com.dotpmm.stocker.data

import com.dotpmm.stocker.data.db.StockerDb
import com.dotpmm.stocker.data.net.RawItem
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.domain.nlp.EventExtractor
import com.dotpmm.stocker.domain.nlp.EventTagger
import com.dotpmm.stocker.domain.nlp.Linker
import com.dotpmm.stocker.domain.nlp.Sentiment
import com.dotpmm.stocker.domain.nlp.TextUtil

/** A fetched item with the context needed to ingest it. */
data class Candidate(
    val raw: RawItem,
    val sourceName: String,
    val credibility: Double,
    /** Ticker whose per-company query returned this item, if any. */
    val owner: String?,
)

/**
 * The rule-based pipeline: normalise, link to companies, tag, score tone, cluster duplicates, store.
 * No AI is involved anywhere in this class.
 */
class Ingest(private val db: StockerDb) {
    private class Known(val id: String, var cluster: String, val tokens: Set<String>, val publishedAt: Long)

    private val dedupeWindowMs = 48L * 3_600_000L
    private val maxAgeMs = 14L * 24L * 3_600_000L

    /** Returns the number of new articles stored. */
    fun process(candidates: List<Candidate>, stocks: List<WatchStock>, now: Long): Int {
        if (candidates.isEmpty() || stocks.isEmpty()) return 0
        val linker = Linker(stocks)
        val known = ArrayList<Known>()
        for (k in db.recentForDedupe(now - 4L * 24L * 3_600_000L)) {
            known.add(Known(k.id, k.cluster, TextUtil.tokens(TextUtil.normalizeTitle(k.title)), k.publishedAt))
        }
        var added = 0
        db.inTransaction {
            for (c in candidates.sortedBy { it.raw.publishedAt }) {
                val published = minOf(c.raw.publishedAt, now + 3_600_000L)
                if (now - published > maxAgeMs) continue

                val title = cleanTitle(TextUtil.stripHtml(c.raw.title), c.sourceName, c.raw.source)
                if (title.length < 15) continue
                val summary = cleanSummary(TextUtil.stripHtml(c.raw.summaryHtml), title)
                val links = HashMap(linker.link(title, summary))
                if (c.owner != null && (links[c.owner] ?: 0.0) < 0.5) links[c.owner] = 0.5
                if (links.isEmpty()) continue

                val id = TextUtil.hash(TextUtil.canonicalUrl(c.raw.link))
                if (db.hasArticle(id)) {
                    for ((ticker, strength) in links) db.upsertLink(id, ticker, strength)
                    continue
                }

                val tokens = TextUtil.tokens(TextUtil.normalizeTitle(title))
                var cluster = id
                if (tokens.size >= 4) {
                    var best = 0.0
                    for (k in known) {
                        if (kotlin.math.abs(k.publishedAt - published) > dedupeWindowMs) continue
                        val score = TextUtil.similarity(tokens, k.tokens)
                        if (score > 0.0 && score > best) {
                            best = score
                            cluster = k.cluster
                        }
                    }
                }

                val article = Article(
                    id = id,
                    title = title,
                    summary = summary,
                    url = c.raw.link,
                    source = c.sourceName,
                    publishedAt = published,
                    tag = EventTagger.tag(title, summary),
                    tone = Sentiment.analyze(title, summary),
                    credibility = c.credibility,
                    cluster = cluster,
                )
                db.insertArticle(article, now)
                known.add(Known(id, cluster, tokens, published))
                for ((ticker, strength) in links) {
                    db.upsertLink(id, ticker, strength)
                }
                val event = EventExtractor.extract(title, summary, published)
                if (event != null) {
                    // Attach the event to the strongest-linked company only.
                    val ticker = links.maxByOrNull { it.value }!!.key
                    db.upsertEvent(
                        id = TextUtil.hash("$ticker|${event.first}|${event.second}"),
                        ticker = ticker,
                        epochDay = event.second,
                        label = event.first,
                        articleId = id,
                    )
                }
                added++
            }
        }
        return added
    }

    /** Google News titles end with " - Publisher"; remove it so headlines match across outlets. */
    private fun cleanTitle(title: String, sourceName: String, rawSource: String?): String {
        var out = title
        for (suffix in listOfNotNull(rawSource, sourceName)) {
            for (separator in listOf(" - ", " | ", " – ", " — ")) {
                if (out.endsWith(separator + suffix, ignoreCase = true)) {
                    out = out.dropLast(separator.length + suffix.length).trim()
                }
            }
        }
        return out
    }

    /** Feeds often repeat the headline as the description; drop that. Cap length for storage. */
    private fun cleanSummary(summary: String, title: String): String {
        if (summary.isEmpty()) return ""
        val s = TextUtil.normalizeTitle(summary)
        val t = TextUtil.normalizeTitle(title)
        if (s.startsWith(t) && s.length < t.length + 40) return ""
        return if (summary.length > 700) summary.take(700).trimEnd() + "…" else summary
    }
}
