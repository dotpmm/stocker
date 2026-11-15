package com.dotpmm.stocker.domain.nlp

import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.domain.model.WatchStock
import kotlin.math.exp
import kotlin.math.max

/** Matches a story to watchlist companies by name, alias or ticker. Title match is strongest. */
class Linker(stocks: List<WatchStock>) {
    private class Entry(val ticker: String, val names: List<Regex>, val tickerRegex: Regex?)

    private val entries: List<Entry> = stocks.map { stock ->
        val aliases = (listOf(Names.clean(stock.name)) + stock.aliases).map { it.trim() }.filter { it.length >= 3 }.distinct()
        val names = aliases.map { boundary(it, ignoreCase = true) }
        val tickerRegex = if (stock.ticker.length >= 3) boundary(stock.ticker, ignoreCase = false) else null
        Entry(stock.ticker, names, tickerRegex)
    }

    private fun boundary(text: String, ignoreCase: Boolean): Regex {
        val pattern = "(?<![\\p{L}\\p{N}])" + Regex.escape(text) + "(?![\\p{L}\\p{N}])"
        return if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
    }

    /** ticker -> strength (1.0 title match, 0.6 summary-only match). */
    fun link(title: String, summary: String): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (entry in entries) {
            val inTitle = entry.names.any { it.containsMatchIn(title) } || entry.tickerRegex?.containsMatchIn(title) == true
            if (inTitle) {
                out[entry.ticker] = 1.0
                continue
            }
            val inSummary = summary.isNotEmpty() &&
                (entry.names.any { it.containsMatchIn(summary) } || entry.tickerRegex?.containsMatchIn(summary) == true)
            if (inSummary) out[entry.ticker] = 0.6
        }
        return out
    }
}

object Credibility {
    private val high = listOf(
        "economic times", "mint", "livemint", "business standard", "businessline", "hindu businessline", "moneycontrol",
        "cnbc", "reuters", "bloomberg", "financial express", "ndtv profit", "sebi", "rbi", "nse", "bse",
        "the hindu", "wall street journal", "financial times", "marketwatch", "barron", "associated press",
        "press trust of india", "pti", "ani", "bq prime", "business today", "forbes india", "the economic times",
    )
    private val medium = listOf(
        "times of india", "hindustan times", "india today", "zee business", "news18", "indian express",
        "firstpost", "deccan herald", "the print", "scroll", "outlook", "ndtv", "investing.com", "yahoo",
        "seeking alpha", "benzinga", "pulse", "tipranks", "stocktwits",
    )

    fun forPublisher(name: String): Double {
        val n = name.lowercase()
        if (high.any { n.contains(it) }) return 0.9
        if (medium.any { n.contains(it) }) return 0.7
        return 0.5
    }
}

object Impact {
    /** Credibility x match strength x recency, with negative stories weighted up. */
    fun score(credibility: Double, strength: Double, publishedAt: Long, now: Long, tone: Tone, extraSources: Int): Double {
        val ageHours = max(0.0, (now - publishedAt) / 3_600_000.0)
        val recency = 0.05 + exp(-ageHours / 30.0)
        val toneWeight = if (tone == Tone.Negative) 1.2 else 1.0
        val confirmation = 1.0 + 0.15 * extraSources.coerceAtMost(4)
        return credibility * strength * recency * toneWeight * confirmation
    }
}
