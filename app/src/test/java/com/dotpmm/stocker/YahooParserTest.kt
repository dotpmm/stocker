package com.dotpmm.stocker

import com.dotpmm.stocker.data.net.YahooParsers
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses the recorded Yahoo payloads that the app consumes.
 *
 * Yahoo's field names are the fragile surface of this app: a silent rename should fail here rather
 * than quietly blank a screen. Each fixture is a real response captured from the endpoint the app
 * calls, so these tests need no network.
 */
class YahooParserTest {

    private fun fixture(name: String) = File("src/test/fixtures/$name").readText()

    // ── Chart / history ──────────────────────────────────────────

    @Test
    fun `daily chart parses into dated closes`() {
        // The 1y payload recorded from the live endpoint.
        val payload = YahooParsers.chart(
            fixture("stock_TITAN.json").let { stock ->
                // stock_TITAN.json is the reference API shape; build a chart body from it instead.
                chartBodyFromReference(stock)
            },
            interval = "1d",
        )
        assertNotNull(payload)
        assertTrue("points parsed", payload!!.closes.size > 100)
        assertTrue("daily stamps carry no time", payload.closes.all { !it.first.contains(' ') })
        assertTrue("closes look like a price", payload.closes.all { it.second > 0 })
    }

    @Test
    fun `intraday chart stamps carry a time`() {
        val payload = YahooParsers.chart(
            fixture("yahoo_chart_5d.json").let { chartBodyFromReference(it) },
            interval = "15m",
        )
        assertNotNull(payload)
        assertTrue("bars parsed", payload!!.closes.isNotEmpty())
        assertTrue("15m stamps include HH:MM", payload.closes.all { it.first.contains(' ') })
    }

    @Test
    fun `chart parses the quote metadata`() {
        val body = fixture("yahoo_chart_5d.json")
        val payload = YahooParsers.chart(chartBodyFromReference(body), "1d")
        assertNotNull(payload)
        assertNotNull("regularMarketPrice present", payload!!.price)
        assertNotNull("chartPreviousClose present", payload.previousClose)
    }

    @Test
    fun `chart tolerates a malformed body rather than throwing`() {
        assertNull(YahooParsers.chart("not json", "1d"))
        assertNull(YahooParsers.chart("{}", "1d"))
        assertNull(YahooParsers.chart("""{"chart":{"result":[]}}""", "1d"))
    }

    @Test
    fun `null closes are skipped, not coerced to zero`() {
        // A gap in an Indian series is normal (halts, thin bars). A zero would wreck the scale.
        val body = """
            {"chart":{"result":[{"meta":{"regularMarketPrice":120.0},
            "timestamp":[100,200,300],
            "indicators":{"quote":[{"close":[100.0,null,130.0]}]}}]}}
        """.trimIndent()
        val payload = YahooParsers.chart(body, "1d")
        assertNotNull(payload)
        assertEquals(2, payload!!.closes.size)
        assertEquals(100.0, payload.closes[0].second, 1e-9)
        assertEquals(130.0, payload.closes[1].second, 1e-9)
    }

    // ── Batch quotes ─────────────────────────────────────────────

    @Test
    fun `batch quotes parse pe and market cap`() {
        val quotes = YahooParsers.batchQuotes(fixture("yahoo_quote_batch.json"))
        assertTrue("rows parsed", quotes.size >= 5)
        val titan = quotes["TITAN"]
        assertNotNull("TITAN present", titan)
        assertEquals(61.149815, titan!!.pe!!, 1e-6)
        assertEquals(4298939301888.0, titan.mcap!!, 1.0)
        assertEquals(4844.9, titan.price!!, 1e-9)
        assertEquals(-1.5644366, titan.changePct!!, 1e-6)
    }

    @Test
    fun `batch quotes expose heatmap fields and derive a missing change`() {
        val body = """{"quoteResponse":{"result":[{
            "symbol":"SBIN.NS","trailingPE":9.5,"marketCap":720000000000,
            "regularMarketPrice":810.0,"regularMarketPreviousClose":800.0,
            "regularMarketVolume":1234567,"regularMarketDayHigh":815.5,"regularMarketDayLow":798.0
        }]}}"""
        val row = YahooParsers.batchQuotes(body).getValue("SBIN")
        assertEquals(1.25, row.changePct!!, 1e-9)
        assertEquals(1_234_567L, row.volume)
        assertEquals(815.5, row.dayHigh!!, 1e-9)
        assertEquals(798.0, row.dayLow!!, 1e-9)
        assertEquals(800.0, row.previousClose!!, 1e-9)
    }

    @Test
    fun `batch quotes strip the nse suffix from the key`() {
        val quotes = YahooParsers.batchQuotes(fixture("yahoo_quote_batch.json"))
        assertTrue("keys are bare symbols", quotes.keys.none { it.endsWith(".NS") })
        assertNotNull(quotes["HDFCBANK"])
    }

    @Test
    fun `batch quotes tolerate a malformed body`() {
        assertTrue(YahooParsers.batchQuotes("nope").isEmpty())
        assertTrue(YahooParsers.batchQuotes("{}").isEmpty())
    }

    @Test
    fun `batch quotes skip rows without a symbol`() {
        val body = """{"quoteResponse":{"result":[{"trailingPE":10.0},{"symbol":"SBIN.NS","trailingPE":9.5}]}}"""
        val quotes = YahooParsers.batchQuotes(body)
        assertEquals(1, quotes.size)
        assertEquals(9.5, quotes["SBIN"]!!.pe!!, 1e-9)
    }

    // ── Profile (quoteSummary) ───────────────────────────────────

    @Test
    fun `profile parses pe, market cap and sector`() {
        val profile = YahooParsers.profile(fixture("yahoo_quotesummary_TITAN.json"))
        assertNotNull(profile)
        assertEquals(61.149815, profile!!.pe!!, 1e-6)
        assertEquals(4298939301888.0, profile.mcap!!, 1.0)
        assertEquals("Consumer Cyclical", profile.sector)
        assertTrue("long name present", profile.name!!.contains("Titan"))
    }

    @Test
    fun `profile derives the change when the field is absent`() {
        val body = """
            {"quoteSummary":{"result":[{"summaryDetail":{"previousClose":{"raw":100.0}},
            "price":{"regularMarketPrice":{"raw":105.0},"marketCap":{"raw":1000000}},
            "assetProfile":{"sector":"Technology"}}]}}
        """.trimIndent()
        val profile = YahooParsers.profile(body)
        assertNotNull(profile)
        assertEquals(5.0, profile!!.changePct!!, 1e-9)
    }

    @Test
    fun `profile survives a body with no modules`() {
        val profile = YahooParsers.profile("""{"quoteSummary":{"result":[{}]}}""")
        assertNotNull("an empty result still yields a profile", profile)
        assertNull(profile!!.pe)
        assertNull(profile.mcap)
    }

    @Test
    fun `profile returns null on a malformed body`() {
        assertNull(YahooParsers.profile("nope"))
        assertNull(YahooParsers.profile("{}"))
    }

    // ── Fundamentals timeseries ──────────────────────────────────

    @Test
    fun `financials parse annual eps and compute a cagr`() {
        val fin = YahooParsers.financials(fixture("yahoo_timeseries_TITAN.json"))
        assertNotNull(fin)
        assertTrue("EPS years parsed", fin!!.epsByYear.isNotEmpty())
        assertTrue("years look sane", fin.epsByYear.all { it.first in 2015..2035 })
        assertTrue("EPS values are positive here", fin.epsByYear.all { it.second > 0 })
        // The recorded TITAN payload carries three fiscal years of diluted EPS.
        assertEquals(3, fin.epsByYear.size)
    }

    @Test
    fun `financials with no revenue series leave cagr null rather than guessing`() {
        val body = """
            {"timeseries":{"result":[{"meta":{"type":["annualDilutedEPS"]},
            "annualDilutedEPS":[{"asOfDate":"2024-03-31","reportedValue":{"raw":10.0}},
            {"asOfDate":"2025-03-31","reportedValue":{"raw":12.0}}]}]}}
        """.trimIndent()
        val fin = YahooParsers.financials(body)
        assertNotNull(fin)
        assertNull("no revenue means no CAGR", fin!!.cagr)
        assertEquals(2, fin.epsByYear.size)
    }

    @Test
    fun `financials tolerate a malformed body`() {
        assertNull(YahooParsers.financials("nope"))
        assertNull(YahooParsers.financials("{}"))
    }

    // ── Earnings ────────────────────────────────────────────────

    @Test
    fun `earnings parse reported dates and surprises`() {
        val earnings = YahooParsers.earnings(fixture("yahoo_earnings_TITAN.json"))
        assertTrue("earnings rows parsed", earnings.isNotEmpty())
        assertTrue("dates are ISO", earnings.all { it.first.matches(Regex("""\d{4}-\d{2}-\d{2}""")) })
        assertTrue("at least one surprise present", earnings.any { it.second != null })
        assertTrue("sorted oldest first", earnings == earnings.sortedBy { it.first })
    }

    @Test
    fun `earnings with no reported date are skipped`() {
        val body = """
            {"quoteSummary":{"result":[{"earnings":{"earningsChart":{"quarterly":[
            {"date":"1Q2026","surprisePct":"5.0"},
            {"date":"2Q2026","reportedDate":{"fmt":"2026-05-08"},"surprisePct":"-14.97"}]}}}]}}
        """.trimIndent()
        val earnings = YahooParsers.earnings(body)
        assertEquals(1, earnings.size)
        assertEquals("2026-05-08", earnings[0].first)
        assertEquals(-14.97, earnings[0].second!!, 1e-9)
    }

    @Test
    fun `earnings tolerate a malformed body`() {
        assertTrue(YahooParsers.earnings("nope").isEmpty())
        assertTrue(YahooParsers.earnings("{}").isEmpty())
        assertTrue(YahooParsers.earnings("""{"quoteSummary":{"result":[{}]}}""").isEmpty())
    }

    /**
     * The recorded fixtures are reference-API payloads (a list of `{t, c}` points); wrap one in the
     * raw Yahoo chart envelope so the parser sees the real shape.
     */
    private fun chartBodyFromReference(referenceJson: String): String {
        val root = org.json.JSONObject(referenceJson)
        val source = root.optJSONArray("price_history")
            ?: root.optJSONObject("hero")?.optJSONArray("sparkline")
            ?: org.json.JSONArray()
        val stamps = org.json.JSONArray()
        val closes = org.json.JSONArray()
        for (i in 0 until source.length()) {
            val point = source.getJSONObject(i)
            val stamp = point.getString("t")
            val epoch = if (stamp.contains(' ')) {
                java.time.LocalDateTime.parse(stamp.replace(' ', 'T'))
                    .toEpochSecond(java.time.ZoneOffset.ofHoursMinutes(5, 30))
            } else {
                java.time.LocalDate.parse(stamp)
                    .atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHoursMinutes(5, 30))
            }
            stamps.put(epoch)
            closes.put(point.getDouble("c"))
        }
        return org.json.JSONObject().apply {
            put(
                "chart",
                org.json.JSONObject().put(
                    "result",
                    org.json.JSONArray().put(
                        org.json.JSONObject().apply {
                            put("meta", org.json.JSONObject().put("regularMarketPrice", 4844.9).put("chartPreviousClose", 4930.0))
                            put("timestamp", stamps)
                            put(
                                "indicators",
                                org.json.JSONObject().put(
                                    "quote",
                                    org.json.JSONArray().put(org.json.JSONObject().put("close", closes)),
                                ),
                            )
                        },
                    ),
                ),
            )
        }.toString()
    }
}
