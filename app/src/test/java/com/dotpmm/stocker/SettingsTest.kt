package com.dotpmm.stocker

import androidx.test.core.app.ApplicationProvider
import com.dotpmm.stocker.settings.AccentIntensity
import com.dotpmm.stocker.settings.AppSettings
import com.dotpmm.stocker.settings.BaseTone
import com.dotpmm.stocker.settings.Density
import com.dotpmm.stocker.settings.GrainLevel
import com.dotpmm.stocker.settings.MetalFinish
import com.dotpmm.stocker.settings.MotionLevel
import com.dotpmm.stocker.settings.SettingsStore
import com.dotpmm.stocker.settings.ShapeLanguage
import com.dotpmm.stocker.ui.theme.finishStrength
import com.dotpmm.stocker.ui.theme.grainFor
import com.dotpmm.stocker.ui.theme.paletteFor
import com.dotpmm.stocker.ui.theme.shapeTokensFor
import com.dotpmm.stocker.ui.theme.spacingTokensFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The settings contract: defaults are the shipped look, every field round-trips, and the derived
 * theme tokens actually respond to the choice. A decorative toggle would fail these.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class SettingsTest {

    private fun store() = SettingsStore(ApplicationProvider.getApplicationContext())

    // ── Defaults ────────────────────────────────────────────────

    @Test
    fun `shipped defaults are the dark metal look`() {
        val d = AppSettings()
        assertEquals(BaseTone.Void, d.baseTone)
        assertEquals(MetalFinish.Standard, d.metalFinish)
        assertEquals(GrainLevel.Fine, d.grain)
        assertEquals(AccentIntensity.Muted, d.accentIntensity)
        assertEquals(ShapeLanguage.Rounded, d.shape)
        assertEquals(Density.Comfortable, d.density)
        assertEquals(MotionLevel.Full, d.motionLevel)
        assertTrue(d.ambientGlow)
        assertTrue(d.borderTraveler)
        assertTrue(d.searchBurst)
        assertTrue(d.haptics)
        assertTrue(d.directionGlyphs)
    }

    @Test
    fun `every news source is on by default`() {
        val d = AppSettings()
        assertTrue("at least a dozen sources", d.newsSources.size >= 12)
        assertTrue("ET markets included", "et-markets" in d.newsSources)
        assertTrue("SEBI included", "sebi" in d.newsSources)
    }

    // ── Persistence ─────────────────────────────────────────────

    @Test
    fun `a changed value survives a reload`() {
        val s = store()
        s.reset()
        s.update { it.copy(baseTone = BaseTone.Amoled, metalFinish = MetalFinish.Pronounced) }

        val reloaded = store()
        assertEquals(BaseTone.Amoled, reloaded.settings.baseTone)
        assertEquals(MetalFinish.Pronounced, reloaded.settings.metalFinish)
    }

    @Test
    fun `booleans round-trip`() {
        val s = store()
        s.reset()
        s.update { it.copy(ambientGlow = false, haptics = false, searchBurst = false) }
        val reloaded = store()
        assertFalse(reloaded.settings.ambientGlow)
        assertFalse(reloaded.settings.haptics)
        assertFalse(reloaded.settings.searchBurst)
    }

    @Test
    fun `a source selection round-trips`() {
        val s = store()
        s.reset()
        s.update { it.copy(newsSources = setOf("sebi", "rbi")) }
        assertEquals(setOf("sebi", "rbi"), store().settings.newsSources)
    }

    @Test
    fun `numbers are coerced into a sane range`() {
        val s = store()
        s.reset()
        // Persist an out-of-range value directly, then read it back through the loader.
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("app-settings", android.content.Context.MODE_PRIVATE)
            .edit().putInt("refreshHours", 999).putInt("newsWindowDays", -5).apply()
        val reloaded = store()
        assertEquals("refresh hours clamp to 24", 24, reloaded.settings.refreshHours)
        assertEquals("news window clamps to 1", 1, reloaded.settings.newsWindowDays)
    }

    @Test
    fun `reset restores exactly the shipped defaults`() {
        val s = store()
        s.update { it.copy(baseTone = BaseTone.Graphite, metalFinish = MetalFinish.Off, haptics = false) }
        s.reset()
        assertEquals(AppSettings(), s.settings)
        assertEquals(AppSettings(), store().settings)
    }

    // ── Derived theme tokens ────────────────────────────────────

    @Test
    fun `amoled is darker than every other tone`() {
        val amoled = paletteFor(BaseTone.Amoled, AccentIntensity.Muted)
        for (tone in listOf(BaseTone.Void, BaseTone.Charcoal, BaseTone.Graphite)) {
            val other = paletteFor(tone, AccentIntensity.Muted)
            assertTrue(
                "amoled wall must be darker than $tone",
                luminance(amoled.base) <= luminance(other.base),
            )
            assertTrue(
                "amoled surface must be darker than $tone",
                luminance(amoled.surface1) <= luminance(other.surface1),
            )
        }
    }

    @Test
    fun `amoled still lifts plates above the wall so metal survives`() {
        val p = paletteFor(BaseTone.Amoled, AccentIntensity.Muted)
        assertTrue(
            "the plate must be brighter than the void, or it reads as a hole",
            luminance(p.surface1) > luminance(p.void),
        )
        assertTrue("amoled keeps a lit edge", p.litAlpha > 0f)
    }

    @Test
    fun `graphite is lighter than charcoal which is lighter than void`() {
        val void = paletteFor(BaseTone.Void, AccentIntensity.Muted)
        val charcoal = paletteFor(BaseTone.Charcoal, AccentIntensity.Muted)
        val graphite = paletteFor(BaseTone.Graphite, AccentIntensity.Muted)
        assertTrue(luminance(void.base) < luminance(charcoal.base))
        assertTrue(luminance(charcoal.base) < luminance(graphite.base))
    }

    @Test
    fun `accent intensity brightens the direction pair without changing hue`() {
        val muted = paletteFor(BaseTone.Void, AccentIntensity.Muted)
        val vivid = paletteFor(BaseTone.Void, AccentIntensity.Vivid)
        assertTrue("up gets brighter", luminance(vivid.up) > luminance(muted.up))
        assertTrue("down gets brighter", luminance(vivid.down) > luminance(muted.down))
        // Hue family is preserved: green stays greener than red, and vice versa.
        assertTrue("up stays green-dominant", green(vivid.up) > red(vivid.up))
        assertTrue("down stays red-dominant", red(vivid.down) > green(vivid.down))
    }

    @Test
    fun `the only chroma in every palette is the direction pair`() {
        // Walls, surfaces and text must be perfectly neutral gray (R == G == B).
        for (tone in BaseTone.entries) {
            for (accent in AccentIntensity.entries) {
                val p = paletteFor(tone, accent)
                for (c in listOf(p.void, p.base, p.surface1, p.surface2, p.surface3)) {
                    assertEquals("neutral surface at $tone/$accent", red(c), green(c), 0.01f)
                    assertEquals("neutral surface at $tone/$accent", green(c), blue(c), 0.01f)
                }
            }
        }
    }

    @Test
    fun `finish strength maps off to zero and pronounced above standard`() {
        assertEquals(0f, finishStrength(MetalFinish.Off), 1e-6f)
        assertTrue(finishStrength(MetalFinish.Subtle) < finishStrength(MetalFinish.Standard))
        assertTrue(finishStrength(MetalFinish.Pronounced) > finishStrength(MetalFinish.Standard))
    }

    @Test
    fun `grain never exceeds the design cap of 0_07`() {
        for (level in GrainLevel.entries) {
            assertTrue("grain $level exceeds 0.07", grainFor(level) <= 0.07f)
        }
        assertEquals(0f, grainFor(GrainLevel.Off), 1e-6f)
    }

    @Test
    fun `shape language actually changes the radii`() {
        val sharp = shapeTokensFor(ShapeLanguage.Sharp)
        val rounded = shapeTokensFor(ShapeLanguage.Rounded)
        val pill = shapeTokensFor(ShapeLanguage.Pill)
        assertTrue("sharp is tighter than rounded", sharp.lg < rounded.lg)
        assertTrue("pill is rounder than rounded", pill.lg > rounded.lg)
        assertNotEquals(sharp.pill, pill.pill)
    }

    @Test
    fun `density changes row height in the expected direction`() {
        val compact = spacingTokensFor(Density.Compact)
        val comfortable = spacingTokensFor(Density.Comfortable)
        val airy = spacingTokensFor(Density.Airy)
        assertTrue(compact.rowHeight < comfortable.rowHeight)
        assertTrue(airy.rowHeight > comfortable.rowHeight)
        assertTrue(compact.cardPad < airy.cardPad)
    }

    @Test
    fun `motion level off disables motion`() {
        assertFalse(AppSettings(motionLevel = MotionLevel.Off).motionEnabled)
        assertTrue(AppSettings(motionLevel = MotionLevel.Reduced).motionEnabled)
        assertTrue(AppSettings(motionLevel = MotionLevel.Full).motionEnabled)
    }

// ── New appearance/behaviour toggles ───────────────────────

    @Test
    fun `nav metal, swipe and smart start default on and round-trip`() {
        val d = AppSettings()
        assertTrue("nav metal on by default", d.metalNavBar)
        assertTrue("swipe on by default", d.swipeTabs)
        assertTrue("smart start on by default", d.smartStart)

        val s = store()
        s.reset()
        s.update { it.copy(metalNavBar = false, swipeTabs = false, smartStart = false) }
        val reloaded = store().settings
        assertFalse(reloaded.metalNavBar)
        assertFalse(reloaded.swipeTabs)
        assertFalse(reloaded.smartStart)
    }

    @Test
    fun `the dependency graph setting is gone`() {
        // The user asked for the panel to be removed, so no setting should resurrect it.
        val fields = AppSettings::class.java.declaredFields.map { it.name }
        assertFalse("showDependencyGraph must not exist", fields.contains("showDependencyGraph"))
    }

    // ── helpers ─────────────────────────────────────────────────

    private fun red(c: androidx.compose.ui.graphics.Color) = c.red
    private fun green(c: androidx.compose.ui.graphics.Color) = c.green
    private fun blue(c: androidx.compose.ui.graphics.Color) = c.blue
    private fun luminance(c: androidx.compose.ui.graphics.Color) = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}