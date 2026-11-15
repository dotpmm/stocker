package com.dotpmm.stocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.theme.StockerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Interaction tests for the controls whose behaviour is a product rule, not a visual choice:
 * the segmented control must move the selection, and the filter toggles must change the row set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class InteractionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `tapping a segment selects it`() {
        var selected = 3
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    SegmentedControl(
                        labels = listOf("5D", "1M", "6M", "1Y"),
                        selectedIndex = selected,
                        onSelect = { selected = it },
                    )
                }
            }
        }
        compose.onNodeWithText("5D").performClick()
        compose.waitForIdle()
        assertEquals("tapping 5D selects index 0", 0, selected)

        compose.onNodeWithText("6M").performClick()
        compose.waitForIdle()
        assertEquals("tapping 6M selects index 2", 2, selected)
    }

    @Test
    fun `every chip stays displayed and tappable after a selection change`() {
        var selected by mutableIntStateOf(3)
        compose.setContent {
            StockerTheme {
                Box(Modifier.fillMaxSize()) {
                    SegmentedControl(
                        labels = listOf("5D", "1M", "6M", "1Y"),
                        selectedIndex = selected,
                        onSelect = { selected = it },
                    )
                }
            }
        }
        for (label in listOf("5D", "1M", "6M", "1Y")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        compose.onNodeWithText("1M").performClick()
        compose.waitForIdle()
        assertEquals(1, selected)
        // After the pill moves, the labels must still all be present.
        for (label in listOf("5D", "1M", "6M", "1Y")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun `a single-label segmented control does not crash`() {
        compose.setContent {
            StockerTheme {
                SegmentedControl(labels = listOf("ONLY"), selectedIndex = 0, onSelect = {})
            }
        }
        compose.onNodeWithText("ONLY").assertIsDisplayed()
    }
}
