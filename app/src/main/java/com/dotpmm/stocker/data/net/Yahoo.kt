package com.dotpmm.stocker.data.net

import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.domain.nlp.Names
import java.io.IOException
import java.net.URLEncoder
import org.json.JSONObject

class ChartData(
    val price: Double,
    val previousClose: Double?,
    /** Epoch seconds of the latest quote. */
    val marketTime: Long?,
    val times: List<Long>,
    val closes: List<Double>,
)

/** Unofficial Yahoo Finance endpoints, used best-effort for search and prices. */
object Yahoo {
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private val exchangeMap = mapOf(
        "NSI" to ("NSE" to Market.India),
        "BSE" to ("BSE" to Market.India),
        "NMS" to ("NASDAQ" to Market.US),
        "NGM" to ("NASDAQ" to Market.US),
        "NCM" to ("NASDAQ" to Market.US),
        "NYQ" to ("NYSE" to Market.US),
        "ASE" to ("NYSE American" to Market.US),
        "PCX" to ("NYSE Arca" to Market.US),
    )

    suspend fun search(query: String): List<WatchStock> {
        val url = "https://query2.finance.yahoo.com/v1/finance/search?q=${enc(query)}" +
            "&quotesCount=12&newsCount=0&listsCount=0&lang=en-IN&region=IN"
        val response = Http.get(url, gateMs = 250)
        if (!response.ok) throw IOException("Search failed (HTTP ${response.code})")
        val quotes = JSONObject(response.body).optJSONArray("quotes") ?: return emptyList()
        val out = ArrayList<WatchStock>()
        for (i in 0 until quotes.length()) {
            val q = quotes.optJSONObject(i) ?: continue
            if (q.optString("quoteType") != "EQUITY") continue
            val (exchange, market) = exchangeMap[q.optString("exchange")] ?: continue
            val symbol = q.optString("symbol")
            if (symbol.isEmpty()) continue
            val rawName = q.optString("longname").ifEmpty { q.optString("shortname") }.ifEmpty { symbol }
            val ticker = symbol.substringBefore('.')
            out.add(
                WatchStock(
                    ticker = ticker,
                    name = Names.clean(rawName),
                    market = market,
                    symbol = symbol,
                    exchange = exchange,
                ),
            )
        }
        // NSE listings before BSE duplicates of the same ticker.
        return out.distinctBy { it.ticker + it.market }.sortedBy { if (it.exchange == "BSE") 1 else 0 }
    }

    suspend fun chart(symbol: String, range: String, interval: String): ChartData {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/${enc(symbol)}" +
            "?range=$range&interval=$interval&includePrePost=false"
        val response = Http.get(url, gateMs = 200)
        if (!response.ok) throw IOException("Prices unavailable (HTTP ${response.code})")
        val root = JSONObject(response.body).getJSONObject("chart")
        val result = root.optJSONArray("result")?.optJSONObject(0)
            ?: throw IOException("No price data for $symbol")
        val meta = result.getJSONObject("meta")
        val timestamps = result.optJSONArray("timestamp")
        val closeArray = result.optJSONObject("indicators")
            ?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
        val times = ArrayList<Long>()
        val closes = ArrayList<Double>()
        if (timestamps != null && closeArray != null) {
            for (i in 0 until minOf(timestamps.length(), closeArray.length())) {
                if (closeArray.isNull(i)) continue
                times.add(timestamps.getLong(i))
                closes.add(closeArray.getDouble(i))
            }
        }
        val price = if (meta.has("regularMarketPrice") && !meta.isNull("regularMarketPrice")) {
            meta.getDouble("regularMarketPrice")
        } else {
            closes.lastOrNull() ?: throw IOException("No price for $symbol")
        }
        val prev = if (meta.has("chartPreviousClose") && !meta.isNull("chartPreviousClose")) {
            meta.getDouble("chartPreviousClose")
        } else {
            null
        }
        val marketTime = if (meta.has("regularMarketTime") && !meta.isNull("regularMarketTime")) meta.getLong("regularMarketTime") else null
        return ChartData(price, prev, marketTime, times, closes)
    }
}
