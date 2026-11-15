package com.dotpmm.stocker

import com.dotpmm.stocker.research.ResearchMath
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.glyph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Presentation-level rules that the design treats as bugs when violated:
 * directions always carry a glyph (§9.4), missing numbers are an em dash never a zero (§7),
 * and prices are Indian-grouped.
 */
class FormattingTest {

    // ── Direction is never colour-only (§9.4) ─────────────────────

    @Test
    fun `direction of a positive move is up`() {
        assertEquals(Direction.Up, directionOf(2.4))
    }

    @Test
    fun `direction of a negative move is down`() {
        assertEquals(Direction.Down, directionOf(-0.6))
    }

    @Test
    fun `direction of zero is flat, not up`() {
        assertEquals(Direction.Flat, directionOf(0.0))
    }

    @Test
    fun `direction of missing data is flat`() {
        assertEquals(Direction.Flat, directionOf(null))
    }

    @Test
    fun `up carries a triangle glyph`() {
        assertEquals("▲", Direction.Up.glyph())
    }

    @Test
    fun `down carries an inverted triangle glyph`() {
        assertEquals("▼", Direction.Down.glyph())
    }

    @Test
    fun `flat carries no glyph so a zero never reads as a move`() {
        assertEquals("", Direction.Flat.glyph())
    }

    // ── Percentages ──────────────────────────────────────────────

    @Test
    fun `percentage is rendered without a sign because the glyph carries it`() {
        assertEquals("2.4%", Fmt.pct(2.4))
        assertEquals("2.4%", Fmt.pct(-2.4))
    }

    @Test
    fun `percentage respects the requested precision`() {
        assertEquals("0.61%", Fmt.pct(-0.61, 2))
        assertEquals("10.4%", Fmt.pct(10.44, 1))
    }

    @Test
    fun `missing percentage is an em dash never zero`() {
        assertEquals("—", Fmt.pct(null))
    }

    // ── Missing numbers are always an em dash ────────────────────

    @Test
    fun `missing ratio is an em dash`() {
        assertEquals("—", Fmt.ratio(null))
    }

    @Test
    fun `non-positive ratio is an em dash because a negative P E is meaningless`() {
        assertEquals("—", Fmt.ratio(-4.2))
        assertEquals("—", Fmt.ratio(0.0))
    }

    @Test
    fun `ratio renders one decimal`() {
        assertEquals("17.6", Fmt.ratio(17.57377))
        assertEquals("16.0", Fmt.ratio(16.0))
    }

    @Test
    fun `missing market cap is an em dash`() {
        assertEquals("—", Fmt.crore(null))
        assertEquals("—", Fmt.crore(0.0))
    }

    @Test
    fun `missing index value is an em dash`() {
        assertEquals("—", Fmt.index(null))
    }

    // ── Indian digit grouping ────────────────────────────────────

    @Test
    fun `market cap in crores uses indian digit grouping`() {
        // Indian grouping: last three digits, then pairs — 12,40,756 not 1,240,756.
        assertEquals("12,40,756", Fmt.crore(1240756.0))
        assertEquals("4,29,894", Fmt.crore(429894.0))
        assertEquals("9,96,907", Fmt.crore(996907.0))
    }

    @Test
    fun `hero index keeps one decimal with indian grouping`() {
        assertEquals("25,144.3", Fmt.index(25144.3))
    }

    @Test
    fun `price under a thousand keeps two decimals`() {
        assertEquals("₹804.00", Fmt.price(804.0))
    }

    @Test
    fun `price over a thousand drops the decimals`() {
        assertEquals("₹4,845", Fmt.price(4844.9))
    }

    @Test
    fun `compact crore label switches unit at one lakh crore`() {
        assertEquals("₹4.3L Cr", Fmt.croreCompact(429894.0))
        assertEquals("₹12.4L Cr", Fmt.croreCompact(1240756.0))
    }

    @Test
    fun `compact crore uses thousands below one lakh crore`() {
        assertEquals("₹42.0K Cr", Fmt.croreCompact(42000.0))
    }

    // ── Chart date labels ────────────────────────────────────────

    @Test
    fun `chart date renders day and short month`() {
        assertEquals("13 Nov", Fmt.chartDate("2025-11-13"))
    }

    @Test
    fun `chart date strips a leading zero from the day`() {
        assertEquals("3 Nov", Fmt.chartDate("2025-11-03"))
    }

    @Test
    fun `chart date handles an intraday stamp by taking the date part`() {
        assertEquals("6 Nov", Fmt.chartDate("2026-11-06 09:15"))
    }

    @Test
    fun `chart time extracts the clock from an intraday stamp`() {
        assertEquals("09:15", Fmt.chartTime("2026-11-06 09:15"))
    }

    @Test
    fun `chart time is empty for a daily stamp`() {
        assertEquals("", Fmt.chartTime("2026-11-06"))
    }

    @Test
    fun `chart date degrades gracefully on an unparseable stamp`() {
        assertEquals("nonsense", Fmt.chartDate("nonsense"))
    }

    // ── Verdict thresholds agree with the design copy ────────────

    @Test
    fun `overheated copy pairs with a down direction so the label is unambiguous`() {
        // The UI maps Overheated to Down; the wording "OVERHEATED" plus ▼ reads as risk.
        val verdict = ResearchMath.badge(52.0, 30.0)
        assertEquals(Verdict.Overheated, verdict)
    }

    @Test
    fun `discounted copy pairs with an up direction`() {
        assertEquals(Verdict.Discounted, ResearchMath.badge(20.3, 26.0))
    }

    @Test
    fun `consolidating is the neutral middle`() {
        assertEquals(Verdict.Consolidating, ResearchMath.badge(17.0, 16.0))
    }

    // ── Indian grouping edge cases ───────────────────────────────

    @Test
    fun `grouping leaves three digits alone`() {
        assertEquals("999", Fmt.groupIndian("999"))
    }

    @Test
    fun `grouping adds one comma at four digits`() {
        assertEquals("1,000", Fmt.groupIndian("1000"))
    }

    @Test
    fun `grouping pairs digits above the last three`() {
        assertEquals("10,000", Fmt.groupIndian("10000"))
        assertEquals("1,00,000", Fmt.groupIndian("100000"))
        assertEquals("1,23,45,678", Fmt.groupIndian("12345678"))
    }

    @Test
    fun `grouping preserves a negative sign`() {
        assertEquals("-1,00,000", Fmt.groupIndian("-100000"))
    }

    @Test
    fun `price grouping is consistent for large values`() {
        assertEquals("₹1,23,456", Fmt.price(123456.0))
    }
}
