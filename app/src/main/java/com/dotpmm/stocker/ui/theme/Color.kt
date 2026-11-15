package com.dotpmm.stocker.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dotpmm.stocker.settings.AccentIntensity
import com.dotpmm.stocker.settings.BaseTone

/**
 * One complete colour set, produced by [paletteFor] from the tone and accent settings and swapped
 * wholesale when the user changes appearance.
 */
data class Palette(
    val void: Color,
    val base: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textGhost: Color,
    val borderSubtle: Color,
    val borderFocus: Color,
    val glass: Color,
    val glassStrong: Color,
    val up: Color,
    val upText: Color,
    val upGlow: Color,
    val upDim: Color,
    val down: Color,
    val downText: Color,
    val downGlow: Color,
    val downDim: Color,
    val flat: Color,
    val upGradFrom: Color,
    val upGradTo: Color,
    val downGradFrom: Color,
    val downGradTo: Color,
    val whiteGradFrom: Color,
    val whiteGradTo: Color,
    /** Wall gradient stops, top-left to bottom-right along the beam. */
    val wallStops: List<Color>,
    /** Intensity of the lit-edge gradient at the top of a plate. */
    val litAlpha: Float,
    /** Intensity of the raking sheen. */
    val rakeAlpha: Float,
    /** Intensity of the ambient beam. */
    val ambientAlpha: Float,
)

/**
 * Builds the palette for a tone. The direction pair is the only chroma in every variant — the tone
 * only moves the monochrome ramp, never the hues.
 *
 * AMOLED is the darkest setting that still reads as metal: the wall bottoms out near true black but
 * the plates keep a slightly raised luminance and a lit top edge, so depth survives.
 */
fun paletteFor(tone: BaseTone, accent: AccentIntensity): Palette {
    val ramp = when (tone) {
        BaseTone.Amoled -> intArrayOf(0xFF000000.toInt(), 0xFF050505.toInt(), 0xFF0A0A0A.toInt(), 0xFF121212.toInt(), 0xFF1A1A1A.toInt())
        BaseTone.Void -> intArrayOf(0xFF030303.toInt(), 0xFF070707.toInt(), 0xFF0C0C0C.toInt(), 0xFF131313.toInt(), 0xFF1B1B1B.toInt())
        BaseTone.Charcoal -> intArrayOf(0xFF0C0C0C.toInt(), 0xFF131313.toInt(), 0xFF1B1B1B.toInt(), 0xFF232323.toInt(), 0xFF2C2C2C.toInt())
        BaseTone.Graphite -> intArrayOf(0xFF121212.toInt(), 0xFF191919.toInt(), 0xFF212121.toInt(), 0xFF2A2A2A.toInt(), 0xFF343434.toInt())
    }
    val wallStops = when (tone) {
        BaseTone.Amoled -> listOf(0xFF000000.toInt(), 0xFF070707.toInt(), 0xFF030303.toInt(), 0xFF080808.toInt())
        BaseTone.Void -> listOf(0xFF0C0C0C.toInt(), 0xFF080808.toInt(), 0xFF050505.toInt(), 0xFF0A0A0A.toInt())
        BaseTone.Charcoal -> listOf(0xFF1E1E1E.toInt(), 0xFF171717.toInt(), 0xFF101010.toInt(), 0xFF151515.toInt())
        BaseTone.Graphite -> listOf(0xFF262626.toInt(), 0xFF1E1E1E.toInt(), 0xFF171717.toInt(), 0xFF1D1D1D.toInt())
    }
    // AMOLED keeps a touch more lit-edge contrast so plates still read as plates.
    val litBoost = if (tone == BaseTone.Amoled) 1.4f else 1f

    // Accent only scales the direction pair's brightness; the hue never changes.
    val accentScale = when (accent) {
        AccentIntensity.Muted -> 1f
        AccentIntensity.Standard -> 1.14f
        AccentIntensity.Vivid -> 1.3f
    }

    return Palette(
        void = Color(ramp[0]),
        base = Color(ramp[1]),
        surface1 = Color(ramp[2]),
        surface2 = Color(ramp[3]),
        surface3 = Color(ramp[4]),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFFA1A1AA),
        textMuted = Color(0xFF52525B),
        textGhost = Color(0xFF27272A),
        borderSubtle = Color(0x14FFFFFF),
        borderFocus = Color(0x40FFFFFF),
        glass = Color(0x0AFFFFFF),
        glassStrong = Color(0x14FFFFFF),
        up = scaleColor(0xFF5FA87A.toInt(), accentScale),
        upText = scaleColor(0xFF7CBF93.toInt(), accentScale),
        upGlow = scaleColor(0x475FA87A.toInt(), accentScale),
        upDim = scaleColor(0x1A5FA87A.toInt(), accentScale),
        down = scaleColor(0xFFD3705F.toInt(), accentScale),
        downText = scaleColor(0xFFE08A7C.toInt(), accentScale),
        downGlow = scaleColor(0x47D3705F.toInt(), accentScale),
        downDim = scaleColor(0x1AD3705F.toInt(), accentScale),
        flat = Color(0xFF52525B),
        upGradFrom = scaleColor(0x385FA87A.toInt(), accentScale),
        upGradTo = Color(0x005FA87A),
        downGradFrom = scaleColor(0x38D3705F.toInt(), accentScale),
        downGradTo = Color(0x00D3705F),
        whiteGradFrom = Color(0x38FFFFFF),
        whiteGradTo = Color(0x00FFFFFF),
        wallStops = wallStops.map { Color(it) },
        litAlpha = 0.12f * litBoost,
        rakeAlpha = 0.06f,
        ambientAlpha = 0.11f * litBoost,
    )
}

/** Scales a colour's RGB toward white by [factor], preserving its alpha. */
private fun scaleColor(argb: Int, factor: Float): Color {
    val a = (argb ushr 24) and 0xFF
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    fun boost(c: Int): Int = (c + (255 - c) * (factor - 1f)).toInt().coerceIn(0, 255)
    return Color(boost(r), boost(g), boost(b), a)
}

/**
 * The live palette. Backed by Compose state so every reader recomposes when the user changes
 * appearance; the `Ink.TextPrimary` call sites keep working unchanged.
 */
object Ink {
    private var current by mutableStateOf(paletteFor(BaseTone.Void, AccentIntensity.Muted))

    fun apply(tone: BaseTone, accent: AccentIntensity) {
        current = paletteFor(tone, accent)
    }

    val Void: Color get() = current.void
    val Base: Color get() = current.base
    val Surface1: Color get() = current.surface1
    val Surface2: Color get() = current.surface2
    val Surface3: Color get() = current.surface3

    val TextPrimary: Color get() = current.textPrimary
    val TextSecondary: Color get() = current.textSecondary
    val TextMuted: Color get() = current.textMuted
    val TextGhost: Color get() = current.textGhost

    val BorderSubtle: Color get() = current.borderSubtle
    val BorderFocus: Color get() = current.borderFocus
    val Glass: Color get() = current.glass
    val GlassStrong: Color get() = current.glassStrong

    val Up: Color get() = current.up
    val UpText: Color get() = current.upText
    val UpGlow: Color get() = current.upGlow
    val UpDim: Color get() = current.upDim

    val Down: Color get() = current.down
    val DownText: Color get() = current.downText
    val DownGlow: Color get() = current.downGlow
    val DownDim: Color get() = current.downDim

    val Flat: Color get() = current.flat

    val UpGradFrom: Color get() = current.upGradFrom
    val UpGradTo: Color get() = current.upGradTo
    val DownGradFrom: Color get() = current.downGradFrom
    val DownGradTo: Color get() = current.downGradTo
    val WhiteGradFrom: Color get() = current.whiteGradFrom
    val WhiteGradTo: Color get() = current.whiteGradTo

    internal fun palette(): Palette = current
}

/** Direction of a market move. Colour is derived from this, never chosen ad hoc. */
enum class Direction { Up, Down, Flat }

fun directionOf(value: Double?): Direction = when {
    value == null -> Direction.Flat
    value > 0 -> Direction.Up
    value < 0 -> Direction.Down
    else -> Direction.Flat
}

/** The chart/text colour for a direction. Flat is always gray — never a washed-out red/green. */
fun Direction.color(): Color = when (this) {
    Direction.Up -> Ink.Up
    Direction.Down -> Ink.Down
    Direction.Flat -> Ink.Flat
}

fun Direction.textColor(): Color = when (this) {
    Direction.Up -> Ink.UpText
    Direction.Down -> Ink.DownText
    Direction.Flat -> Ink.TextSecondary
}

fun Direction.glow(): Color = when (this) {
    Direction.Up -> Ink.UpGlow
    Direction.Down -> Ink.DownGlow
    Direction.Flat -> Color.Transparent
}

fun Direction.gradFrom(): Color = when (this) {
    Direction.Up -> Ink.UpGradFrom
    Direction.Down -> Ink.DownGradFrom
    Direction.Flat -> Ink.WhiteGradFrom
}

fun Direction.gradTo(): Color = when (this) {
    Direction.Up -> Ink.UpGradTo
    Direction.Down -> Ink.DownGradTo
    Direction.Flat -> Ink.WhiteGradTo
}

/** ▲ / ▼ / nothing — colour must never be the only signal (design.md §9.4). */
fun Direction.glyph(): String = when (this) {
    Direction.Up -> "▲"
    Direction.Down -> "▼"
    Direction.Flat -> ""
}

/**
 * Brushes for the metal finish, rebuilt whenever the palette or finish level changes.
 *
 * The wall is one wide beam entering top-left and gone by ~56% across. Plates get a vertical light
 * falloff plus a raking sheen; [strength] scales both with the user's finish setting.
 */
class MetalPalette(val palette: Palette, val strength: Float) {
    val wall: Brush = Brush.linearGradient(
        0f to palette.wallStops[0],
        0.28f to palette.wallStops[1],
        0.56f to palette.wallStops[2],
        1f to palette.wallStops[3],
    )

    val lit: Brush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = palette.litAlpha * strength),
        0.10f to Color.White.copy(alpha = palette.litAlpha * 0.56f * strength),
        0.26f to Color.White.copy(alpha = palette.litAlpha * 0.22f * strength),
        0.46f to Color.White.copy(alpha = 0f),
    )

    val rake: Brush = Brush.linearGradient(
        0f to Color.White.copy(alpha = palette.rakeAlpha * strength),
        0.42f to Color.White.copy(alpha = 0f),
    )

    val ambient: Brush = Brush.linearGradient(
        0f to Color.White.copy(alpha = palette.ambientAlpha * strength),
        0.20f to Color.White.copy(alpha = palette.ambientAlpha * 0.55f * strength),
        0.38f to Color.White.copy(alpha = palette.ambientAlpha * 0.16f * strength),
        0.56f to Color.White.copy(alpha = 0f),
    )
}

object Metal {
    private var current by mutableStateOf(MetalPalette(Ink.palette(), 1f))

    /** Rebuilds the brushes for a finish strength (0 = flat, 1 = standard, 1.5 = pronounced). */
    fun apply(finishStrength: Float) {
        current = MetalPalette(Ink.palette(), finishStrength)
    }

    fun refresh() {
        current = MetalPalette(Ink.palette(), current.strength)
    }

    val wall: Brush get() = current.wall
    val lit: Brush get() = current.lit
    val rake: Brush get() = current.rake
    val ambient: Brush get() = current.ambient
}
