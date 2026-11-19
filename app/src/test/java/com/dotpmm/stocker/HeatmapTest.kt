package com.dotpmm.stocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dotpmm.stocker.research.HeatmapSector
import com.dotpmm.stocker.research.HeatmapStock
import com.dotpmm.stocker.ui.components.squarify
import com.dotpmm.stocker.ui.screens.MarketHeatmap
import com.dotpmm.stocker.ui.theme.StockerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.max

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class HeatmapTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `squarify fills the box without overlaps and keeps caller indices`() {
        val weights = (1..10).map { (11 - it).toFloat() }
        val cells = squarify(weights, 360f, 160f)

        assertEquals(10, cells.size)
        assertEquals("every weight gets a cell", weights.indices.toSet(), cells.map { it.index }.toSet())
        assertEquals(
            "cells cover the whole box",
            (360.0 * 160.0),
            cells.sumOf { it.area.toDouble() },
            0.5,
        )
        for (cell in cells) {
            assertTrue("x in bounds", cell.x >= -0.01f && cell.x + cell.width <= 360.01f)
            assertTrue("y in bounds", cell.y >= -0.01f && cell.y + cell.height <= 160.01f)
            assertTrue("positive size", cell.width > 0f && cell.height > 0f)
        }
        for (i in cells.indices) {
            for (j in i + 1 until cells.size) {
                val a = cells[i]
                val b = cells[j]
                val overlapW = (minOf(a.x + a.width, b.x + b.width) - maxOf(a.x, b.x)).coerceAtLeast(0f)
                val overlapH = (minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y)).coerceAtLeast(0f)
                assertTrue("cells ${a.index} and ${b.index} overlap", overlapW * overlapH < 0.01f)
            }
        }
    }

    @Test
    fun `squarify makes roughly square tiles from equal weights`() {
        val cells = squarify(List(9) { 1f }, 300f, 300f)
        assertEquals(9, cells.size)
        for (cell in cells) {
            val ratio = max(cell.width / cell.height, cell.height / cell.width)
            assertTrue("aspect ratio $ratio should stay near square", ratio < 4f)
        }
    }

    @Test
    fun `squarify drops bad weights and handles empty input`() {
        assertTrue(squarify(emptyList(), 100f, 100f).isEmpty())
        assertTrue(squarify(listOf(0f, -1f, Float.NaN), 100f, 100f).isEmpty())
        val cells = squarify(listOf(0f, 2f, Float.NaN, 1f), 100f, 100f)
        assertEquals(listOf(1, 3), cells.map { it.index }.sorted())
    }

    @Test
    fun `tapping a tile pins the quote card and details opens the deep dive`() {
        val sector = HeatmapSector(
            name = "Financial Services",
            stocks = listOf(
                HeatmapStock("HDFCBANK", "HDFC Bank", 1711.2, 1.29, 8_400_000L, 1725.0, 1698.0, 1_300_000.0),
                HeatmapStock("ICICIBANK", "ICICI Bank", 1298.4, -0.03, 6_100_000L, 1304.0, 1290.0, 900_000.0),
            ),
        )
        var opened: String? = null
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    MarketHeatmap(
                        sectors = listOf(sector),
                        loading = false,
                        repository = null,
                        onOpenStock = { opened = it },
                    )
                }
            }
        }
        compose.onNodeWithText("HDFCBANK").assertIsDisplayed()
        compose.onNodeWithText("HDFCBANK").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Details").assertIsDisplayed()
        compose.onNodeWithText("₹1,711").assertIsDisplayed()
        compose.onNodeWithText("8.40M").assertIsDisplayed()
        compose.onNodeWithText("₹1,698 – ₹1,725").assertIsDisplayed()
        compose.onNodeWithText("Details").performClick()
        compose.waitForIdle()
        assertEquals("HDFCBANK", opened)
        compose.onNodeWithText("Details").assertDoesNotExist()
    }
}
