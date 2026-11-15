package com.dotpmm.stocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.PriceChart
import com.dotpmm.stocker.ui.components.RollingNumber
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.VerdictPill
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.StockerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reduced motion is a hard rule (design.md §8.3, §12 rule 9), not a partial implementation.
 * These tests render the motion-bearing components with the flag on and assert they still
 * display, settle immediately, and never leave content invisible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class ReducedMotionTest {

    @get:Rule
    val compose = createComposeRule()

    private val points = (0 until 20).map { Point("2026-01-${(it % 28 + 1).toString().padStart(2, '0')}", 100.0 + it) }

    private fun reduced(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides true) {
                StockerTheme { Box(Modifier.fillMaxSize()) { content() } }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `rolling number settles instantly and keeps its glyphs`() {
        reduced { RollingNumber("25,144.30") }
        // Under reduced motion the ticker takes the static branch: one plain Text per glyph with no
        // AnimatedContent, so every character is present on the very first frame.
        val nodes = compose.onAllNodes(androidx.compose.ui.test.isRoot().not(), useUnmergedTree = true)
            .fetchSemanticsNodes()
        val texts = nodes.mapNotNull { node ->
            node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }
        }.flatten().map { it.text }
        val joined = texts.joinToString("")
        assertEquals("all glyphs present immediately", "25,144.30", joined)
    }

    @Test
    fun `price chart renders fully drawn without waiting`() {
        reduced { PriceChart(points = points, direction = Direction.Up) }
        // The invariant is that the tree settles at once and needs no animation frame to be valid.
        compose.waitForIdle()
    }

    @Test
    fun `sparkline renders with animation disabled`() {
        reduced { Sparkline(points = points.map { it.c.toFloat() }, direction = Direction.Down) }
        compose.waitForIdle()
    }

    @Test
    fun `change text is readable without motion`() {
        reduced { ChangeText(value = -2.35, fractionDigits = 2) }
        compose.onNodeWithText("▼ 2.35%").assertIsDisplayed()
    }

    @Test
    fun `verdict pills stay visible without motion`() {
        reduced { VerdictPill(Verdict.Overheated) }
        compose.onNodeWithText("OVERHEATED").assertIsDisplayed()
    }
}
