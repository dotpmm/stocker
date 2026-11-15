package com.dotpmm.stocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.FloatingNavBar
import com.dotpmm.stocker.ui.components.NavItem
import com.dotpmm.stocker.ui.components.PriceChart
import com.dotpmm.stocker.ui.components.RollingNumber
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.VerdictPill
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.StockerTheme
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.metal
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verification harness for a machine with no working emulator: it inflates real Compose
 * hierarchies through Robolectric on a phone-sized viewport and asserts they measure, lay out and
 * settle without throwing — including the degenerate inputs chart code usually dies on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class LayoutHarnessTest {

    @get:Rule
    val compose = createComposeRule()

    private val series = (0 until 61).map { i ->
        Point("2026-01-${(i % 28 + 1).toString().padStart(2, '0')}", 100.0 + Math.sin(i / 6.0) * 12 + i * 0.6)
    }

    private fun assertLaysOut(content: @Composable () -> Unit) {
        compose.setContent { StockerTheme { Box(Modifier.fillMaxSize()) { content() } } }
        compose.waitForIdle()
        compose.onNode(isRoot()).fetchSemanticsNode()
    }

    @Test
    fun `component gallery lays out`() = assertLaysOut {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Monochrome Exchange", style = Type.Section)
            RollingNumber("25,144.30")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VerdictPill(Verdict.Overheated)
                VerdictPill(Verdict.Discounted)
                VerdictPill(Verdict.Consolidating)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChangeText(2.4)
                ChangeText(-1.56, fractionDigits = 2)
                ChangeText(null)
            }
            SegmentedControl(listOf("5D", "1M", "6M", "1Y"), 3, {})
            Sparkline(series.map { it.c.toFloat() }, Direction.Up, Modifier.size(320.dp, 46.dp))
            PriceChart(series, Direction.Up, height = 170.dp)
            Box(
                Modifier
                    .size(300.dp, 90.dp)
                    .metal(Ink.Surface1, RoundedCornerShape(22.dp), 22.dp),
            )
        }
    }

    @Test
    fun `nav bar lays out`() = assertLaysOut {
        Box(Modifier.fillMaxSize()) {
            FloatingNavBar(
                items = listOf(
                    NavItem("Markets", StockerIcons.Compass),
                    NavItem("News", StockerIcons.News),
                    NavItem("More", StockerIcons.Settings),
                ),
                selectedIndex = 0,
                onSelect = {},
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            )
        }
    }

    @Test
    fun `chart panel lays out with earnings dots`() = assertLaysOut {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            PriceChart(
                points = series,
                direction = Direction.Down,
                height = 220.dp,
                earningsDots = listOf(
                    "2026-01-05" to 3.5,
                    "2026-01-12" to -2.1,
                    "2026-01-20" to 18.9,
                ),
            )
        }
    }
}
