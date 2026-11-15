package com.dotpmm.stocker.ui.theme

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.settings.AppSettings
import com.dotpmm.stocker.settings.Density as DensitySetting
import com.dotpmm.stocker.settings.GrainLevel
import com.dotpmm.stocker.settings.MetalFinish
import com.dotpmm.stocker.settings.ShapeLanguage

/** Corner radii for the current shape setting. */
data class ShapeTokens(val sm: Dp, val md: Dp, val lg: Dp, val xl: Dp, val pill: Dp) {
    val plate: Shape get() = RoundedCornerShape(lg)
    val panel: Shape get() = RoundedCornerShape(xl)
    val chip: Shape get() = RoundedCornerShape(pill)
    fun corner(size: Dp): Shape = RoundedCornerShape(size)
}

/** Row heights and padding for the current density setting. */
data class SpacingTokens(
    val rowHeight: Dp,
    val cardPad: Dp,
    val sectionGap: Dp,
    val rowGap: Dp,
    val textScale: Float,
)

val LocalShape = staticCompositionLocalOf { ShapeTokens(8.dp, 14.dp, 22.dp, 26.dp, 999.dp) }
val LocalSpacing = staticCompositionLocalOf { SpacingTokens(44.dp, 16.dp, 48.dp, 6.dp, 1f) }

/** How much grain to lay over gradient surfaces (design.md §5 caps it at 0.07). */
val LocalGrain = staticCompositionLocalOf { 0.05f }

/**
 * True when the user has disabled animations, either in the OS or via the app setting. Every
 * transform collapses to an instant fade, charts render fully drawn, and the burst never runs.
 */
val LocalReducedMotion = androidx.compose.runtime.compositionLocalOf { false }

/** The live settings object, so screens can honour data and behaviour flags too. */
val LocalSettings = staticCompositionLocalOf { AppSettings() }

/** True on a touch device; the design doc's cursor glow is absent there by rule. */
val LocalTouchDevice = staticCompositionLocalOf { true }

private val scheme = darkColorScheme()

/** Maps the metal finish setting to a brush strength. */
fun finishStrength(finish: MetalFinish): Float = when (finish) {
    MetalFinish.Off -> 0f
    MetalFinish.Subtle -> 0.55f
    MetalFinish.Standard -> 1f
    MetalFinish.Pronounced -> 1.5f
}

fun shapeTokensFor(language: ShapeLanguage): ShapeTokens = when (language) {
    ShapeLanguage.Sharp -> ShapeTokens(2.dp, 4.dp, 6.dp, 8.dp, 4.dp)
    ShapeLanguage.Soft -> ShapeTokens(6.dp, 10.dp, 16.dp, 20.dp, 12.dp)
    ShapeLanguage.Rounded -> ShapeTokens(8.dp, 14.dp, 22.dp, 26.dp, 999.dp)
    ShapeLanguage.Pill -> ShapeTokens(12.dp, 18.dp, 28.dp, 34.dp, 999.dp)
}

fun spacingTokensFor(density: DensitySetting): SpacingTokens = when (density) {
    DensitySetting.Compact -> SpacingTokens(38.dp, 12.dp, 34.dp, 4.dp, 0.94f)
    DensitySetting.Comfortable -> SpacingTokens(44.dp, 16.dp, 48.dp, 6.dp, 1f)
    DensitySetting.Airy -> SpacingTokens(54.dp, 20.dp, 60.dp, 10.dp, 1.05f)
}

fun grainFor(level: GrainLevel): Float = when (level) {
    GrainLevel.Off -> 0f
    GrainLevel.Whisper -> 0.025f
    GrainLevel.Fine -> 0.05f
    GrainLevel.Heavy -> 0.07f
}

/** Reads the OS animation scale so reduced motion is obeyed rather than assumed. */
fun reduceMotionEnabled(context: android.content.Context): Boolean {
    val duration = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    } catch (e: Exception) {
        1f
    }
    val transition = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    } catch (e: Exception) {
        1f
    }
    return duration == 0f || transition == 0f
}

/**
 * Applies appearance and behaviour settings, then paints the single 118° metal wall behind
 * everything.
 *
 * Palette changes rebuild the whole tree via [key] rather than through snapshot reads on every
 * colour: reading `Ink.TextPrimary` is a plain field access, and the key re-roots composition when
 * the palette actually changes. That keeps scrolling free of hundreds of snapshot subscriptions.
 */
@Composable
fun StockerTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val osReduced = remember(context) { reduceMotionEnabled(context) }
    val reduced = osReduced || !settings.motionEnabled

    // Apply before composing the subtree so the first frame already has the right colours.
    Ink.apply(settings.baseTone, settings.accentIntensity)
    Metal.apply(finishStrength(settings.metalFinish))

    val shapes = remember(settings.shape) { shapeTokensFor(settings.shape) }
    val spacing = remember(settings.density) { spacingTokensFor(settings.density) }
    val grain = remember(settings.grain) { grainFor(settings.grain) }

    CompositionLocalProvider(
        LocalReducedMotion provides reduced,
        LocalSettings provides settings,
        LocalShape provides shapes,
        LocalSpacing provides spacing,
        LocalGrain provides grain,
        LocalTouchDevice provides true,
    ) {
        // Re-root when the palette changes so every colour read picks up the new values.
        key(settings.baseTone, settings.accentIntensity, settings.metalFinish, settings.shape, settings.density) {
            MaterialTheme(colorScheme = scheme) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Metal.wall)
                        .then(if (grain > 0f) Modifier.grainOverlay(grain) else Modifier)
                        .background(Metal.ambient),
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Film grain over the wall so large gradients don't band (design.md §5).
 *
 * Drawn as a single tiled shader rather than per-pixel dots: the old scatter approach issued a
 * quarter of a million draw calls per frame, which was the dominant cost while scrolling.
 */
fun Modifier.grainOverlay(alpha: Float): Modifier = composed {
    val tile = remember { GrainTile.bitmap() }
    drawWithContent {
        drawContent()
        drawRect(
            brush = ShaderBrush(
                ImageShader(tile, TileMode.Repeated, TileMode.Repeated),
            ),
            alpha = alpha,
        )
    }
}

/** A small pre-rendered noise tile, generated once and tiled by the GPU. */
private object GrainTile {
    private var cached: ImageBitmap? = null

    fun bitmap(): ImageBitmap {
        cached?.let { return it }
        val size = 96
        val pixels = IntArray(size * size)
        var seed = 0x9E3779B9.toInt()
        for (i in pixels.indices) {
            // xorshift keeps the noise stable between frames without allocating.
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 17)
            seed = seed xor (seed shl 5)
            val v = (seed ushr 24) and 0xFF
            // Only the brightest few percent lift, so the tile reads as sparse grain.
            val a = if (v > 175) (v - 175) * 3 else 0
            pixels[i] = (a.coerceAtMost(255) shl 24) or 0x00FFFFFF
        }
        val android = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        android.setPixels(pixels, 0, size, 0, 0, size, size)
        val image = android.asImageBitmap()
        cached = image
        return image
    }
}

/**
 * Ambient glow that follows the container's rounded corners on all four sides.
 *
 * The light is drawn *inside* the clip, so it never leaks past a corner: a bloom anchored top-left
 * plus two rim passes — one vertical, one horizontal — which together light every edge while the
 * bottom-right stays dim. This is what makes a panel read as lit by one beam rather than as a
 * rectangle with a bright top only.
 */
fun Modifier.ambientGlow(
    shape: Shape? = null,
    radius: Dp? = null,
    intensity: Float = 1f,
): Modifier = composed {
    val shapes = LocalShape.current
    val resolvedShape = shape ?: shapes.plate
    val resolvedRadius = radius ?: shapes.lg
    val settings = LocalSettings.current
    if (!settings.ambientGlow || intensity <= 0f) {
        return@composed this.clip(resolvedShape)
    }
    this
        .clip(resolvedShape)
        .drawWithContent {
            drawContent()
            val r = resolvedRadius.toPx()
            val corner = CornerRadius(r, r)
            // Bloom where the beam lands.
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.10f * intensity),
                    0.45f to Color.White.copy(alpha = 0.035f * intensity),
                    1f to Color.Transparent,
                    center = Offset(size.width * 0.14f, 0f),
                    radius = size.width * 0.95f,
                ),
            )
            // Vertical rim: top bright, bottom faint — same beam direction.
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.16f * intensity),
                    0.30f to Color.White.copy(alpha = 0.03f * intensity),
                    1f to Color.White.copy(alpha = 0.05f * intensity),
                ),
                size = Size(size.width, size.height),
                cornerRadius = corner,
                style = Stroke(width = 1.4.dp.toPx()),
            )
            // Horizontal rim: left bright, right faint — the 118° shade.
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.10f * intensity),
                    0.55f to Color.White.copy(alpha = 0.015f * intensity),
                    1f to Color.White.copy(alpha = 0.03f * intensity),
                ),
                size = Size(size.width, size.height),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}

/**
 * A raised, top-lit metal plate (design.md §4a) — reserved for surfaces that must read as raised:
 * the nav bar, the hero, the active segment, primary actions, sheets.
 *
 * Everything else uses [flat], which keeps the radius and the base fill but drops the light
 * gradients and the bevel. Repeated metal everywhere was both visually noisy and the second-largest
 * cost per frame.
 */
fun Modifier.metal(
    surface: Color = Ink.Surface1,
    shape: Shape? = null,
    radius: Dp? = null,
    lifted: Boolean = false,
): Modifier = composed {
    val shapes = LocalShape.current
    val resolvedShape = shape ?: shapes.plate
    val resolvedRadius = radius ?: shapes.lg
    val strength = finishStrength(LocalSettings.current.metalFinish)

    this
        .drawBehind {
            if (strength > 0f) {
                drawRoundRect(
                    color = Color(0x66000000),
                    topLeft = Offset(0f, 1.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(resolvedRadius.toPx(), resolvedRadius.toPx()),
                )
            }
        }
        .clip(resolvedShape)
        .background(surface)
        .then(if (strength > 0f) Modifier.background(Metal.lit).background(Metal.rake) else Modifier)
        .drawWithContent {
            drawContent()
            val corner = CornerRadius(resolvedRadius.toPx(), resolvedRadius.toPx())
            if (strength > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.11f * strength * (if (lifted) 1.45f else 1f)),
                        0.22f to Color.White.copy(alpha = 0.02f * strength),
                        1f to Color.Transparent,
                    ),
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            drawRoundRect(
                color = if (lifted) Ink.BorderFocus.copy(alpha = 0.35f) else Color(0x0AFFFFFF),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}

/**
 * A flat surface: the base fill, the radius, and one hairline. No gradients, no bevel, no shadow.
 *
 * This is the default for rows, list cards and grouped controls, so metal stays meaningful.
 */
fun Modifier.flat(
    surface: Color = Ink.Surface1,
    shape: Shape? = null,
    radius: Dp? = null,
    bordered: Boolean = true,
): Modifier = composed {
    val shapes = LocalShape.current
    val resolvedShape = shape ?: shapes.plate
    val resolvedRadius = radius ?: shapes.lg
    this
        .clip(resolvedShape)
        .background(surface)
        .then(
            if (bordered) {
                Modifier.drawBehind {
                    drawRoundRect(
                        color = Color(0x0CFFFFFF),
                        cornerRadius = CornerRadius(resolvedRadius.toPx(), resolvedRadius.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            } else {
                Modifier
            },
        )
}

/** Glass: translucent white over the beam, hairline border, inner top bevel. */
fun Modifier.glass(
    shape: Shape? = null,
    intensity: Color = Ink.Glass,
    strong: Boolean = false,
): Modifier = composed {
    val shapes = LocalShape.current
    val resolved = shape ?: shapes.plate
    this
        .clip(resolved)
        .background(if (strong) Ink.GlassStrong else intensity)
        .background(Metal.lit)
        .drawWithContent {
            drawContent()
            drawRoundRect(
                color = Ink.BorderSubtle,
                cornerRadius = CornerRadius(shapes.lg.toPx(), shapes.lg.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
}

/**
 * The "light running along the border" hairline (design.md §8.2). A short blurred streak travels the
 * top edge on a slow loop — travel is on the gradient, never on layout.
 */
fun Modifier.borderTraveler(color: Color, progress: Float, widthRatio: Float = 0.22f): Modifier =
    drawBehind {
        val w = size.width
        val streak = w * widthRatio
        val x = -streak + (w + streak) * progress
        val y = 1.dp.toPx()
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.35f to color.copy(alpha = 0.05f),
                0.72f to color.copy(alpha = 0.45f),
                1f to Color.Transparent,
                startX = x,
                endX = x + streak,
            ),
            start = Offset(x, y),
            end = Offset(x + streak, y),
            strokeWidth = 1.4.dp.toPx(),
        )
    }

/** Text gradient for hero numerals only (design.md §3.4). */
val NumeralGradient: Brush
    get() = Brush.verticalGradient(
        0.30f to Ink.TextPrimary,
        1f to Color(0x59FAFAFA),
    )
