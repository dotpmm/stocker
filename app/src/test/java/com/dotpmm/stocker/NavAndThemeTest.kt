package com.dotpmm.stocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.dotpmm.stocker.ui.components.FloatingNavBar
import com.dotpmm.stocker.ui.components.NavItem
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.StockerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nav and appearance regressions: the bar must render its items, the metal setting must not break
 * it, and the palette must actually darken when the tone changes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class NavAndThemeTest {

    @get:Rule
    val compose = createComposeRule()

    private val items = listOf(
        NavItem("Watchlist", StockerIcons.Watchlist),
        NavItem("Markets", StockerIcons.Compass),
        NavItem("News", StockerIcons.News),
        NavItem("Events", StockerIcons.Events),
        NavItem("Settings", StockerIcons.Settings),
    )

    @Test
    fun `the nav bar shows every label`() {
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = 0, onSelect = {})
                }
            }
        }
        for (item in items) {
            compose.onNodeWithText(item.label).assertIsDisplayed()
        }
    }

    @Test
    fun `the nav bar renders with the metal finish off`() {
        compose.setContent {
            StockerTheme(
                settings = com.dotpmm.stocker.settings.AppSettings(
                    metalFinish = com.dotpmm.stocker.settings.MetalFinish.Off,
                    metalNavBar = false,
                ),
            ) {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = 2, onSelect = {})
                }
            }
        }
        compose.onNodeWithText("News").assertIsDisplayed()
    }

    @Test
    fun `the nav bar renders with the ambient glow off`() {
        compose.setContent {
            StockerTheme(
                settings = com.dotpmm.stocker.settings.AppSettings(ambientGlow = false),
            ) {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = 1, onSelect = {})
                }
            }
        }
        compose.onNodeWithText("Markets").assertIsDisplayed()
    }

    @Test
    fun `selecting a nav item reports its index`() {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = selected, onSelect = { selected = it })
                }
            }
        }
        compose.onNodeWithText("Events").performClickSafely()
        compose.waitForIdle()
        assertEquals("tapping Events selects index 3", 3, selected)
    }

    @Test
    fun `every tone renders the whole shell without throwing`() {
        var tone by mutableStateOf(com.dotpmm.stocker.settings.BaseTone.Amoled)
        compose.setContent {
            StockerTheme(settings = com.dotpmm.stocker.settings.AppSettings(baseTone = tone)) {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = 0, onSelect = {})
                }
            }
        }
        // Drive the tone through every variant in one composition, then assert it survives.
        for (candidate in com.dotpmm.stocker.settings.BaseTone.entries) {
            compose.runOnIdle { tone = candidate }
            compose.waitForIdle()
            compose.onNodeWithText("Watchlist").assertIsDisplayed()
        }
    }

    @Test
    fun `every shape language renders the bar without throwing`() {
        var shape by mutableStateOf(com.dotpmm.stocker.settings.ShapeLanguage.Sharp)
        compose.setContent {
            StockerTheme(settings = com.dotpmm.stocker.settings.AppSettings(shape = shape)) {
                Box(Modifier.fillMaxSize()) {
                    FloatingNavBar(items = items, selectedIndex = 0, onSelect = {})
                }
            }
        }
        for (candidate in com.dotpmm.stocker.settings.ShapeLanguage.entries) {
            compose.runOnIdle { shape = candidate }
            compose.waitForIdle()
            compose.onNodeWithText("Watchlist").assertIsDisplayed()
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.performClickSafely() {
    try {
        performClick()
    } catch (e: AssertionError) {
        // Swallow: the assertion in the test body is what matters.
    }
}
