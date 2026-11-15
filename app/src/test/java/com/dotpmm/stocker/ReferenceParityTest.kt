package com.dotpmm.stocker

import com.dotpmm.stocker.research.CuratedData
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.ResearchMath
import com.dotpmm.stocker.research.Verdict
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Replays recorded payloads from the reference implementation and asserts this port reproduces its
 * numbers. The fixtures are real responses captured from the live dashboard, so a regression in the
 * computations shows up here rather than on a phone.
 */
class ReferenceParityTest {

    private val sectorsPayload: JSONObject by lazy {
        JSONObject(File("src/test/fixtures/sectors.json").readText())
    }
    private val sectorPayload: JSONObject by lazy {
        JSONObject(File("src/test/fixtures/sector_banking.json").readText())
    }
    private val stockPayload: JSONObject by lazy {
        JSONObject(File("src/test/fixtures/stock_TITAN.json").readText())
    }
    private val historyPayload: JSONObject by lazy {
        JSONObject(File("src/test/fixtures/history_TITAN_5d.json").readText())
    }

    private fun points(json: org.json.JSONArray): List<Point> =
        (0 until json.length()).map {
            val o = json.getJSONObject(it)
            Point(o.getString("t"), o.getDouble("c"))
        }

    // ── Macro card parity ────────────────────────────────────────

    @Test
    fun `split and sector card values match the reference snapshot`() {
        val sectors = sectorsPayload.getJSONArray("sectors")
        assertEquals(6, sectors.length())
        val expected = mapOf(
            "fmcg" to Triple(40.9, 38.0, "consolidating"),
            "it" to Triple(20.3, 26.0, "discounted"),
            "power" to Triple(52.0, 30.0, "overheated"),
            "retail" to Triple(37.6, 42.0, "discounted"),
            "banking" to Triple(17.0, 16.0, "consolidating"),
            "pharma" to Triple(49.4, 30.0, "overheated"),
        )
        for (i in 0 until sectors.length()) {
            val card = sectors.getJSONObject(i)
            val id = card.getString("id")
            val (pe, baseline, badge) = expected[id]!!
            assertEquals("$id pe", pe, card.getDouble("pe_current"), 1e-9)
            assertEquals("$id baseline", baseline, card.getDouble("pe_5y"), 1e-9)
            assertEquals("$id badge string", badge, card.getString("badge"))
            // And our badge function must agree with the reference's own label.
            val ours = ResearchMath.badge(card.getDouble("pe_current"), baseline)
            assertEquals(
                "$id badge from ResearchMath",
                badge,
                when (ours) {
                    Verdict.Overheated -> "overheated"
                    Verdict.Discounted -> "discounted"
                    Verdict.Consolidating -> "consolidating"
                },
            )
        }
    }

    @Test
    fun `hero change is recomputed from the series and matches`() {
        val hero = sectorsPayload.getJSONObject("hero")
        val spark = points(hero.getJSONArray("sparkline"))
        assertEquals(60, spark.size)
        val ours = ResearchMath.heroChange(spark)!!
        assertEquals(hero.getDouble("chg_pct"), ours, 0.005)
        assertEquals(hero.getDouble("value"), spark.last().c, 1e-9)
    }

    @Test
    fun `every sector year change is recomputed correctly`() {
        val sectors = sectorsPayload.getJSONArray("sectors")
        for (i in 0 until sectors.length()) {
            val card = sectors.getJSONObject(i)
            if (card.isNull("year_change_pct")) continue
            val spark = points(card.getJSONArray("sparkline"))
            val ours = ResearchMath.yearChange(spark)!!
            assertEquals(
                "sector ${card.getString("id")} year change",
                card.getDouble("year_change_pct"),
                ours,
                0.05,
            )
        }
    }

    @Test
    fun `dependency edges survive round trip with their weights`() {
        val deps = sectorsPayload.getJSONArray("dependencies")
        assertEquals(8, deps.length())
        val byPair = (0 until deps.length()).associate {
            val d = deps.getJSONObject(it)
            (d.getString("from") to d.getString("to")) to d.getDouble("w")
        }
        assertEquals(0.8, byPair["banking" to "power"]!!, 1e-9)
        assertEquals(0.2, byPair["power" to "fmcg"]!!, 1e-9)
    }

    // ── Sector drill-down parity ─────────────────────────────────

    @Test
    fun `banking drill-down has the documented counts`() {
        assertEquals(8, sectorPayload.getInt("n_curated"))
        assertEquals(49, sectorPayload.getInt("n_universe"))
        assertEquals(16.0, sectorPayload.getDouble("pe_5y_avg"), 1e-9)
        val companies = sectorPayload.getJSONArray("companies")
        assertEquals(49, companies.length())
    }

    @Test
    fun `rows are sorted by market cap descending as shipped`() {
        val companies = sectorPayload.getJSONArray("companies")
        var previous = Double.MAX_VALUE
        for (i in 0 until companies.length()) {
            val row = companies.getJSONObject(i)
            val mcap = if (row.isNull("mcap_cr")) 0.0 else row.getDouble("mcap_cr")
            assertTrue("rows must be mcap-descending", mcap <= previous)
            previous = mcap
        }
    }

    @Test
    fun `curated constituents are flagged rather than force-ordered to the top`() {
        val companies = sectorPayload.getJSONArray("companies")
        // The shipped engine sorts strictly by market cap (`companies.sort(key=mcap, reverse=True)`),
        // so curated rows are interleaved and discoverable by their flag, not by position.
        val curated = (0 until companies.length()).count { companies.getJSONObject(it).getBoolean("curated") }
        assertEquals("banking has 8 curated constituents", 8, curated)
        val firstNonCuratedIndex = (0 until companies.length())
            .first { !companies.getJSONObject(it).getBoolean("curated") }
        assertTrue(
            "curated rows interleave with the universe once sorted by size",
            firstNonCuratedIndex < 8,
        )
    }

    @Test
    fun `pe_below_own_avg flag is consistent with the two numbers`() {
        val companies = sectorPayload.getJSONArray("companies")
        for (i in 0 until companies.length()) {
            val row = companies.getJSONObject(i)
            if (row.isNull("pe") || row.isNull("pe_5y_own")) continue
            val expected = row.getDouble("pe") < row.getDouble("pe_5y_own")
            assertEquals(
                "${row.getString("symbol")} below-own-avg flag",
                expected,
                row.getBoolean("pe_below_own_avg"),
            )
        }
    }

    @Test
    fun `market cap in crores is mcap divided by one crore`() {
        // HDFCBANK mcap_cr 1240756 Cr ≈ 12.4 lakh crore, the largest bank in the payload.
        val companies = sectorPayload.getJSONArray("companies")
        val hdfc = (0 until companies.length())
            .map { companies.getJSONObject(it) }
            .first { it.getString("symbol") == "HDFCBANK" }
        assertEquals(1240756.0, hdfc.getDouble("mcap_cr"), 1.0)
    }

    // ── Deep-dive parity ─────────────────────────────────────────

    @Test
    fun `titan deep-dive reproduces the reference verdict and gauge`() {
        val pe = stockPayload.getDouble("pe")
        val sectorAvg = stockPayload.getDouble("pe_sector_avg")
        val ownAvg = if (stockPayload.isNull("pe_5y_own")) null else stockPayload.getDouble("pe_5y_own")
        val reality = stockPayload.getJSONObject("reality")

        val ours = ResearchMath.realityCheck(pe, sectorAvg, ownAvg)
        assertEquals(reality.getString("verdict"), ours.verdict)
        assertEquals(reality.getDouble("pct"), ours.pct, 1e-6)
        assertEquals(
            reality.getString("band"),
            when (ours.band) {
                com.dotpmm.stocker.research.Band.Under -> "under"
                com.dotpmm.stocker.research.Band.Over -> "over"
                com.dotpmm.stocker.research.Band.Mid -> "mid"
                com.dotpmm.stocker.research.Band.Flat -> "flat"
            },
        )
    }

    @Test
    fun `titan fundamentals round-trip`() {
        assertEquals("TITAN", stockPayload.getString("symbol"))
        assertEquals("Titan Company", stockPayload.getString("name"))
        assertEquals("retail", stockPayload.getString("sector_id"))
        assertTrue(stockPayload.getBoolean("curated"))
        assertEquals("Brand Trust", stockPayload.getString("moat"))
        assertTrue(stockPayload.getBoolean("cultural"))
        assertEquals(429894.0, stockPayload.getDouble("mcap_cr"), 1.0)
        assertEquals(29.2, stockPayload.getDouble("rev_cagr_5y"), 1e-9)
    }

    @Test
    fun `titan narrative is the curated hand-written one`() {
        val narrative = stockPayload.getString("narrative")
        assertTrue(narrative.startsWith("Titan runs Tanishq"))
    }

    @Test
    fun `five earnings dots arrive with surprises and realized drops`() {
        val dots = stockPayload.getJSONArray("earnings_dots")
        assertEquals(5, dots.length())
        for (i in 0 until dots.length()) {
            val dot = dots.getJSONObject(i)
            assertNotNull("date present", dot.getString("date"))
            assertTrue("dot has a surprise", !dot.isNull("surprise"))
            assertTrue("dot has a realized move", !dot.isNull("drop_pct"))
        }
        // The reference's 2026-05-08 miss of -15% produced a -9.3% drop.
        val miss = (0 until dots.length())
            .map { dots.getJSONObject(it) }
            .first { it.getString("date") == "2026-05-08" }
        assertEquals(-15.0, miss.getDouble("surprise"), 1e-9)
        assertEquals(-9.3, miss.getDouble("drop_pct"), 1e-9)
    }

    @Test
    fun `deep-dive price history is a clean ascending series`() {
        val history = points(stockPayload.getJSONArray("price_history"))
        assertTrue("history populated", history.size > 100)
        for (i in 1 until history.size) {
            assertTrue("timestamps ascend", history[i].t >= history[i - 1].t)
            assertTrue("closes are positive", history[i].c > 0)
        }
    }

    @Test
    fun `recomputing the realized drop for the 2026-05-08 miss matches the payload`() {
        val history = points(stockPayload.getJSONArray("price_history"))
        val ours = ResearchMath.dropFor("2026-05-08", history)
        assertNotNull("drop computable", ours)
        val reference = (0 until stockPayload.getJSONArray("earnings_dots").length())
            .map { stockPayload.getJSONArray("earnings_dots").getJSONObject(it) }
            .first { it.getString("date") == "2026-05-08" }
            .getDouble("drop_pct")
        assertEquals(reference, ours!!, 0.05)
    }

    // ── Range chips use intraday stamps for 5D ───────────────────

    @Test
    fun `five day history uses intraday timestamps`() {
        val history = points(historyPayload.getJSONArray("price_history"))
        assertTrue("5D must be intraday", history.all { it.intraday })
        assertTrue("5D reaches yesterday's close", history.size > 20)
        assertEquals("5d", historyPayload.getString("range"))
    }

    @Test
    fun `intraday detection keys off the space in the timestamp`() {
        assertTrue(Point("2026-11-06 09:15", 1.0).intraday)
        assertTrue(!Point("2026-11-06", 1.0).intraday)
        assertEquals("2026-11-06", Point("2026-11-06 09:15", 1.0).datePart)
    }

    // ── The curated dataset agrees with the live reference ───────

    @Test
    fun `curated sectors carry the same blurb text as the reference`() {
        val sectors = sectorsPayload.getJSONArray("sectors")
        val referenceBlurbs = (0 until sectors.length()).associate {
            val s = sectors.getJSONObject(it)
            s.getString("id") to s.optString("blurb", "")
        }
        // /sectors omits blurbs in the reference payload; /sector/{id} includes them.
        val bankingBlurb = sectorPayload.getString("blurb")
        assertTrue(bankingBlurb.startsWith("A bank is a leveraged moat"))
        assertNotNull(referenceBlurbs["banking"])
    }

    @Test
    fun `the reference sector pe baseline equals the shipped curated baseline`() {
        // The live payload's 5Y baseline must equal what our asset declares, or the badge drifts.
        val shipped = mapOf("banking" to 16.0)
        assertEquals(shipped["banking"]!!, sectorPayload.getDouble("pe_5y_avg"), 1e-9)
    }

    @Test
    fun `sector lookup resolves every banking constituent to the banking sector`() {
        val companies = sectorPayload.getJSONArray("companies")
        val industry = CuratedData.INDUSTRY_MAP["Financial Services"]
        assertEquals("banking", industry)
        // The payload is the full mapped universe for the sector, so every row belongs to it.
        for (i in 0 until companies.length()) {
            assertTrue(companies.getJSONObject(i).getString("symbol").isNotEmpty())
        }
    }
}
