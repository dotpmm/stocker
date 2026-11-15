package com.dotpmm.stocker.data.net

import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Yahoo Finance endpoints behind the research surface.
 *
 * The quoteSummary and quote endpoints require a cookie + crumb handshake; the handshake is done
 * once and refreshed lazily when a request comes back unauthorised. Index symbols (^NSEI, ^CNXIT…)
 * must never get `.NS` appended (FUNCTIONALITY.md §5).
 */
object YahooResearch {
    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Stocks get `.NS`; indices are raw. */
    fun nseSymbol(symbol: String): String =
        if (symbol.startsWith("^")) symbol else "$symbol.NS"

    @Volatile private var crumb: String? = null
    @Volatile private var cookie: String? = null

    private val browserUa =
        "Mozilla/5.0 (Linux; Android 14; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0.0.0 Mobile Safari/537.36"

    private suspend fun handshake(): Boolean {
        return try {
            val consent = Http.get(
                "https://fc.yahoo.com",
                headers = mapOf("User-Agent" to browserUa),
                timeoutMs = 12_000,
            )
            val setCookie = consent.headers["Set-Cookie"]?.substringBefore(';')
            val cx = Http.get(
                "https://query1.finance.yahoo.com/v1/test/getcrumb",
                headers = buildMap {
                    put("User-Agent", browserUa)
                    if (setCookie != null) put("Cookie", setCookie)
                },
                timeoutMs = 12_000,
            )
            if (!cx.ok || cx.body.isBlank()) return false
            cookie = setCookie
            crumb = cx.body.trim()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun authed(url: String): HttpResponse {
        if (crumb == null) handshake()
        var response = Http.get(
            url,
            headers = buildMap {
                put("User-Agent", browserUa)
                cookie?.let { put("Cookie", it) }
            },
            timeoutMs = 20_000,
        )
        if (response.code == 401 || response.code == 403) {
            crumb = null
            if (handshake()) {
                response = Http.get(
                    url,
                    headers = buildMap {
                        put("User-Agent", browserUa)
                        cookie?.let { put("Cookie", it) }
                    },
                    timeoutMs = 20_000,
                )
            }
        }
        return response
    }

    private fun crumbSuffix(): String = crumb?.let { "&crumb=${enc(it)}" } ?: ""

    // ── Chart / history ──────────────────────────────────────────

    data class ChartPayload(
        val closes: List<Pair<String, Double>>,
        val price: Double?,
        val previousClose: Double?,
    )

    /**
     * Close series for a symbol. Index symbols pass through untouched.
     * Intraday bars are stamped "YYYY-MM-DD HH:MM", daily bars "YYYY-MM-DD".
     */
    suspend fun history(
        symbol: String,
        range: String,
        interval: String,
        zoneOffsetSeconds: Int = 19800,
    ): ChartPayload? {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/${enc(nseSymbol(symbol))}" +
            "?range=$range&interval=$interval&includePrePost=false"
        val response = Http.get(url, headers = mapOf("User-Agent" to browserUa), timeoutMs = 20_000)
        if (!response.ok) return null
        return runCatching { parseChart(response.body, interval, zoneOffsetSeconds) }.getOrNull()
    }

    private fun parseChart(body: String, interval: String, offsetSeconds: Int): ChartPayload? =
        YahooParsers.chart(body, interval, offsetSeconds)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    // ── Batch quotes (P/E + market cap for a whole sector) ───────

    data class QuoteRow(val symbol: String, val pe: Double?, val mcap: Double?, val name: String?)

    /**
     * Batched fundamentals for up to ~50 symbols. Cuts a sector load from 50 requests to a handful.
     */
    suspend fun quotes(symbols: List<String>): Map<String, QuoteRow> {
        if (symbols.isEmpty()) return emptyMap()
        val out = HashMap<String, QuoteRow>()
        for (chunk in symbols.chunked(40)) {
            val joined = chunk.joinToString(",") { enc(nseSymbol(it)) }
            val url = "https://query1.finance.yahoo.com/v7/finance/quote?symbols=$joined" +
                "&fields=trailingPE,marketCap,shortName,longName${crumbSuffix()}"
            val response = authed(url)
            if (!response.ok) continue
            out.putAll(YahooParsers.batchQuotes(response.body))
        }
        return out
    }

    // ── Per-symbol fundamentals ──────────────────────────────────

    data class Profile(
        val pe: Double?,
        val mcap: Double?,
        val sector: String,
        val name: String?,
        val price: Double?,
        val previousClose: Double?,
        val changePct: Double?,
    )

    suspend fun profile(symbol: String): Profile? {
        val url = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/${enc(nseSymbol(symbol))}" +
            "?modules=summaryDetail,price,assetProfile${crumbSuffix()}"
        val response = authed(url)
        if (!response.ok) return null
        return YahooParsers.profile(response.body)
    }

    data class Financials(val cagr: Double?, val epsByYear: List<Pair<Int, Double>>)

    /**
     * Revenue CAGR + annual EPS from the fundamentals timeseries (4 fiscal years).
     * Falls back to the legacy incomeStatementHistory module when timeseries is empty.
     */
    suspend fun financials(symbol: String): Financials? {
        val now = System.currentTimeMillis() / 1000
        val from = now - 6L * 365 * 86_400
        val types = "annualDilutedEPS,annualBasicEPS,annualTotalRevenue,annualOperatingRevenue"
        val url = "https://query2.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/" +
            "${enc(nseSymbol(symbol))}?symbol=${enc(nseSymbol(symbol))}&type=$types&period1=$from&period2=$now"
        val response = authed(url)
        if (!response.ok) return null
        return YahooParsers.financials(response.body)
    }

    /** Past earnings dates with surprise %, from the quarterly reported-earnings chart. */
    suspend fun earnings(symbol: String): List<Pair<String, Double?>> {
        val url = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/${enc(nseSymbol(symbol))}" +
            "?modules=earnings,calendarEvents${crumbSuffix()}"
        val response = authed(url)
        if (!response.ok) return emptyList()
        return YahooParsers.earnings(response.body)
    }

    // ── Search across the full NSE universe ──────────────────────

    data class SearchHit(val symbol: String, val name: String, val exchange: String)

    suspend fun search(query: String): List<SearchHit> {
        val url = "https://query2.finance.yahoo.com/v1/finance/search?q=${enc(query)}" +
            "&quotesCount=20&newsCount=0&listsCount=0&lang=en-IN&region=IN"
        val response = Http.get(url, headers = mapOf("User-Agent" to browserUa), timeoutMs = 15_000)
        if (!response.ok) return emptyList()
        return runCatching {
            val quotes = JSONObject(response.body).optJSONArray("quotes") ?: JSONArray()
            (0 until quotes.length()).mapNotNull { i ->
                val q = quotes.optJSONObject(i) ?: return@mapNotNull null
                if (q.optString("quoteType") != "EQUITY") return@mapNotNull null
                val exchange = q.optString("exchange")
                if (exchange != "NSI" && exchange != "BSE") return@mapNotNull null
                val symbol = q.optString("symbol").substringBefore('.')
                if (symbol.isEmpty()) return@mapNotNull null
                SearchHit(
                    symbol = symbol,
                    name = q.optString("longname").ifEmpty { q.optString("shortname") }.ifEmpty { symbol },
                    exchange = if (exchange == "NSI") "NSE" else "BSE",
                )
            }
        }.getOrDefault(emptyList())
    }
}
