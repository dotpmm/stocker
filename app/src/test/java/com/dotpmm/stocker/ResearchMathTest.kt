package com.dotpmm.stocker

import com.dotpmm.stocker.research.Band
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.ResearchMath
import com.dotpmm.stocker.research.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reference computations are product logic (FUNCTIONALITY.md §5). These tests pin the exact
 * thresholds and formulas so a future refactor cannot silently drift from the shipped behaviour.
 */
class ResearchMathTest {

    // ── §3.1 verdict badge: 1.12× / 0.92× boundaries ─────────────

    @Test
    fun `badge is consolidating when pe is null`() {
        assertEquals(Verdict.Consolidating, ResearchMath.badge(null, 30.0))
    }

    @Test
    fun `badge is overheated at exactly 1_12x baseline`() {
        // 30 × 1.12 = 33.6
        assertEquals(Verdict.Overheated, ResearchMath.badge(33.6, 30.0))
    }

    @Test
    fun `badge is consolidating just below 1_12x`() {
        assertEquals(Verdict.Consolidating, ResearchMath.badge(33.59, 30.0))
    }

    @Test
    fun `badge is discounted at exactly 0_92x baseline`() {
        // 30 × 0.92 = 27.6
        assertEquals(Verdict.Discounted, ResearchMath.badge(27.6, 30.0))
    }

    @Test
    fun `badge is consolidating just above 0_92x`() {
        assertEquals(Verdict.Consolidating, ResearchMath.badge(27.61, 30.0))
    }

    @Test
    fun `badge boundaries hold for the shipped fmcg baseline`() {
        // FMCG baseline 38.0 → overheated ≥ 42.56, discounted ≤ 34.96
        assertEquals(Verdict.Overheated, ResearchMath.badge(42.56, 38.0))
        assertEquals(Verdict.Consolidating, ResearchMath.badge(38.0, 38.0))
        assertEquals(Verdict.Discounted, ResearchMath.badge(34.96, 38.0))
    }

    // ── Market-cap weighted P/E ──────────────────────────────────

    @Test
    fun `weighted pe averages by market cap not by count`() {
        val pairs = listOf(
            Triple("A", 20.0, 900.0),
            Triple("B", 40.0, 100.0),
        )
        // (20×900 + 40×100) / 1000 = 22.0
        assertEquals(22.0, ResearchMath.weightedPe(pairs)!!, 1e-9)
    }

    @Test
    fun `weighted pe ignores rows missing either value`() {
        val pairs = listOf(
            Triple("A", 20.0, 900.0),
            Triple("B", null, 100.0),
            Triple("C", 40.0, null),
        )
        assertEquals(20.0, ResearchMath.weightedPe(pairs)!!, 1e-9)
    }

    @Test
    fun `weighted pe is null when nothing is usable`() {
        assertNull(ResearchMath.weightedPe(listOf(Triple("A", null, null))))
    }

    @Test
    fun `weighted pe rejects non-positive pe and mcap`() {
        val pairs = listOf(
            Triple("A", -5.0, 100.0),
            Triple("B", 25.0, 0.0),
        )
        assertNull(ResearchMath.weightedPe(pairs))
    }

    // ── 1Y change and hero change ────────────────────────────────

    @Test
    fun `year change compares last against first point`() {
        val points = listOf(Point("2025-01-01", 100.0), Point("2025-06-01", 150.0), Point("2025-12-31", 110.0))
        assertEquals(10.0, ResearchMath.yearChange(points)!!, 1e-9)
    }

    @Test
    fun `year change is null with a single point`() {
        assertNull(ResearchMath.yearChange(listOf(Point("2025-01-01", 100.0))))
    }

    @Test
    fun `hero change compares last against the previous point`() {
        // Today's change is within the series (last vs previous), not vs the first.
        val points = listOf(Point("d1", 100.0), Point("d2", 200.0), Point("d3", 210.0))
        assertEquals(5.0, ResearchMath.heroChange(points)!!, 1e-9)
    }

    @Test
    fun `hero change handles a decline`() {
        val points = listOf(Point("d1", 100.0), Point("d2", 90.0))
        assertEquals(-10.0, ResearchMath.heroChange(points)!!, 1e-9)
    }

    // ── Own 5Y P/E approximation ─────────────────────────────────

    @Test
    fun `own 5y pe averages yearly price to eps ratios`() {
        val eps = listOf(2021 to 10.0, 2022 to 20.0)
        val closes = listOf(Point("2021-12-31", 200.0), Point("2022-12-31", 400.0))
        // 200/10 = 20, 400/20 = 20 → 20
        assertEquals(20.0, ResearchMath.own5yPe(eps, closes)!!, 1e-9)
    }

    @Test
    fun `own 5y pe keeps the last close of each calendar year`() {
        val eps = listOf(2021 to 10.0)
        val closes = listOf(
            Point("2021-01-05", 50.0),
            Point("2021-12-30", 300.0),
            Point("2021-12-31", 400.0),
        )
        // Only one valid year → below the ≥2 requirement.
        assertNull(ResearchMath.own5yPe(eps, closes))
    }

    @Test
    fun `own 5y pe needs at least two valid years`() {
        val eps = listOf(2021 to 10.0, 2022 to 20.0)
        // 2022 has no close in the series, leaving one usable ratio.
        val closes = listOf(Point("2021-12-31", 200.0))
        assertNull(ResearchMath.own5yPe(eps, closes))
    }

    @Test
    fun `own 5y pe skips non-positive eps years`() {
        val eps = listOf(2021 to -5.0, 2022 to 20.0)
        val closes = listOf(Point("2021-12-31", 200.0), Point("2022-12-31", 400.0))
        assertNull(ResearchMath.own5yPe(eps, closes))
    }

    @Test
    fun `own 5y pe ignores zeros when averaging`() {
        val eps = listOf(2021 to -1.0, 2022 to 20.0)
        val closes = listOf(
            Point("2021-12-31", 200.0),
            Point("2022-12-31", 400.0),
        )
        assertNull(ResearchMath.own5yPe(eps, closes))
    }

    // ── 5Y revenue CAGR ──────────────────────────────────────────

    @Test
    fun `cagr computes compound growth across four points`() {
        // 100 → 146.41 over 3 years = 13.55%, which the reference rounds to 1 dp (13.6).
        val cagr = ResearchMath.revenueCagr(listOf(100.0, 110.0, 120.0, 146.41))!!
        assertEquals(13.6, cagr, 1e-9)
    }

    @Test
    fun `cagr needs at least three annual values`() {
        assertNull(ResearchMath.revenueCagr(listOf(100.0, 120.0)))
    }

    @Test
    fun `cagr rejects a zero or negative base`() {
        assertNull(ResearchMath.revenueCagr(listOf(0.0, 100.0, 200.0)))
    }

    @Test
    fun `cagr of a flat series is zero`() {
        assertEquals(0.0, ResearchMath.revenueCagr(listOf(100.0, 100.0, 100.0))!!, 1e-9)
    }

    // ── Reality check + gauge spans 0_55x–1_85x ──────────────────

    @Test
    fun `reality check returns no data without a pe`() {
        val reality = ResearchMath.realityCheck(null, 30.0, null)
        assertEquals("NO DATA", reality.verdict)
        assertEquals(Band.Flat, reality.band)
        assertEquals(0.5, reality.pct, 1e-9)
    }

    @Test
    fun `reality check is priced for perfection at 1_25x sector`() {
        // 30 × 1.25 = 37.5
        val reality = ResearchMath.realityCheck(37.5, 30.0, null)
        assertEquals("PRICED FOR PERFECTION", reality.verdict)
        assertEquals(Band.Over, reality.band)
    }

    @Test
    fun `reality check is discounted at 0_9x sector`() {
        // 30 × 0.9 = 27.0
        val reality = ResearchMath.realityCheck(27.0, 30.0, null)
        assertEquals("DISCOUNTED", reality.verdict)
        assertEquals(Band.Under, reality.band)
    }

    @Test
    fun `reality check is fair value between the bands`() {
        val reality = ResearchMath.realityCheck(30.0, 30.0, null)
        assertEquals("FAIR VALUE", reality.verdict)
        assertEquals(Band.Mid, reality.band)
    }

    @Test
    fun `own average discount wins over the sector test`() {
        // 29 is under its own 5Y average (40 × 0.92 = 36.8) and also mid-band vs sector 30.
        val reality = ResearchMath.realityCheck(29.0, 30.0, 40.0)
        assertEquals("DISCOUNTED", reality.verdict)
        assertEquals(Band.Under, reality.band)
    }

    @Test
    fun `gauge parks at zero below the 0_55x floor`() {
        // 30 × 0.55 = 16.5
        val reality = ResearchMath.realityCheck(10.0, 30.0, null)
        assertEquals(0.0, reality.pct, 1e-9)
    }

    @Test
    fun `gauge saturates at one above the 1_85x ceiling`() {
        // 30 × 1.85 = 55.5
        val reality = ResearchMath.realityCheck(80.0, 30.0, null)
        assertEquals(1.0, reality.pct, 1e-9)
    }

    @Test
    fun `gauge midpoint is the sector average`() {
        // (30 - 16.5) / (55.5 - 16.5) = 13.5 / 39 = 0.3461…
        val reality = ResearchMath.realityCheck(30.0, 30.0, null)
        assertEquals(0.346153, reality.pct, 1e-5)
    }

    @Test
    fun `gauge position is monotonic in pe`() {
        var previous = -1.0
        for (pe in listOf(10.0, 20.0, 30.0, 40.0, 60.0)) {
            val pct = ResearchMath.realityCheck(pe, 30.0, null).pct
            assertTrue("pct must increase with pe", pct >= previous)
            previous = pct
        }
    }

    // ── Earnings dot realized reaction (bisect + 3 trading days) ─

    @Test
    fun `drop pct compares close three trading days after the date`() {
        val history = listOf(
            Point("2025-01-01", 100.0),
            Point("2025-01-02", 101.0),
            Point("2025-01-03", 102.0),
            Point("2025-01-06", 103.0),
            Point("2025-01-07", 110.0),
        )
        // bisect lands on 2025-01-02, +3 → 2025-01-07: 110/101 - 1 = 8.910…
        assertEquals(8.9, ResearchMath.dropFor("2025-01-02", history)!!, 1e-6)
    }

    @Test
    fun `drop pct starts at the next trading day when earnings land on a holiday`() {
        val history = listOf(
            Point("2025-01-01", 100.0),
            Point("2025-01-02", 100.0),
            Point("2025-01-03", 100.0),
            Point("2025-01-06", 100.0),
            Point("2025-01-07", 50.0),
            Point("2025-01-08", 50.0),
            Point("2025-01-09", 60.0),
        )
        // "2025-01-04" (Saturday) bisects to index 3 (2025-01-06); +3 → index 6 (2025-01-09):
        // 60/100 - 1 = -40 → a fall. The bisect is what skips the non-trading day.
        assertEquals(-40.0, ResearchMath.dropFor("2025-01-04", history)!!, 1e-6)
    }

    @Test
    fun `drop pct also works when the earnings date is before the whole series`() {
        val history = listOf(
            Point("2025-02-01", 100.0),
            Point("2025-02-03", 100.0),
            Point("2025-02-04", 100.0),
            Point("2025-02-05", 200.0),
        )
        // bisect lands at index 0; +3 → 2025-02-05: 200/100 - 1 = 100
        assertEquals(100.0, ResearchMath.dropFor("2024-12-31", history)!!, 1e-6)
    }

    @Test
    fun `drop pct is null when there are fewer than three days after`() {
        val history = listOf(Point("2025-01-01", 100.0), Point("2025-01-02", 101.0))
        assertNull(ResearchMath.dropFor("2025-01-01", history))
    }

    @Test
    fun `drop pct is null for empty history`() {
        assertNull(ResearchMath.dropFor("2025-01-01", emptyList()))
    }

    // ── Downsampling (wire format ≤140 points) ───────────────────

    @Test
    fun `downsample leaves short series untouched`() {
        val points = (0 until 50).map { Point("2025-01-${it % 28 + 1}", it.toDouble()) }
        assertEquals(50, ResearchMath.downsample(points).size)
    }

    @Test
    fun `downsample keeps the last point so the chart ends correctly`() {
        val points = (0 until 500).map { Point("t$it", it.toDouble()) }
        val out = ResearchMath.downsample(points)
        assertTrue("should cap near 140", out.size <= 145)
        assertEquals(points.last(), out.last())
    }

    @Test
    fun `downsample is identity at exactly the cap`() {
        val points = (0 until 140).map { Point("t$it", it.toDouble()) }
        assertEquals(140, ResearchMath.downsample(points).size)
    }
}
