package com.dotpmm.stocker.research

import android.content.Context
import com.dotpmm.stocker.data.db.StockerDb
import com.dotpmm.stocker.data.net.YahooResearch
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** One sector card on the macro screen. */
data class SectorCard(
    val id: String,
    val name: String,
    val peCurrent: Double?,
    val pe5y: Double,
    val yearChangePct: Double?,
    val badge: Verdict,
    val nCompanies: Int,
    val blurb: String,
    val sparkline: List<Float>,
)

/** Hero index payload. */
data class Hero(val value: Double?, val changePct: Double?, val sparkline: List<Float>)

/** One stock tile in the market heatmap. [mcapCr] drives the tile area. */
data class HeatmapStock(
    val symbol: String,
    val name: String,
    val price: Double?,
    val changePct: Double?,
    val volume: Long?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val mcapCr: Double?,
)

/** A heatmap group — the NSE industry label plus its constituents. */
data class HeatmapSector(
    val name: String,
    val stocks: List<HeatmapStock>,
) {
    val totalMcapCr: Double
        get() = stocks.sumOf { it.mcapCr ?: 0.0 }

    /** Market-cap weighted day move, used for the group header. */
    val changePct: Double?
        get() {
            if (stocks.isEmpty()) return null
            val weights = stocks.sumOf { it.mcapCr ?: 0.0 }
            if (weights <= 0.0) {
                val changes = stocks.mapNotNull { it.changePct }
                return if (changes.isEmpty()) null else changes.average()
            }
            val weighted = stocks.sumOf { (it.mcapCr ?: 0.0) * (it.changePct ?: 0.0) }
            return weighted / weights
        }
}

data class MacroSnapshot(
    val hero: Hero,
    val sectors: List<SectorCard>,
    val dependencies: List<Dependency>,
    /** Non-null when a live refresh failed but cached/stale data is on screen. */
    val staleNote: String? = null,
)

/** A row of the sector drill-down table. */
data class CompanyRow(
    val symbol: String,
    val name: String,
    val pe: Double?,
    val pe5yOwn: Double?,
    val peBelowOwnAvg: Boolean,
    val mcapCr: Double?,
    val revCagr5y: Double?,
    val moat: String,
    val monopoly: Boolean,
    val cultural: Boolean,
    val defensive: Boolean,
    val note: String,
    val curated: Boolean,
) {
    val isCore: Boolean get() = monopoly || cultural || defensive
}

data class SectorDetail(
    val id: String,
    val name: String,
    val pe5yAvg: Double,
    val blurb: String,
    val sparkline: List<Float>,
    val companies: List<CompanyRow>,
    val nCurated: Int,
    val nUniverse: Int,
)

/** The full deep-dive payload for one symbol. */
data class StockDeep(
    val symbol: String,
    val name: String,
    val sector: String,
    val sectorId: String?,
    val curated: Boolean,
    val note: String,
    val ltp: Double?,
    val chgPct: Double?,
    val pe: Double?,
    val pe5yOwn: Double?,
    val peSectorAvg: Double,
    val mcapCr: Double?,
    val revCagr5y: Double?,
    val moat: String,
    val monopoly: Boolean,
    val cultural: Boolean,
    val defensive: Boolean,
    val narrative: String,
    val reality: Reality,
    val priceHistory: List<Point>,
    val earningsDots: List<EarningsDot>,
    val sparkline: List<Float>,
)

data class SearchResult(val symbol: String, val name: String, val sector: String, val sectorId: String?)

/** A chart series for one range chip. */
data class StockSeries(val range: String, val points: List<Point>, val dots: List<EarningsDot>) {
    val intraday: Boolean get() = points.firstOrNull()?.intraday == true
}

/** TTLs are the shipped values from FUNCTIONALITY.md §4. */
private object Ttl {
    const val QUOTE = 120_000L
    const val PROFILE = 12 * 3_600_000L
    const val FINANCIALS = 24 * 3_600_000L
    const val EARNINGS = 24 * 3_600_000L
    const val INDEX_HISTORY = 12 * 3_600_000L
    const val STOCK_1Y = 6 * 3_600_000L
    const val STOCK_5Y = 7 * 3_600_000L
    const val SECTOR_PE = 6 * 3_600_000L
    const val HEATMAP = 5 * 60_000L
    const val H5D = 15 * 60_000L
    const val H1M = 30 * 60_000L
    const val H6M = 90 * 60_000L
    const val H1Y = 6 * 3_600_000L
}

private val rangeTtl = mapOf("5d" to Ttl.H5D, "1m" to Ttl.H1M, "6m" to Ttl.H6M, "1y" to Ttl.H1Y)

/**
 * Research data access. Fetches through an in-memory + SQLite TTL cache; on a network failure the
 * stale cached copy is served so the UI keeps working through Yahoo outages (FUNCTIONALITY.md §4).
 */
class ResearchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = StockerDb(appContext)
    private val curated: CuratedData by lazy { ResearchAssets.curated(appContext) }
    private val universe: List<UniverseStock> by lazy { ResearchAssets.universe(appContext) }

    private val memo = ConcurrentHashMap<String, Pair<Long, String>>()
    private val gate = Semaphore(6)

    fun data(): CuratedData = curated

    fun universeStocks(): List<UniverseStock> = universe

    // ── Cache primitives ─────────────────────────────────────────

    private suspend fun cached(key: String, ttlMs: Long, fetch: suspend () -> String?): String? =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            memo[key]?.let { (at, body) -> if (now - at < ttlMs) return@withContext body }
            db.cacheGet("research_cache", key, now - ttlMs)?.let { body ->
                memo[key] = now to body
                return@withContext body
            }
            val fresh = try {
                gate.withPermit { fetch() }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (fresh != null) {
                memo[key] = now to fresh
                db.cachePut("research_cache", key, fresh, now)
                return@withContext fresh
            }
            // Stale serve: any age is better than an empty screen.
            db.cacheGet("research_cache", key, 0L)?.also { memo[key] = now to it }
        }

    // ── Macro screen ─────────────────────────────────────────────

    /**
     * Screen 1 payload. Never throws: on a total failure the snapshot carries an explanatory note
     * instead of leaving the UI to render a silent wall of em dashes.
     */
    suspend fun macro(): MacroSnapshot = coroutineScope {
        val cards = curated.sectors.map { sector ->
            async { sectorCard(sector) }
        }.awaitAll().filterNotNull()

        val indexHistory = cachedJson("idx:^NSEI", Ttl.INDEX_HISTORY) { fetchHistoryJson("^NSEI", "1y", "1d") }
        val points = parsePoints(indexHistory)
        val hero = Hero(
            value = points.lastOrNull()?.c,
            changePct = ResearchMath.heroChange(points),
            sparkline = points.takeLast(60).map { it.c.toFloat() },
        )

        val nothingLoaded = cards.isEmpty() && hero.value == null
        val partial = cards.size < curated.sectors.size
        val note = when {
            nothingLoaded ->
                "Couldn't reach the market data. Check your connection — cached values will " +
                    "reappear as soon as one request succeeds."
            partial -> "Some sectors are showing cached data."
            else -> null
        }
        MacroSnapshot(
            hero = hero,
            sectors = cards,
            dependencies = curated.dependencies,
            staleNote = note,
        )
    }

    private suspend fun sectorCard(sector: CuratedSector): SectorCard? {
        val history = cachedJson("idx:${sector.indexSymbol}", Ttl.INDEX_HISTORY) {
            fetchHistoryJson(sector.indexSymbol, "1y", "1d")
        }
        val points = parsePoints(history).takeLast(120)

        val peNow = cachedJson("sectorpe:${sector.id}", Ttl.SECTOR_PE) {
            // One batched call for the constituents instead of one request each.
            val quotes = YahooResearch.quotes(sector.companies.map { it.symbol })
            val pairs = sector.companies.map { co ->
                Triple(co.symbol, quotes[co.symbol]?.pe, quotes[co.symbol]?.mcap)
            }
            val pe = ResearchMath.weightedPe(pairs)
            if (pe == null) null else "{\"pe\":$pe}"
        }?.let { runCatching { JSONObjectBody(it).double("pe") }.getOrNull() }

        return SectorCard(
            id = sector.id,
            name = sector.name,
            peCurrent = peNow,
            pe5y = sector.pe5yAvg,
            yearChangePct = ResearchMath.yearChange(points),
            badge = ResearchMath.badge(peNow, sector.pe5yAvg),
            nCompanies = sector.companies.size,
            blurb = sector.blurb,
            sparkline = points.map { it.c.toFloat() },
        )
    }

    // ── Market heatmap ───────────────────────────────────────────

    /**
     * The Nifty-200 universe grouped by NSE industry. The batch quote call returns market cap,
     * day change, volume and the day range, so one cached pass feeds the whole treemap.
     */
    suspend fun heatmap(): List<HeatmapSector> {
        val body = cachedJson("heatmap:v1", Ttl.HEATMAP) { fetchHeatmapJson() }
        return parseHeatmap(body)
    }

    /** A light 5-day intraday sparkline for the heatmap popover — no earnings or fundamentals. */
    suspend fun sparkline(symbolRaw: String): List<Float> {
        val symbol = symbolRaw.uppercase()
        val body = cachedJson("spark5d:$symbol", Ttl.H5D) { fetchHistoryJson(symbol, "5d", "15m") }
        return parsePoints(body).map { it.c.toFloat() }
    }

    private suspend fun fetchHeatmapJson(): String? {
        val listed = universe.filter { it.industry.isNotBlank() }
        if (listed.isEmpty()) return null
        val quotes = runCatching { YahooResearch.quotes(listed.map { it.symbol }) }
            .getOrDefault(emptyMap())
        if (quotes.isEmpty()) return null

        val sectors = listed.groupBy { it.industry }.mapNotNull { (industry, members) ->
            val stocks = members.mapNotNull { entry ->
                val quote = quotes[entry.symbol] ?: return@mapNotNull null
                val change = quote.changePct?.takeIf { it.isFinite() } ?: return@mapNotNull null
                HeatmapStock(
                    symbol = entry.symbol,
                    name = entry.name,
                    price = quote.price?.takeIf { it.isFinite() },
                    changePct = change,
                    volume = quote.volume,
                    dayHigh = quote.dayHigh?.takeIf { it.isFinite() },
                    dayLow = quote.dayLow?.takeIf { it.isFinite() },
                    mcapCr = quote.mcap?.takeIf { it.isFinite() && it > 0 }?.div(1e7),
                )
            }.sortedByDescending { it.mcapCr ?: 0.0 }
            if (stocks.size < 2) null else HeatmapSector(industry, stocks)
        }.sortedByDescending { it.totalMcapCr }
        if (sectors.isEmpty()) return null

        val root = org.json.JSONObject()
        val sectorArray = org.json.JSONArray()
        for (sector in sectors) {
            val stockArray = org.json.JSONArray()
            for (stock in sector.stocks) {
                val o = org.json.JSONObject()
                    .put("symbol", stock.symbol)
                    .put("name", stock.name)
                    .put("chg", stock.changePct)
                    .put("mcap", stock.mcapCr ?: 0.0)
                stock.price?.let { o.put("ltp", it) }
                stock.volume?.let { o.put("vol", it) }
                stock.dayHigh?.let { o.put("high", it) }
                stock.dayLow?.let { o.put("low", it) }
                stockArray.put(o)
            }
            sectorArray.put(
                org.json.JSONObject().put("name", sector.name).put("stocks", stockArray),
            )
        }
        root.put("sectors", sectorArray)
        return root.toString()
    }

    private fun parseHeatmap(body: String?): List<HeatmapSector> {
        if (body == null) return emptyList()
        fun number(o: org.json.JSONObject, key: String): Double? =
            if (o.has(key) && !o.isNull(key)) o.optDouble(key).takeIf { it.isFinite() } else null
        return runCatching {
            val arr = org.json.JSONObject(body).getJSONArray("sectors")
            (0 until arr.length()).mapNotNull { i ->
                val sector = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = sector.optString("name")
                val rows = sector.optJSONArray("stocks") ?: org.json.JSONArray()
                val stocks = (0 until rows.length()).mapNotNull { j ->
                    val o = rows.optJSONObject(j) ?: return@mapNotNull null
                    HeatmapStock(
                        symbol = o.optString("symbol"),
                        name = o.optString("name"),
                        price = number(o, "ltp"),
                        changePct = number(o, "chg"),
                        volume = if (o.has("vol") && !o.isNull("vol")) o.optLong("vol") else null,
                        dayHigh = number(o, "high"),
                        dayLow = number(o, "low"),
                        mcapCr = number(o, "mcap")?.takeIf { it > 0 },
                    )
                }
                if (name.isBlank() || stocks.isEmpty()) null else HeatmapSector(name, stocks)
            }
        }.getOrDefault(emptyList())
    }

    // ── Sector drill-down ────────────────────────────────────────

    suspend fun sector(id: String): SectorDetail? {
        val sector = curated.byId[id] ?: return null
        val history = cachedJson("idx:${sector.indexSymbol}", Ttl.INDEX_HISTORY) {
            fetchHistoryJson(sector.indexSymbol, "1y", "1d")
        }
        val sparkline = parsePoints(history)

        val curatedSymbols = sector.companies.map { it.symbol }.toSet()
        val peers = universe.filter {
            CuratedData.INDUSTRY_MAP[it.industry] == id && it.symbol !in curatedSymbols
        }

        val quotes = runCatching { YahooResearch.quotes(sector.companies.map { it.symbol } + peers.map { it.symbol }) }
            .getOrDefault(emptyMap())

        val rows = coroutineScope {
            val curatedRows = sector.companies.map { co ->
                async { rowFor(co.symbol, co.name, quotes, curatedEntry = co) }
            }
            val peerRows = peers.map { u ->
                async { rowFor(u.symbol, quotes[u.symbol]?.name ?: u.name, quotes, curatedEntry = null) }
            }
            (curatedRows + peerRows).awaitAll().filterNotNull()
        }.sortedByDescending { it.mcapCr ?: 0.0 }

        return SectorDetail(
            id = sector.id,
            name = sector.name,
            pe5yAvg = sector.pe5yAvg,
            blurb = sector.blurb,
            sparkline = sparkline.map { it.c.toFloat() },
            companies = rows,
            nCurated = sector.companies.size,
            nUniverse = rows.size,
        )
    }

    private suspend fun rowFor(
        symbol: String,
        fallbackName: String,
        quotes: Map<String, YahooResearch.QuoteRow>,
        curatedEntry: CuratedCompany?,
    ): CompanyRow? {
        val quote = quotes[symbol]
        val profileBody = cachedJson("info:$symbol", Ttl.PROFILE) { fetchProfileJson(symbol) }
        val profile = ProfileBody.parse(profileBody)
        val pe = quote?.pe ?: profile?.pe
        val mcap = quote?.mcap ?: profile?.mcap
        val own = own5yPe(symbol)
        return CompanyRow(
            symbol = symbol,
            name = curatedEntry?.name ?: profile?.name ?: quote?.name ?: fallbackName,
            pe = pe,
            pe5yOwn = own,
            peBelowOwnAvg = pe != null && own != null && pe < own,
            mcapCr = mcap?.let { it / 1e7 },
            revCagr5y = financialsCagr(symbol),
            moat = curatedEntry?.moat ?: "None",
            monopoly = curatedEntry?.monopoly ?: false,
            cultural = curatedEntry?.cultural ?: false,
            defensive = curatedEntry?.defensive ?: false,
            note = curatedEntry?.note ?: "",
            curated = curatedEntry != null,
        )
    }

    // ── Deep-dive ────────────────────────────────────────────────

    suspend fun stock(symbolRaw: String): StockDeep {
        val symbol = symbolRaw.uppercase()
        val found = curated.find(symbol)
        val infoBody = cachedJson("info:$symbol", Ttl.PROFILE) { fetchProfileJson(symbol) }
        val profile = ProfileBody.parse(infoBody)

        val sectorName: String
        val sectorId: String?
        val sectorAvg: Double
        val moat: String
        val monopoly: Boolean
        val cultural: Boolean
        val defensive: Boolean
        val note: String
        val name: String
        val narrative: String

        if (found != null) {
            val (sector, co) = found
            sectorName = sector.name
            sectorId = sector.id
            sectorAvg = sector.pe5yAvg
            moat = co.moat
            monopoly = co.monopoly
            cultural = co.cultural
            defensive = co.defensive
            note = co.note
            name = co.name
            narrative = curated.narrativeFor(symbol, co.name, sector.name, co.moat, co.monopoly, co.cultural, co.defensive)
        } else {
            sectorName = profile?.sector.orEmpty().ifEmpty { "NSE listed" }
            sectorId = null
            sectorAvg = CuratedData.YF_SECTOR_PE[sectorName] ?: CuratedData.DEFAULT_SECTOR_PE
            moat = "None"
            monopoly = false
            cultural = false
            defensive = false
            note = ""
            name = profile?.name ?: symbol
            narrative = "$name is an NSE-listed ${sectorName.lowercase()} business outside the " +
                "curated Coffee-Can set. Judge it on the gauge and the 5Y revenue CAGR below — " +
                "the noise filter still applies."
        }

        val pe = profile?.pe
        val own = own5yPe(symbol)
        val history = cachedJson("hist1y:$symbol", Ttl.STOCK_1Y) { fetchHistoryJson(symbol, "1y", "1d") }
        val points = parsePoints(history)
        val earnings = earningsFor(symbol)
        val dots = attachDrops(earnings, points)

        val ltp = profile?.price ?: points.lastOrNull()?.c
        val chg = profile?.changePct
            ?: if (points.size >= 2) ResearchMath.heroChange(points) else null

        return StockDeep(
            symbol = symbol,
            name = name,
            sector = sectorName,
            sectorId = sectorId,
            curated = found != null,
            note = note,
            ltp = ltp,
            chgPct = chg,
            pe = pe,
            pe5yOwn = own,
            peSectorAvg = sectorAvg,
            mcapCr = profile?.mcap?.let { it / 1e7 },
            revCagr5y = financialsCagr(symbol),
            moat = moat,
            monopoly = monopoly,
            cultural = cultural,
            defensive = defensive,
            narrative = narrative,
            reality = ResearchMath.realityCheck(pe, sectorAvg, own),
            priceHistory = points,
            earningsDots = dots,
            sparkline = points.takeLast(60).map { it.c.toFloat() },
        )
    }

    /** Refetched when a range chip changes; re-runs the full chart choreography on the client. */
    suspend fun series(symbolRaw: String, range: String): StockSeries {
        val symbol = symbolRaw.uppercase()
        val spec = when (range) {
            "5d" -> Triple("5d", "15m", "5d")
            "1m" -> Triple("1mo", "1d", "1m")
            "6m" -> Triple("6mo", "1d", "6m")
            else -> Triple("1y", "1d", "1y")
        }
        val body = cachedJson("hist${spec.third}:$symbol", rangeTtl[spec.third] ?: Ttl.H1Y) {
            fetchHistoryJson(symbol, spec.first, spec.second)
        }
        val points = parsePoints(body)
        return StockSeries(
            range = spec.third,
            points = points,
            dots = attachDrops(earningsFor(symbol), points),
        )
    }

    // ── Search ───────────────────────────────────────────────────

    /** Curated matches first, then universe matches, then live Yahoo hits. Max 12. */
    suspend fun search(query: String): List<SearchResult> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val out = LinkedHashMap<String, SearchResult>()

        for (sector in curated.sectors) {
            for (co in sector.companies) {
                if (q in co.symbol.lowercase() || q in co.name.lowercase()) {
                    out.getOrPut(co.symbol) { SearchResult(co.symbol, co.name, sector.name, sector.id) }
                }
            }
        }
        for (u in universe) {
            if (q in u.symbol.lowercase() || q in u.name.lowercase()) {
                val sid = CuratedData.INDUSTRY_MAP[u.industry]
                val label = sid?.let { curated.byId[it]?.name } ?: u.industry.ifEmpty { "NSE" }
                out.getOrPut(u.symbol) { SearchResult(u.symbol, u.name, label, sid) }
            }
        }
        if (out.size < 12) {
            val live = runCatching { YahooResearch.search(query) }.getOrDefault(emptyList())
            for (hit in live) {
                if (out.size >= 12) break
                val entry = curated.find(hit.symbol)
                out.getOrPut(hit.symbol) {
                    SearchResult(
                        symbol = hit.symbol,
                        name = entry?.second?.name ?: hit.name,
                        sector = entry?.first?.name ?: hit.exchange,
                        sectorId = entry?.first?.id,
                    )
                }
            }
        }
        return out.values.take(12).toList()
    }

    // ── Internals ────────────────────────────────────────────────

    private suspend fun fetchHistoryJson(symbol: String, range: String, interval: String): String? {
        val payload = YahooResearch.history(symbol, range, interval) ?: return null
        val points = ResearchMath.downsample(payload.closes.map { Point(it.first, it.second) })
        if (points.isEmpty()) return null
        val root = org.json.JSONObject()
        root.put("price", payload.price ?: points.last().c)
        payload.previousClose?.let { root.put("prev", it) }
        val arr = org.json.JSONArray()
        for (p in points) arr.put(org.json.JSONObject().put("t", p.t).put("c", p.c))
        root.put("pts", arr)
        return root.toString()
    }

    private suspend fun fetchProfileJson(symbol: String): String? {
        val profile = YahooResearch.profile(symbol) ?: return null
        val root = org.json.JSONObject()
        profile.pe?.let { root.put("pe", it) }
        profile.mcap?.let { root.put("mcap", it) }
        profile.price?.let { root.put("ltp", it) }
        profile.changePct?.let { root.put("chg", it) }
        root.put("sector", profile.sector)
        profile.name?.let { root.put("name", it) }
        return root.toString()
    }

    private fun parsePoints(body: String?): List<Point> {
        if (body == null) return emptyList()
        return runCatching {
            val arr = org.json.JSONObject(body).getJSONArray("pts")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Point(o.getString("t"), o.getDouble("c"))
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun cachedJson(key: String, ttl: Long, fetch: suspend () -> String?): String? =
        cached(key, ttl, fetch)

    private suspend fun own5yPe(symbol: String): Double? {
        val finBody = cachedJson("fin:$symbol", Ttl.FINANCIALS) { fetchFinancialsJson(symbol) }
        val fin = FinancialsBody.parse(finBody) ?: return null
        if (fin.eps.size < 2) return null
        val histBody = cachedJson("hist5y:$symbol", Ttl.STOCK_5Y) {
            fetchHistoryJson(symbol, "5y", "1mo")
        }
        val closes = parsePoints(histBody)
        if (closes.isEmpty()) return null
        return ResearchMath.own5yPe(fin.eps, closes)
    }

    private suspend fun financialsCagr(symbol: String): Double? {
        val body = cachedJson("fin:$symbol", Ttl.FINANCIALS) { fetchFinancialsJson(symbol) }
        return FinancialsBody.parse(body)?.cagr
    }

    private suspend fun fetchFinancialsJson(symbol: String): String? {
        val fin = YahooResearch.financials(symbol) ?: return null
        val root = org.json.JSONObject()
        fin.cagr?.let { root.put("cagr", it) }
        val eps = org.json.JSONArray()
        for ((year, value) in fin.epsByYear) eps.put(org.json.JSONObject().put("y", year).put("e", value))
        root.put("eps", eps)
        return root.toString()
    }

    private suspend fun earningsFor(symbol: String): List<EarningsDot> {
        val body = cachedJson("earn:$symbol", Ttl.EARNINGS) {
            val rows = YahooResearch.earnings(symbol)
            if (rows.isEmpty()) return@cachedJson "[]"
            val arr = org.json.JSONArray()
            for ((date, surprise) in rows) {
                val o = org.json.JSONObject().put("date", date)
                if (surprise != null) o.put("surprise", surprise)
                arr.put(o)
            }
            arr.toString()
        } ?: return emptyList()
        return runCatching {
            val arr = org.json.JSONArray(body)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                EarningsDot(
                    date = o.getString("date"),
                    surprise = if (o.has("surprise")) o.getDouble("surprise") else null,
                    dropPct = null,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun attachDrops(dots: List<EarningsDot>, history: List<Point>): List<EarningsDot> =
        dots.map { it.copy(dropPct = ResearchMath.dropFor(it.date, history)) }
}

/** Minimal field readers over the cached JSON blobs — no reflection, no extra dependencies. */
private class JSONObjectBody(raw: String) {
    private val root = org.json.JSONObject(raw)
    fun double(key: String): Double? = if (root.has(key)) root.getDouble(key) else null
}

private object ProfileBody {
    internal data class Parsed(
        val pe: Double?,
        val mcap: Double?,
        val sector: String,
        val name: String?,
        val price: Double?,
        val changePct: Double?,
    )

    fun parse(body: String?): Parsed? {
        if (body == null) return null
        return runCatching {
            val o = org.json.JSONObject(body)
            Parsed(
                pe = if (o.has("pe")) o.getDouble("pe") else null,
                mcap = if (o.has("mcap")) o.getDouble("mcap") else null,
                sector = o.optString("sector"),
                name = o.optString("name").ifEmpty { null },
                price = if (o.has("ltp")) o.getDouble("ltp") else null,
                changePct = if (o.has("chg")) o.getDouble("chg") else null,
            )
        }.getOrNull()
    }
}

private object FinancialsBody {
    internal data class Parsed(val cagr: Double?, val eps: List<Pair<Int, Double>>)

    fun parse(body: String?): Parsed? {
        if (body == null) return null
        return runCatching {
            val o = org.json.JSONObject(body)
            val arr = o.optJSONArray("eps")
            val eps = ArrayList<Pair<Int, Double>>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    eps.add(e.getInt("y") to e.getDouble("e"))
                }
            }
            Parsed(
                cagr = if (o.has("cagr")) o.getDouble("cagr") else null,
                eps = eps,
            )
        }.getOrNull()
    }
}
