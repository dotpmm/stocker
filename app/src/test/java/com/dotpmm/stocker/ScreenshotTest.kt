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
    fun `nav bar`() {
        compose.setContent {
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
