package com.dotpmm.stocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.dotpmm.stocker.research.Band
import com.dotpmm.stocker.research.CompanyRow
import com.dotpmm.stocker.research.EarningsDot
import com.dotpmm.stocker.research.MacroSnapshot
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.Reality
import com.dotpmm.stocker.research.SectorCard
import com.dotpmm.stocker.research.StockDeep
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.PriceChart
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.VerdictPill
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.StockerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose rendering smoke tests on the JVM. These catch the failures a unit test can't: a
 * composable that throws on measure, a chart that crashes on a degenerate series, a stroke
 * measuring path that NPEs. Rendering happens for real through Robolectric's layout pipeline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class ComposeRenderTest {

    @get:Rule
    val compose = createComposeRule()

    private val session = listOf(
        Point("2025-01-01", 100.0),
        Point("2025-01-02", 104.0),
        Point("2025-01-03", 99.0),
        Point("2025-01-06", 108.0),
        Point("2025-01-07", 112.0),
    )

    @Test
    fun `price chart renders a normal series`() {
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    PriceChart(points = session, direction = Direction.Up)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `price chart survives an empty series`() {
        compose.setContent {
            StockerTheme {
                PriceChart(points = emptyList(), direction = Direction.Flat)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `price chart survives a single point`() {
        compose.setContent {
            StockerTheme {
                PriceChart(points = listOf(Point("2025-01-01", 100.0)), direction = Direction.Up)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `price chart survives a perfectly flat series`() {
        // A zero range is a classic divide-by-zero crash in chart code.
        val flat = (1..20).map { Point("2025-01-${it.toString().padStart(2, '0')}", 100.0) }
        compose.setContent {
            StockerTheme {
                PriceChart(points = flat, direction = Direction.Flat)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `price chart renders earnings dots without crashing`() {
        compose.setContent {
            StockerTheme {
                PriceChart(
                    points = session,
                    direction = Direction.Down,
                    earningsDots = listOf("2025-01-03" to -4.2, "2025-01-06" to 9.1),
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `sparkline renders both directions`() {
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    Sparkline(points = listOf(1f, 3f, 2f, 5f), direction = Direction.Up)
                    Sparkline(points = listOf(5f, 3f, 4f, 1f), direction = Direction.Down)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `sparkline survives degenerate input`() {
        compose.setContent {
            StockerTheme {
                Sparkline(points = emptyList(), direction = Direction.Flat)
                Sparkline(points = listOf(5f), direction = Direction.Up)
                Sparkline(points = listOf(5f, 5f, 5f), direction = Direction.Flat)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `change text renders the arrow and the number`() {
        compose.setContent {
            StockerTheme {
                ChangeText(value = 2.4)
            }
        }
        compose.onNodeWithText("▲ 2.4%").assertIsDisplayed()
    }

    @Test
    fun `change text renders a loss with a down arrow`() {
        compose.setContent {
            StockerTheme {
                ChangeText(value = -1.56, fractionDigits = 2)
            }
        }
        compose.onNodeWithText("▼ 1.56%").assertIsDisplayed()
    }

    @Test
    fun `change text renders an em dash for missing data`() {
        compose.setContent {
            StockerTheme {
                ChangeText(value = null)
            }
        }
        compose.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun `change text omits the arrow when asked`() {
        compose.setContent {
            StockerTheme {
                ChangeText(value = 18.2, showGlyph = false)
            }
        }
        compose.onNodeWithText("18.2%").assertIsDisplayed()
    }

    @Test
    fun `segmented control renders every range label`() {
        compose.setContent {
            StockerTheme {
                SegmentedControl(
                    labels = listOf("5D", "1M", "6M", "1Y"),
                    selectedIndex = 3,
                    onSelect = {},
                )
            }
        }
        for (label in listOf("5D", "1M", "6M", "1Y")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun `every verdict pill renders its own label`() {
        compose.setContent {
            StockerTheme {
                androidx.compose.foundation.layout.Column {
                    VerdictPill(Verdict.Overheated)
                    VerdictPill(Verdict.Discounted)
                    VerdictPill(Verdict.Consolidating)
                }
            }
        }
        compose.onNodeWithText("OVERHEATED").assertIsDisplayed()
        compose.onNodeWithText("DISCOUNTED").assertIsDisplayed()
        compose.onNodeWithText("CONSOLIDATING").assertIsDisplayed()
    }
}
