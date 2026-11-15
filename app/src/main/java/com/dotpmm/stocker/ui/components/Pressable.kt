package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion

/**
 * Global press feedback (design.md §8.4). Every interactive surface sinks to ~0.97 and springs
 * back — nothing on the page is ever "dead" under the finger.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
    haptic: Boolean = true,
    role: androidx.compose.ui.semantics.Role = androidx.compose.ui.semantics.Role.Button,
): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val settings = com.dotpmm.stocker.ui.theme.LocalSettings.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        val target = if (pressed) pressedScale else 1f
        scale.animateTo(
            target,
            if (reduced) tween(90) else Motion.springPop,
        )
    }
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                // A short tick makes every press feel physical without adding noise.
                if (haptic && settings.haptics) runCatching {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
                onClick()
            },
        )
}

/**
 * A travelling light sweep for loading skeletons and "thinking" panels (design.md §8.2).
 * Duration 1.4s linear, looping — spinners are banned.
 */
fun Modifier.sweepShimmer(shape: RoundedCornerShape = RoundedCornerShape(8.dp)): Modifier = composed {
    val reduced = LocalReducedMotion.current
    if (reduced) return@composed this.background(Ink.Surface2, shape)
    val transition = rememberInfiniteTransition(label = "sweep")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sweepX",
    )
    this
        .clip(shape)
        .background(Ink.Surface2)
        .drawBehind {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.White.copy(alpha = 0.02f),
                    0.5f to Color.White.copy(alpha = 0.07f),
                    0.55f to Color.White.copy(alpha = 0.02f),
                    1f to Color.Transparent,
                    startX = size.width * (x - 0.6f),
                    endX = size.width * (x + 0.6f),
                ),
            )
        }
}

/**
 * The orbiter: a small white dot travelling the rounded border of a panel — "processing" without a
 * spinner (design.md §8.2). Stops entirely when [active] is false.
 */
@Composable
fun OrbitBorder(
    active: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Ink.TextPrimary,
    radius: Dp = 22.dp,
    durationMs: Int = 3200,
) {
    val reduced = LocalReducedMotion.current
    if (!active || reduced) return
    val transition = rememberInfiniteTransition(label = "orbit")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "orbitT",
    )
    Box(
        modifier.drawBehind {
            val inset = 1.5.dp.toPx()
            val r = radius.toPx()
            val w = size.width - inset * 2
            val h = size.height - inset * 2
            val perimeter = 2 * (w + h) - (8 - 2 * Math.PI.toFloat()) * r
            val d = perimeter * t
            val (px, py) = pointOnRoundedRect(d, inset, inset, w, h, r)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = 0.5f),
                    0.35f to color.copy(alpha = 0.16f),
                    1f to Color.Transparent,
                    center = Offset(px, py),
                    radius = 9.dp.toPx(),
                ),
                radius = 9.dp.toPx(),
                center = Offset(px, py),
            )
            drawCircle(color = color.copy(alpha = 0.9f), radius = 1.6.dp.toPx(), center = Offset(px, py))
        },
    )
}

private fun pointOnRoundedRect(d: Float, ox: Float, oy: Float, w: Float, h: Float, r: Float): Pair<Float, Float> {
    val arc = (Math.PI.toFloat() / 2f) * r
    val straightW = w - 2 * r
    val straightH = h - 2 * r
    var t = d.coerceAtLeast(0f)
    if (t < straightW) return (ox + r + t) to oy
    t -= straightW
    if (t < arc) {
        val a = -Math.PI.toFloat() / 2f + (t / arc) * (Math.PI.toFloat() / 2f)
        return (ox + w - r + r * kotlin.math.cos(a)) to (oy + r + r * kotlin.math.sin(a))
    }
    t -= arc
    if (t < straightH) return (ox + w) to (oy + r + t)
    t -= straightH
    if (t < arc) {
        val a = (t / arc) * (Math.PI.toFloat() / 2f)
        return (ox + w - r + r * kotlin.math.cos(a)) to (oy + h - r + r * kotlin.math.sin(a))
    }
    t -= arc
    if (t < straightW) return (ox + w - r - t) to (oy + h)
    t -= straightW
    if (t < arc) {
        val a = Math.PI.toFloat() / 2f + (t / arc) * (Math.PI.toFloat() / 2f)
        return (ox + r + r * kotlin.math.cos(a)) to (oy + h - r + r * kotlin.math.sin(a))
    }
    t -= arc
    if (t < straightH) return ox to (oy + h - r - t)
    t -= straightH
    val a = Math.PI.toFloat() + (t / arc).coerceAtMost(1f) * (Math.PI.toFloat() / 2f)
    return (ox + r + r * kotlin.math.cos(a)) to (oy + r + r * kotlin.math.sin(a))
}

/**
 * Breathing ambient glow (design.md §8.2). Opacity 0.5→1 over 2.4s, eased. Used only while a
 * panel is actively computing, never as decoration.
 */
@Composable
fun rememberBreath(active: Boolean): Float {
    val reduced = LocalReducedMotion.current
    if (!active || reduced) return 1f
    val transition = rememberInfiniteTransition(label = "breath")
    val value by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = Motion.Emerge), RepeatMode.Reverse),
        label = "breathA",
    )
    return value
}

/** Counts from 0 to [target] once (design.md §8.2). Tabular numerals stop the layout shifting. */
@Composable
fun rememberCountUp(target: Double, durationMs: Int = 900): Double {
    val reduced = LocalReducedMotion.current
    val anim = remember(target) { Animatable(if (reduced) target.toFloat() else 0f) }
    LaunchedEffect(target) {
        if (reduced) {
            anim.snapTo(target.toFloat())
        } else {
            anim.animateTo(target.toFloat(), tween(durationMs, easing = Motion.Emerge))
        }
    }
    return anim.value.toDouble()
}
