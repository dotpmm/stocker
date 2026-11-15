package com.dotpmm.stocker

import com.dotpmm.stocker.research.CompanyRow
import com.dotpmm.stocker.research.SectorFilters
import com.dotpmm.stocker.research.SortBy
import com.dotpmm.stocker.research.applyFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Filter and sort semantics (FUNCTIONALITY.md §3.2). These are product rules, including the
 * "missing data never silently passes a filter" requirement.
 */
class SectorFilterTest {

    private fun row(
        symbol: String,
        mcapCr: Double? = 10_000.0,
        pe: Double? = 25.0,
        pe5yOwn: Double? = null,
        belowOwn: Boolean = false,
        cagr: Double? = 10.0,
        moat: String = "None",
        monopoly: Boolean = false,
        cultural: Boolean = false,
        defensive: Boolean = false,
    ) = CompanyRow(
        symbol = symbol,
        name = "$symbol Ltd",
        pe = pe,
        pe5yOwn = pe5yOwn,
        peBelowOwnAvg = belowOwn,
        mcapCr = mcapCr,
        revCagr5y = cagr,
        moat = moat,
        monopoly = monopoly,
        cultural = cultural,
        defensive = defensive,
        note = "",
        curated = true,
    )

    private val fixture = listOf(
        row("AAA", mcapCr = 200_000.0, pe = 15.0, cagr = 22.0, moat = "Brand Trust", cultural = true),
        row("BBB", mcapCr = 50_000.0, pe = 35.0, cagr = 8.0, monopoly = true),
        row("CCC", mcapCr = 10_000.0, pe = 45.0, cagr = 30.0, defensive = true, moat = "Scale"),
        row("DDD", mcapCr = null, pe = null, cagr = null),
        row("EEE", mcapCr = 5_000.0, pe = 12.0, cagr = 5.0, moat = "Cost Advantage"),
    )

    // ── Defaults ─────────────────────────────────────────────────

    @Test
    fun `defaults are coffee-can core on and everything else off`() {
        val defaults = SectorFilters.DEFAULT
        assertTrue(defaults.core)
        assertEquals(false, defaults.monopoly)
        assertEquals(false, defaults.cultural)
        assertEquals(false, defaults.defensive)
        assertEquals(false, defaults.peBelowOwn)
        assertEquals(false, defaults.peBelow20)
        assertEquals(false, defaults.cagrAbove15)
        assertEquals(false, defaults.largeCap)
        assertEquals("Any", defaults.moat)
        assertEquals(SortBy.MktCap, defaults.sort)
    }

    @Test
    fun `default filter keeps only the coffee-can core`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT)
        // AAA (cultural), BBB (monopoly), CCC (defensive) survive; DDD and EEE are not core.
        assertEquals(listOf("AAA", "BBB", "CCC"), out.map { it.symbol })
    }

    @Test
    fun `resetting restores exactly the shipped defaults`() {
        val custom = SectorFilters(core = false, monopoly = true, sort = SortBy.Ticker)
        assertEquals(SectorFilters.DEFAULT, custom.copy(core = true, monopoly = false, sort = SortBy.MktCap))
    }

    @Test
    fun `core off shows the whole mapped universe`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false))
        assertEquals(5, out.size)
    }

    // ── Individual structure toggles ─────────────────────────────

    @Test
    fun `monopoly toggle narrows to monopolies`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, monopoly = true))
        assertEquals(listOf("BBB"), out.map { it.symbol })
    }

    @Test
    fun `cultural toggle narrows to cultural moats`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, cultural = true))
        assertEquals(listOf("AAA"), out.map { it.symbol })
    }

    @Test
    fun `defensive toggle narrows to defensive anchors`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, defensive = true))
        assertEquals(listOf("CCC"), out.map { it.symbol })
    }

    @Test
    fun `multiple toggles are ANDed together`() {
        val out = applyFilters(
            fixture,
            SectorFilters.DEFAULT.copy(core = false, monopoly = true, cultural = true),
        )
        assertTrue("no row is both monopoly and cultural", out.isEmpty())
    }

    // ── Valuation ────────────────────────────────────────────────

    @Test
    fun `pe below 20 keeps only cheaper names with a real pe`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, peBelow20 = true))
        assertEquals(setOf("AAA", "EEE"), out.map { it.symbol }.toSet())
    }

    @Test
    fun `pe below 20 excludes rows with a missing pe`() {
        // DDD has pe == null; a missing number must never pass a valuation filter.
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, peBelow20 = true))
        assertTrue(out.none { it.symbol == "DDD" })
    }

    @Test
    fun `pe below own 5y average keeps only flagged rows`() {
        val rows = listOf(
            row("KEEP", belowOwn = true),
            row("DROP", belowOwn = false),
        )
        val out = applyFilters(rows, SectorFilters.DEFAULT.copy(core = false, peBelowOwn = true))
        assertEquals(listOf("KEEP"), out.map { it.symbol })
    }

    // ── Growth & size ────────────────────────────────────────────

    @Test
    fun `cagr filter keeps names above fifteen percent`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, cagrAbove15 = true))
        assertEquals(setOf("AAA", "CCC"), out.map { it.symbol }.toSet())
    }

    @Test
    fun `cagr filter drops rows with a missing cagr`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, cagrAbove15 = true))
        assertTrue(out.none { it.symbol == "DDD" })
    }

    @Test
    fun `large cap filter uses the fifty thousand crore floor inclusively`() {
        val rows = listOf(
            row("BIG", mcapCr = 50_000.0),
            row("HUGE", mcapCr = 100_000.0),
            row("SMALL", mcapCr = 49_999.0),
            row("NONE", mcapCr = null),
        )
        val out = applyFilters(rows, SectorFilters.DEFAULT.copy(core = false, largeCap = true))
        assertEquals(setOf("BIG", "HUGE"), out.map { it.symbol }.toSet())
    }

    // ── Moat select ──────────────────────────────────────────────

    @Test
    fun `moat select matches exactly`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, moat = "Brand Trust"))
        assertEquals(listOf("AAA"), out.map { it.symbol })
    }

    @Test
    fun `any moat is no constraint`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, moat = "Any"))
        assertEquals(5, out.size)
    }

    // ── Sorting ──────────────────────────────────────────────────

    @Test
    fun `default sort is market cap descending`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false))
        assertEquals(listOf("AAA", "BBB", "CCC", "EEE", "DDD"), out.map { it.symbol })
    }

    @Test
    fun `pe ascending pushes the null pe to the end`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, sort = SortBy.PeAsc))
        assertEquals(listOf("EEE", "AAA", "BBB", "CCC", "DDD"), out.map { it.symbol })
    }

    @Test
    fun `cagr descending pushes the null cagr to the end`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, sort = SortBy.CagrDesc))
        assertEquals(listOf("CCC", "AAA", "BBB", "EEE", "DDD"), out.map { it.symbol })
    }

    @Test
    fun `ticker sort is alphabetical`() {
        val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, sort = SortBy.Ticker))
        assertEquals(listOf("AAA", "BBB", "CCC", "DDD", "EEE"), out.map { it.symbol })
    }

    @Test
    fun `sorting never loses rows`() {
        val all = fixture.size
        for (sort in SortBy.entries) {
            val out = applyFilters(fixture, SectorFilters.DEFAULT.copy(core = false, sort = sort))
            assertEquals("sort $sort dropped rows", all, out.size)
        }
    }

    @Test
    fun `filters compose with a sort`() {
        val out = applyFilters(
            fixture,
            SectorFilters.DEFAULT.copy(core = false, cagrAbove15 = true, sort = SortBy.Ticker),
        )
        assertEquals(listOf("AAA", "CCC"), out.map { it.symbol })
    }
}
