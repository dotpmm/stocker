package com.dotpmm.stocker.data

import android.content.Context
import com.dotpmm.stocker.data.db.LinkedArticle
import com.dotpmm.stocker.data.db.StockerDb
import com.dotpmm.stocker.data.net.ArticleReader
import com.dotpmm.stocker.data.net.FeedParser
import com.dotpmm.stocker.data.net.Http
import com.dotpmm.stocker.data.net.SourceKind
import com.dotpmm.stocker.data.net.Sources
import com.dotpmm.stocker.data.net.Yahoo
import com.dotpmm.stocker.domain.ai.AiSummary
import com.dotpmm.stocker.domain.ai.Gemini
import com.dotpmm.stocker.domain.ai.GeminiException
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.MarketIndex
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.Quote
import com.dotpmm.stocker.domain.model.RangeSeries
import com.dotpmm.stocker.domain.model.RefreshState
import com.dotpmm.stocker.domain.model.SourceHealth
import com.dotpmm.stocker.domain.model.StockRow
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.domain.model.ToneCounts
import com.dotpmm.stocker.domain.model.UpcomingEvent
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.domain.nlp.Credibility
import com.dotpmm.stocker.domain.nlp.Impact
import com.dotpmm.stocker.domain.nlp.TextUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

data class RangeSpec(val label: String, val range: String, val interval: String, val caption: String, val ttlMs: Long)

/** Single source of truth for the UI: watchlist, stored news, refresh engine, charts and AI summaries. */
class StockerStore(context: Context) {
    val prefs = AppPrefs(context)
    private val db = StockerDb(context)
    private val ingest = Ingest(db)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshLock = Mutex()
    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private val _watchlist = MutableStateFlow(prefs.loadWatchlist())
    val watchlist: StateFlow<List<WatchStock>> = _watchlist.asStateFlow()

    private val _refresh = MutableStateFlow(RefreshState())
    val refresh: StateFlow<RefreshState> = _refresh.asStateFlow()

    private val _version = MutableStateFlow(0)
    val dataVersion: StateFlow<Int> = _version.asStateFlow()

    private val _lastRefresh = MutableStateFlow(prefs.lastRefresh)
    val lastRefresh: StateFlow<Long> = _lastRefresh.asStateFlow()

    private fun bump() = _version.update { it + 1 }

    companion object {
        const val TONE_WINDOW_MS = 3L * 24 * 3_600_000
        const val NEWS_WINDOW_MS = 7L * 24 * 3_600_000
        const val KEEP_MS = 14L * 24 * 3_600_000
        const val STALE_MS = 2L * 3_600_000

        val ranges = listOf(
            RangeSpec("1D", "1d", "5m", "Today", 10 * 60_000L),
            RangeSpec("1W", "5d", "30m", "Past week", 60 * 60_000L),
            RangeSpec("1M", "1mo", "1d", "Past month", 6 * 3_600_000L),
            RangeSpec("3M", "3mo", "1d", "Past 3 months", 6 * 3_600_000L),
            RangeSpec("1Y", "1y", "1d", "Past year", 12 * 3_600_000L),
        )
    }

    // ---- watchlist ----

    fun stockOf(ticker: String): WatchStock? = _watchlist.value.firstOrNull { it.ticker == ticker }

    fun isWatched(ticker: String): Boolean = stockOf(ticker) != null

    fun add(stock: WatchStock) {
        if (isWatched(stock.ticker)) return
        val updated = _watchlist.value + stock
        _watchlist.value = updated
        prefs.saveWatchlist(updated)
        bump()
        // The very first add also pulls indices and the shared feeds, so the watchlist is complete at once.
        launchRefresh(only = if (prefs.lastRefresh == 0L) null else stock.ticker)
    }

    suspend fun remove(ticker: String) {
        val updated = _watchlist.value.filter { it.ticker != ticker }
        _watchlist.value = updated
        prefs.saveWatchlist(updated)
        withContext(Dispatchers.IO) { db.removeStockData(ticker) }
        bump()
    }

    // ---- queries for the UI ----

    private fun today(): Long = LocalDate.now(zone).toEpochDay()

    private fun collapse(rows: List<LinkedArticle>, now: Long): List<NewsItem> {
        val byArticle = LinkedHashMap<String, MutableList<LinkedArticle>>()
        for (r in rows) byArticle.getOrPut(r.article.id) { ArrayList() }.add(r)
        val byCluster = LinkedHashMap<String, MutableList<List<LinkedArticle>>>()
        for (links in byArticle.values) byCluster.getOrPut(links[0].article.cluster) { ArrayList() }.add(links)
        return byCluster.values.map { group ->
            val rep = group.maxWith(compareBy<List<LinkedArticle>>({ it[0].article.credibility }, { it[0].article.publishedAt }))
            val a = rep[0].article
            val sources = group.map { it[0].article.source }.distinct().filter { it != a.source }
            val tickers = group.flatMap { g -> g.map { it.ticker } }.distinct()
            val strength = rep.maxOf { it.strength }
            NewsItem(a, tickers, sources, Impact.score(a.credibility, strength, a.publishedAt, now, a.tone, sources.size))
        }.sortedByDescending { it.article.publishedAt }
    }

    fun toneCounts(items: List<NewsItem>, now: Long = System.currentTimeMillis()): ToneCounts {
        val recent = items.filter { now - it.article.publishedAt <= TONE_WINDOW_MS }
        return ToneCounts(
            positive = recent.count { it.article.tone == Tone.Positive },
            neutral = recent.count { it.article.tone == Tone.Neutral },
            negative = recent.count { it.article.tone == Tone.Negative },
        )
    }

    private fun overallTone(c: ToneCounts): Tone {
        val net = c.positive - c.negative
        val threshold = maxOf(1, (c.total * 0.2).toInt())
        return when {
            net >= threshold -> Tone.Positive
            net <= -threshold -> Tone.Negative
            else -> Tone.Neutral
        }
    }

    fun toneLabel(c: ToneCounts): String = overallTone(c).name.lowercase()

    suspend fun watchRows(): List<StockRow> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val byTicker = db.linked(null, now - TONE_WINDOW_MS, 4000).groupBy { it.ticker }
        _watchlist.value.map { stock ->
            val items = collapse(byTicker[stock.ticker].orEmpty(), now)
            val quote = if (stock.market == Market.India) {
                db.quote(stock.symbol)?.let { Quote(it.price, it.changePct, it.trend) }
            } else {
                null
            }
            StockRow(stock, quote, items.size, overallTone(toneCounts(items, now)))
        }
    }

    suspend fun quoteOf(stock: WatchStock): Quote? = withContext(Dispatchers.IO) {
        db.quote(stock.symbol)?.let { Quote(it.price, it.changePct, it.trend) }
    }

    suspend fun indices(): List<MarketIndex> = withContext(Dispatchers.IO) {
        listOfNotNull(
            db.quote("^NSEI")?.let { MarketIndex("NIFTY 50", it.price, it.changePct) },
            db.quote("^BSESN")?.let { MarketIndex("SENSEX", it.price, it.changePct) },
        )
    }

    suspend fun newsFor(ticker: String, top: Boolean): List<NewsItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val items = collapse(db.linked(ticker, now - NEWS_WINDOW_MS, 600), now)
        if (top) items.sortedByDescending { it.impact } else items
    }

    suspend fun allNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        collapse(db.linked(null, now - NEWS_WINDOW_MS, 2500), now).take(200)
    }

    suspend fun eventsUpcoming(): List<UpcomingEvent> = withContext(Dispatchers.IO) {
        db.events(null, today(), today() + 120)
    }

    suspend fun eventsFor(ticker: String): List<UpcomingEvent> = withContext(Dispatchers.IO) {
        db.events(ticker, today(), today() + 120)
    }

    suspend fun article(id: String): Article? = withContext(Dispatchers.IO) { db.article(id) }

    suspend fun clusterMembers(article: Article): List<Article> = withContext(Dispatchers.IO) {
        db.clusterMembers(article.cluster).filter { it.id != article.id }
    }

    suspend fun health(): List<SourceHealth> = withContext(Dispatchers.IO) { db.health() }

    suspend fun clearNews() {
        withContext(Dispatchers.IO) { db.clearNews() }
        bump()
    }

    suspend fun readBody(article: Article): List<String>? = withContext(Dispatchers.IO) {
        try {
            ArticleReader.fetch(article.url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    // ---- charts ----

    suspend fun series(stock: WatchStock, spec: RangeSpec): RangeSeries? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val key = "${stock.symbol}|${spec.range}|${spec.interval}"
        fun parse(body: String): RangeSeries? = try {
            val o = JSONObject(body)
            val arr = o.getJSONArray("points")
            RangeSeries(spec.label, spec.caption, o.getDouble("abs"), o.getDouble("pct"), (0 until arr.length()).map { arr.getDouble(it).toFloat() })
        } catch (e: Exception) {
            null
        }
        db.cacheGet("chart_cache", key, now - spec.ttlMs)?.let { cached -> parse(cached)?.let { return@withContext it } }
        try {
            val data = Yahoo.chart(stock.symbol, spec.range, spec.interval)
            if (data.closes.size < 2) throw IllegalStateException("Not enough data")
            val first = if (spec.range == "1d") (data.previousClose ?: data.closes.first()) else data.closes.first()
            val abs = data.price - first
            val pct = if (first != 0.0) abs / first * 100 else 0.0
            val step = maxOf(1, data.closes.size / 120)
            val points = data.closes.filterIndexed { i, _ -> i % step == 0 || i == data.closes.lastIndex }.map { it.toFloat() }
            val body = JSONObject().put("abs", abs).put("pct", pct).put("points", JSONArray(points.map { it.toDouble() })).toString()
            db.cachePut("chart_cache", key, body, now)
            RangeSeries(spec.label, spec.caption, abs, pct, points)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            db.cacheGet("chart_cache", key, 0L)?.let { parse(it) }
        }
    }

    // ---- AI ----

    fun hasGeminiKey(): Boolean = prefs.geminiKey.isNotBlank()

    suspend fun aiSummary(ticker: String): Pair<AiSummary, List<Article>> {
        val stock = stockOf(ticker) ?: throw GeminiException("Stock not found.")
        val key = prefs.geminiKey
        if (key.isBlank()) throw GeminiException("Add your Gemini key in Settings first.")
        val now = System.currentTimeMillis()
        val items = newsFor(ticker, top = true).take(25)
        if (items.isEmpty()) throw GeminiException("No recent news to summarise yet. Refresh first.")
        val articles = items.map { it.article }
        val tone = toneLabel(toneCounts(items, now))
        val cacheKey = TextUtil.hash(ticker + "|" + prefs.geminiModel + "|" + articles.joinToString(",") { it.id })
        val cached = withContext(Dispatchers.IO) { db.cacheGet("ai_cache", cacheKey, now - 24L * 3_600_000) }
        if (cached != null) {
            return try {
                Gemini.parse(cached, "cached", articles.map { it.id }) to articles
            } catch (e: GeminiException) {
                fresh(stock, key, articles, tone, now, cacheKey)
            }
        }
        return fresh(stock, key, articles, tone, now, cacheKey)
    }

    private suspend fun fresh(
        stock: WatchStock, key: String, articles: List<Article>, tone: String, now: Long, cacheKey: String,
    ): Pair<AiSummary, List<Article>> {
        val summary = Gemini.summarize(key, prefs.geminiModel, stock.name, stock.ticker, articles, tone, now)
        withContext(Dispatchers.IO) { db.cachePut("ai_cache", cacheKey, Gemini.toJson(summary), now) }
        return summary to articles
    }

    // ---- refresh engine ----

    fun launchRefresh(only: String? = null) {
        scope.launch { refresh(only) }
    }

    fun refreshIfStale() {
        if (_watchlist.value.isEmpty()) return
        if (System.currentTimeMillis() - prefs.lastRefresh > STALE_MS) launchRefresh()
    }

    suspend fun refresh(only: String? = null) {
        if (!refreshLock.tryLock()) return
        try {
            runRefresh(only)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _refresh.value = RefreshState(false, "", e.message ?: "Refresh failed")
        } finally {
            refreshLock.unlock()
        }
    }

    private suspend fun runRefresh(only: String?) {
        val all = _watchlist.value
        val targets = if (only == null) all else all.filter { it.ticker == only }
        _refresh.value = RefreshState(true, "Prices")
        refreshQuotes(targets, includeIndices = only == null)
        bump()

        val candidates = ConcurrentLinkedQueue<Candidate>()
        val okCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val companyItems = AtomicInteger(0)
        val companyOk = AtomicInteger(0)
        val companyFail = AtomicInteger(0)
        val companyError = AtomicReference<String?>(null)

        val hasIndia = targets.any { it.market == Market.India }
        val hasUs = targets.any { it.market == Market.US }
        val tasks = ArrayList<suspend () -> Unit>()

        if (only == null) {
            for (src in Sources.generic) {
                if (src.market == Market.India && !hasIndia) continue
                if (src.market == Market.US && !hasUs) continue
                tasks.add {
                    try {
                        val response = Http.get(src.url, gateMs = if (src.kind == SourceKind.GoogleNews) 1500L else 600L)
                        if (!response.ok) throw java.io.IOException("HTTP ${response.code}")
                        val items = FeedParser.parse(response.body)
                        for (raw in items) {
                            val name = if (src.kind == SourceKind.GoogleNews) (raw.source ?: src.name) else src.name
                            val cred = if (src.kind == SourceKind.GoogleNews) Credibility.forPublisher(name) else src.credibility
                            candidates.add(Candidate(raw, name, cred, null))
                        }
                        db.recordHealth(src.id, src.name, true, items.size, null, System.currentTimeMillis())
                        okCount.incrementAndGet()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        db.recordHealth(src.id, src.name, false, 0, shortError(e), System.currentTimeMillis())
                        failCount.incrementAndGet()
                    }
                }
            }
        }
        for (stock in targets) {
            tasks.add {
                try {
                    val response = Http.get(Sources.companyUrl(stock.name, stock.market), gateMs = 1500L)
                    if (!response.ok) throw java.io.IOException("HTTP ${response.code}")
                    val items = FeedParser.parse(response.body)
                    for (raw in items) {
                        val name = raw.source ?: "Google News"
                        candidates.add(Candidate(raw, name, Credibility.forPublisher(name), stock.ticker))
                    }
                    companyItems.addAndGet(items.size)
                    companyOk.incrementAndGet()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    companyFail.incrementAndGet()
                    companyError.set(shortError(e))
                }
            }
        }

        val total = tasks.size
        val done = AtomicInteger(0)
        val semaphore = Semaphore(4)
        withTimeoutOrNull(5 * 60_000L) {
            coroutineScope {
                tasks.map { task ->
                    async {
                        semaphore.withPermit {
                            task()
                            _refresh.value = RefreshState(true, "News ${done.incrementAndGet()}/$total")
                        }
                    }
                }.awaitAll()
            }
        }

        if (targets.isNotEmpty()) {
            val ok = companyOk.get() > 0
            db.recordHealth(
                Sources.COMPANY_SOURCE_ID, Sources.COMPANY_SOURCE_NAME, ok, companyItems.get(),
                if (ok) null else (companyError.get() ?: "No response"), System.currentTimeMillis(),
            )
            if (ok) okCount.incrementAndGet() else failCount.incrementAndGet()
        }

        _refresh.value = RefreshState(true, "Sorting")
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            ingest.process(candidates.toList(), all, now)
            db.prune(now - KEEP_MS, today() - 7)
        }
        if (only == null && okCount.get() > 0) {
            prefs.lastRefresh = now
            _lastRefresh.value = now
        }
        bump()
        val error = if (okCount.get() == 0 && failCount.get() > 0) "Couldn't reach any news source. Showing saved news." else null
        _refresh.value = RefreshState(false, "", error)
    }

    private fun shortError(e: Exception): String {
        val message = e.message ?: e.javaClass.simpleName
        return message.take(80)
    }

    private suspend fun refreshQuotes(stocks: List<WatchStock>, includeIndices: Boolean) {
        val symbols = stocks.filter { it.market == Market.India }.map { it.symbol } +
            (if (includeIndices) listOf("^NSEI", "^BSESN") else emptyList())
        if (symbols.isEmpty()) return
        val ok = AtomicInteger(0)
        val error = AtomicReference<String?>(null)
        val semaphore = Semaphore(4)
        coroutineScope {
            symbols.map { symbol ->
                async {
                    semaphore.withPermit {
                        try {
                            val data = Yahoo.chart(symbol, "1mo", "1d")
                            val closes = data.closes
                            val lastBarDay = data.times.lastOrNull()?.let { Instant.ofEpochSecond(it).atZone(zone).toLocalDate() }
                            val quoteDay = data.marketTime?.let { Instant.ofEpochSecond(it).atZone(zone).toLocalDate() }
                            val previous: Double? = when {
                                closes.size >= 2 && lastBarDay != null && lastBarDay == quoteDay -> closes[closes.size - 2]
                                closes.isNotEmpty() && lastBarDay != null && quoteDay != null -> closes.last()
                                closes.size >= 2 -> closes[closes.size - 2]
                                else -> data.previousClose
                            }
                            val change = if (previous != null && previous != 0.0) (data.price - previous) / previous * 100 else 0.0
                            val trend = closes.takeLast(22).map { it.toFloat() }
                            withContext(Dispatchers.IO) { db.saveQuote(symbol, data.price, change, trend, System.currentTimeMillis()) }
                            ok.incrementAndGet()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error.set(shortError(e))
                        }
                    }
                }
            }.awaitAll()
        }
        withContext(Dispatchers.IO) {
            db.recordHealth(
                Sources.PRICES_SOURCE_ID, Sources.PRICES_SOURCE_NAME, ok.get() > 0, ok.get(),
                if (ok.get() > 0) null else (error.get() ?: "No response"), System.currentTimeMillis(),
            )
        }
    }
}
