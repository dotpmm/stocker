package com.dotpmm.stocker

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.research.HeatmapSector
import com.dotpmm.stocker.research.HeatmapStock
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
import com.dotpmm.stocker.ui.screens.MarketHeatmap
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.StockerTheme
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.metal
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Draws the design surface into real bitmaps on the JVM and writes them to
 * `build/screenshots/`, so the visual language can be inspected on a machine where the
 * Android emulator cannot run. This is the visual verification path for this build.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private val series = (0 until 61).map { i ->
        Point("2026-01-${(i % 28 + 1).toString().padStart(2, '0')}", 100.0 + Math.sin(i / 6.0) * 12 + i * 0.6)
    }

    private val sampleHeatmap = listOf(
        HeatmapSector(
            "Financial Services",
            listOf(
                HeatmapStock("HDFCBANK", "HDFC Bank", 1711.2, 1.29, 8_421_000, 1724.0, 1698.4, 1_301_000.0),
                HeatmapStock("ICICIBANK", "ICICI Bank", 1298.4, -0.03, 6_118_000, 1304.9, 1290.2, 911_000.0),
                HeatmapStock("SBIN", "State Bank of India", 812.6, 1.34, 9_904_000, 818.0, 799.1, 724_000.0),
                HeatmapStock("KOTAKBANK", "Kotak Mahindra Bank", 1762.5, 1.24, 2_114_000, 1770.2, 1739.5, 351_000.0),
                HeatmapStock("AXISBANK", "Axis Bank", 1101.2, 1.18, 3_882_000, 1108.0, 1089.1, 339_000.0),
                HeatmapStock("BAJFINANCE", "Bajaj Finance", 941.8, 0.05, 2_771_000, 949.2, 935.0, 281_000.0),
                HeatmapStock("BAJAJFINSV", "Bajaj Finserv", 1712.0, 1.39, 812_000, 1721.4, 1686.0, 274_000.0),
                HeatmapStock("RBLBANK", "RBL Bank", 238.4, -0.36, 1_804_000, 241.0, 236.2, 14_000.0),
            ),
        ),
        HeatmapSector(
            "Information Technology",
            listOf(
                HeatmapStock("INFY", "Infosys", 1582.1, -2.84, 5_190_000, 1611.0, 1575.5, 655_000.0),
                HeatmapStock("TCS", "Tata Consultancy", 3899.0, -2.22, 2_402_000, 3980.0, 3881.0, 1_410_000.0),
                HeatmapStock("HCLTECH", "HCL Technologies", 1801.4, -2.02, 1_910_000, 1839.0, 1794.0, 488_000.0),
                HeatmapStock("TECHM", "Tech Mahindra", 1602.2, -2.12, 1_144_000, 1638.0, 1596.0, 157_000.0),
                HeatmapStock("WIPRO", "Wipro", 262.8, -1.44, 2_701_000, 266.9, 261.5, 274_000.0),
            ),
        ),
        HeatmapSector(
            "Oil Gas & Consumable Fuels",
            listOf(
                HeatmapStock("RELIANCE", "Reliance Industries", 1412.0, 0.07, 7_402_000, 1420.0, 1401.0, 1_912_000.0),
                HeatmapStock("ONGC", "Oil & Natural Gas", 246.8, -0.02, 8_211_000, 249.0, 245.5, 310_000.0),
                HeatmapStock("COALINDIA", "Coal India", 402.1, 0.97, 6_002_000, 405.0, 396.4, 247_000.0),
            ),
        ),
        HeatmapSector(
            "Automobile and Auto Components",
            listOf(
                HeatmapStock("MARUTI", "Maruti Suzuki", 10980.0, 5.95, 1_101_000, 11100.0, 10402.0, 345_000.0),
                HeatmapStock("M&M", "Mahindra & Mahindra", 3021.5, 4.45, 3_402_000, 3055.0, 2901.0, 378_000.0),
                HeatmapStock("BAJAJ-AUTO", "Bajaj Auto", 9104.0, 1.57, 602_000, 9180.0, 8950.0, 254_000.0),
                HeatmapStock("EICHERMOT", "Eicher Motors", 4810.0, 4.30, 811_000, 4855.0, 4620.0, 132_000.0),
            ),
        ),
        HeatmapSector(
            "Telecommunication",
            listOf(
                HeatmapStock("BHARTIARTL", "Bharti Airtel", 1642.4, 0.69, 4_802_000, 1650.0, 1629.0, 981_000.0),
            ),
        ),
        HeatmapSector(
            "Healthcare",
            listOf(
                HeatmapStock("SUNPHARMA", "Sun Pharma", 1781.2, 1.55, 1_501_000, 1790.0, 1755.0, 427_000.0),
                HeatmapStock("CIPLA", "Cipla", 1502.1, -0.88, 902_000, 1519.0, 1496.0, 121_000.0),
            ),
        ),
    )

    /** Forces a measure + layout + draw pass, then reads the window's pixels back. */
    private fun shoot(name: String) {
        compose.waitForIdle()
        val activity = compose.activity
        val root = activity.window.decorView
        activity.runOnUiThread {
            val width = if (root.width > 0) root.width else 1080
            val height = if (root.height > 0) root.height else 2340
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(outDir, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `component gallery`() {
        compose.setContent {
            StockerTheme {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Monochrome Exchange", style = Type.Section)
                    RollingNumber("25,144.30")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Sparkline(series.map { it.c.toFloat() }, Direction.Up, Modifier.size(340.dp, 44.dp))
                    Box(
                        Modifier
                            .size(340.dp, 70.dp)
                            .metal(Ink.Surface1, RoundedCornerShape(22.dp), 22.dp),
                    )
                    PriceChart(series, Direction.Up, height = 150.dp)
                }
            }
        }
        shoot("01-gallery")
    }

    @Test
    fun `market heatmap`() {
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    MarketHeatmap(
                        sectors = sampleHeatmap,
                        loading = false,
                        repository = null,
                        onOpenStock = {},
                    )
                }
            }
        }
        shoot("04-heatmap")
    }

    @Test
    fun `nav bar`() {        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(
                        items = listOf(
                            NavItem("Markets", StockerIcons.Compass),
                            NavItem("News", StockerIcons.News),
                            NavItem("More", StockerIcons.Settings),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    )
                }
            }
        }
        shoot("02-nav")
    }

    @Test
    fun `metal plate bevel`() {
        compose.setContent {
            StockerTheme {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Metal surfaces", style = Type.Section)
                    Box(
                        Modifier
                            .size(340.dp, 96.dp)
                            .metal(Ink.Surface1, RoundedCornerShape(22.dp), 22.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("SURFACE 1 · RAISED PLATE", style = Type.Micro)
                        }
                    }
                    Box(
                        Modifier
                            .size(340.dp, 96.dp)
                            .metal(Ink.Surface2, RoundedCornerShape(22.dp), 22.dp, lifted = true),
                    )
                    Box(
                        Modifier
                            .size(340.dp, 96.dp)
                            .metal(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp),
                    )
                    PriceChart(series, Direction.Down, height = 160.dp)
                }
            }
        }
        shoot("03-metal")
    }
}
