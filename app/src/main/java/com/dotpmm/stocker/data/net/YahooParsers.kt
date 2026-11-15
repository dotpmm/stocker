package com.dotpmm.stocker.data.net

import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure parsers for the Yahoo payload shapes. Kept separate from the network calls so they can be
 * exercised against recorded payloads — Yahoo's field names are the fragile part of this app, and
 * a rename should fail a test rather than silently blank a screen.
 */
object YahooParsers {

    internal fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    /** `chart.result[0]` → dated closes plus the quote metadata. */
    fun chart(body: String, interval: String, offsetSeconds: Int = 19800): YahooResearch.ChartPayload? {
        return runCatching {
            val result = JSONObject(body).getJSONObject("chart")
                .getJSONArray("result").getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val stamps = result.optJSONArray("timestamp")
            val closes = result.optJSONObject("indicators")
                ?.optJSONArray("quote")?.optJSONObject(0)?.optJSONArray("close")
            // Intraday detection is by interval, and the stamp format follows it.
            val intraday = interval.endsWith("m") && interval != "1mo"
            val formatter = if (intraday) {
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            } else {
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            }
            val zone = java.time.ZoneOffset.ofTotalSeconds(offsetSeconds)
            val out = ArrayList<Pair<String, Double>>()
            if (stamps != null && closes != null) {
                for (i in 0 until minOf(stamps.length(), closes.length())) {
                    if (closes.isNull(i)) continue
                    val ts = java.time.Instant.ofEpochSecond(stamps.getLong(i)).atOffset(zone)
                    out.add(formatter.format(ts) to closes.getDouble(i))
                }
            }
            YahooResearch.ChartPayload(
                closes = out,
                price = meta.optDoubleOrNull("regularMarketPrice"),
                previousClose = meta.optDoubleOrNull("chartPreviousClose"),
            )
        }.getOrNull()
    }

    /** `quoteResponse.result[]` → the batch fundamentals map keyed by bare NSE symbol. */
    fun batchQuotes(body: String): Map<String, YahooResearch.QuoteRow> {
        val out = HashMap<String, YahooResearch.QuoteRow>()
        val rows = runCatching {
            JSONObject(body).getJSONObject("quoteResponse").getJSONArray("result")
        }.getOrNull() ?: return out
        for (i in 0 until rows.length()) {
            val o = rows.optJSONObject(i) ?: continue
            val sym = o.optString("symbol").substringBefore('.')
            if (sym.isEmpty()) continue
            out[sym] = YahooResearch.QuoteRow(
                symbol = sym,
                pe = o.optDoubleOrNull("trailingPE"),
                mcap = o.optDoubleOrNull("marketCap"),
                name = o.optString("longName").ifEmpty { o.optString("shortName") }.ifEmpty { null },
            )
        }
        return out
    }

    /** `quoteSummary.result[0]` with summaryDetail + price + assetProfile → the profile card. */
    fun profile(body: String): YahooResearch.Profile? {
        return runCatching {
            val result = JSONObject(body).getJSONObject("quoteSummary")
                .getJSONArray("result").getJSONObject(0)
            val summary = result.optJSONObject("summaryDetail") ?: JSONObject()
            val price = result.optJSONObject("price") ?: JSONObject()
            val asset = result.optJSONObject("assetProfile") ?: JSONObject()

            fun raw(obj: JSONObject, key: String): Double? =
                obj.optJSONObject(key)?.optDoubleOrNull("raw")

            val last = raw(price, "regularMarketPrice")
            val prev = raw(summary, "previousClose") ?: raw(price, "regularMarketPreviousClose")
            val chg = raw(price, "regularMarketChangePercent")
                ?: run {
                    if (last != null && prev != null && prev > 0) (last - prev) / prev * 100 else null
                }
            YahooResearch.Profile(
                pe = raw(summary, "trailingPE"),
                mcap = raw(price, "marketCap"),
                sector = asset.optString("sector"),
                name = price.optString("longName").ifEmpty { price.optString("shortName") }.ifEmpty { null },
                price = last,
                previousClose = prev,
                changePct = chg,
            )
        }.getOrNull()
    }

    /** Fundamentals timeseries → revenue CAGR plus the annual EPS pairs. */
    fun financials(body: String): YahooResearch.Financials? {
        return runCatching {
            val results = JSONObject(body).getJSONObject("timeseries").getJSONArray("result")
            val revenues = LinkedHashMap<String, Double>()
            val eps = LinkedHashMap<String, Double>()
            for (i in 0 until results.length()) {
                val entry = results.optJSONObject(i) ?: continue
                collectSeries(entry, "annualTotalRevenue", revenues)
                collectSeries(entry, "annualOperatingRevenue", revenues)
                collectSeries(entry, "annualDilutedEPS", eps)
                collectSeries(entry, "annualBasicEPS", eps)
            }
            val orderedRevenue = revenues.entries.sortedBy { it.key }.map { it.value }
            val epsByYear = eps.entries.sortedBy { it.key }.mapNotNull { (date, value) ->
                val year = date.take(4).toIntOrNull() ?: return@mapNotNull null
                year to value
            }
            YahooResearch.Financials(
                cagr = com.dotpmm.stocker.research.ResearchMath.revenueCagr(orderedRevenue),
                epsByYear = epsByYear,
            )
        }.getOrNull()
    }

    private fun collectSeries(entry: JSONObject, key: String, into: LinkedHashMap<String, Double>) {
        val arr: JSONArray = entry.optJSONArray(key) ?: return
        for (j in 0 until arr.length()) {
            val point = arr.optJSONObject(j) ?: continue
            val date = point.optString("asOfDate")
            val value = point.optJSONObject("reportedValue")?.optDoubleOrNull("raw") ?: continue
            if (date.isNotEmpty() && into[date] == null) into[date] = value
        }
    }

    /** `earnings.earningsChart.quarterly[]` → (reported date, surprise %) pairs, oldest first. */
    fun earnings(body: String): List<Pair<String, Double?>> {
        return runCatching {
            val result = JSONObject(body).getJSONObject("quoteSummary")
                .getJSONArray("result").getJSONObject(0)
            val out = ArrayList<Pair<String, Double?>>()
            val quarterly = result.optJSONObject("earnings")
                ?.optJSONObject("earningsChart")?.optJSONArray("quarterly")
            if (quarterly != null) {
                for (i in 0 until quarterly.length()) {
                    val q = quarterly.optJSONObject(i) ?: continue
                    val reported = q.optJSONObject("reportedDate")?.optString("fmt").orEmpty()
                    if (reported.isEmpty()) continue
                    out.add(reported to q.optString("surprisePct").toDoubleOrNull())
                }
            }
            out.sortedBy { it.first }
        }.getOrDefault(emptyList())
    }
}
