package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.color
import com.dotpmm.stocker.ui.theme.gradFrom
import com.dotpmm.stocker.ui.theme.gradTo
import com.dotpmm.stocker.ui.theme.glow
import kotlin.math.abs

/**
 * The price chart — the app's centerpiece (design.md §9.1).
 *
 * Choreography, in order: line draws in via stroke-dashoffset over 1100ms → the under-curve
 * gradient fades in at 780ms → the underglow arrives last → the endpoint dot pops. Grid is
 * horizontal hairlines only. Dragging moves a 1px crosshair; the tooltip follows with a lerp so it
 * trails like a fluid.
 */
@Composable
fun PriceChart(
    points: List<Point>,
    direction: Direction,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    earningsDots: List<Pair<String, Double?>> = emptyList(),
    onScrub: (Int?) -> Unit = {},
    showGrid: Boolean = true,
    showFill: Boolean = true,
    showGlow: Boolean = true,
) {
    val reduced = LocalReducedMotion.current
    val seriesKey = remember(points) { points.hashCode() }
    val lineProgress = remember(seriesKey) { Animatable(if (reduced) 1f else 0f) }
    val areaAlpha = remember(seriesKey) { Animatable(if (reduced) 1f else 0f) }
    val glowAlpha = remember(seriesKey) { Animatable(if (reduced) 1f else 0f) }
    val dotScale = remember(seriesKey) { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(seriesKey) {
        if (reduced) {
            lineProgress.snapTo(1f); areaAlpha.snapTo(1f); glowAlpha.snapTo(1f); dotScale.snapTo(1f)
            return@LaunchedEffect
        }
        lineProgress.snapTo(0f); areaAlpha.snapTo(0f); glowAlpha.snapTo(0f); dotScale.snapTo(0f)
        lineProgress.animateTo(1f, tween(Motion.Cinematic, easing = Motion.Fluid))
        areaAlpha.animateTo(1f, tween(400, easing = Motion.Emerge))
        glowAlpha.animateTo(1f, tween(400, easing = Motion.Emerge))
        dotScale.animateTo(1f, Motion.springPop)
    }

    var scrubX by remember(seriesKey) { mutableStateOf<Float?>(null) }
    val colors = remember(direction) { Triple(direction.color(), direction.glow(), direction.gradFrom()) }
    val minMax = remember(points) {
        if (points.isEmpty()) 0f to 1f else {
            val values = points.map { it.c.toFloat() }
            values.min() to values.max()
        }
    }

    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(seriesKey) {
                    detectTapGestures(
                        onPress = { offset ->
                            scrubX = offset.x
                            onScrub(indexAt(offset.x, size.width.toFloat(), points.size))
                        },
                        onTap = { offset ->
                            scrubX = null
                            onScrub(null)
                        },
                    )
                }
                .pointerInput(seriesKey) {
                    detectHorizontalDragGestures(
                        onDragEnd = { scrubX = null; onScrub(null) },
                        onDragCancel = { scrubX = null; onScrub(null) },
                        onHorizontalDrag = { change, _ ->
                            scrubX = change.position.x
                            onScrub(indexAt(change.position.x, size.width.toFloat(), points.size))
                        },
                    )
                },
        ) {
            if (points.size < 2) return@Canvas
            val padTop = 12.dp.toPx()
            val padBottom = 8.dp.toPx()
            val (lo, hi) = minMax
            val span = (hi - lo).takeIf { it > 0f } ?: 1f
            val usableH = size.height - padTop - padBottom
            val stepX = size.width / (points.size - 1)

            fun yAt(value: Float): Float = padTop + usableH * (1f - (value - lo) / span)
            fun pointAt(index: Int) = Offset(index * stepX, yAt(points[index].c.toFloat()))

            if (showGrid) {
                for (i in 0..4) {
                    val y = padTop + usableH * i / 4f
                    drawLine(Ink.BorderSubtle, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
            }

            val line = Path()
            for (i in points.indices) {
                val p = pointAt(i)
                if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
            }

            val area = Path().apply {
                addPath(line)
                lineTo(size.width, size.height - padBottom)
                lineTo(0f, size.height - padBottom)
                close()
            }

            // Under-curve gradient: quiet (0.22 → 0), fades in after the line lands.
            // Alpha is interpolated to zero at the tail — never overwritten with a flat value,
            // or the fill turns into a saturated block (design.md §3.1, hard rule 5).
            if (showFill && areaAlpha.value > 0.01f) {
                val from = colors.third
                drawPath(
                    area,
                    brush = Brush.verticalGradient(
                        0f to from.copy(alpha = from.alpha * areaAlpha.value),
                        1f to from.copy(alpha = 0f),
                    ),
                )
            }

            // The line itself. While drawing in it needs a dash offset, which costs a PathMeasure;
            // once complete the dash is dropped so scrolling only issues a plain stroke.
            val drawn = lineProgress.value >= 0.999f
            val length = if (drawn) 0f else measureLength(line)
            drawPath(
                line,
                color = colors.first.copy(alpha = 0.35f + 0.65f * glowAlpha.value),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (drawn) {
                        null
                    } else {
                        PathEffect.dashPathEffect(floatArrayOf(length * lineProgress.value, length), 0f)
                    },
                ),
            )
            // Underglow pass — the signature "emitting light" effect.
            if (showGlow && glowAlpha.value > 0.02f) {
                drawPath(
                    line,
                    color = colors.second.copy(alpha = colors.second.alpha * 0.5f * glowAlpha.value),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = if (drawn) {
                            null
                        } else {
                            PathEffect.dashPathEffect(floatArrayOf(length * lineProgress.value, length), 0f)
                        },
                    ),
                )
            }

            // Earnings dots: black core, red ring.
            for (dot in earningsDots) {
                val index = indexForDate(points, dot.first) ?: continue
                val p = pointAt(index)
                drawCircle(Ink.Void, radius = 3.4.dp.toPx(), center = p)
                drawCircle(
                    Ink.Down, radius = 3.4.dp.toPx(), center = p,
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }

            // Endpoint dot pops last.
            if (dotScale.value > 0.02f) {
                val end = pointAt(points.lastIndex)
                drawCircle(colors.first.copy(alpha = 0.25f * dotScale.value), radius = 8.dp.toPx() * dotScale.value, center = end)
                drawCircle(colors.first, radius = 3.2.dp.toPx() * dotScale.value, center = end)
                drawCircle(Ink.Void, radius = 1.2.dp.toPx() * dotScale.value, center = end)
            }

            // Crosshair.
            val sx = scrubX
            if (sx != null && lineProgress.value > 0.9f) {
                val index = indexAt(sx, size.width, points.size).coerceIn(0, points.lastIndex)
                val cxp = index * stepX
                drawLine(
                    Ink.TextPrimary.copy(alpha = 0.12f),
                    Offset(cxp, 0f),
                    Offset(cxp, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                val dot = pointAt(index)
                drawCircle(Ink.TextPrimary, radius = 3.dp.toPx(), center = dot)
                drawCircle(direction.color(), radius = 1.4.dp.toPx(), center = dot)
            }
        }
    }
}

private fun DrawScope.measureLength(path: Path): Float {
    val measure = androidx.compose.ui.graphics.PathMeasure()
    measure.setPath(path, false)
    return measure.length.coerceAtLeast(1f)
}

private fun indexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    val ratio = (x / width).coerceIn(0f, 1f)
    return Math.round(ratio * (count - 1))
}

private fun indexForDate(points: List<Point>, date: String): Int? {
    if (points.isEmpty()) return null
    var best: Int? = null
    for (i in points.indices) {
        if (points[i].datePart >= date) {
            best = i
            break
        }
    }
    return best
}

/**
 * Sparkline (design.md §9.3): 1.25px line in the direction colour, mini gradient under-fill at
 * 0.15, no glow at this size, drawn in with a fast 400ms dash.
 */
@Composable
fun Sparkline(
    points: List<Float>,
    direction: Direction,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    strokeWidth: Dp = 1.25.dp,
    drawAnimation: Boolean = true,
) {
    val reduced = LocalReducedMotion.current
    val key = remember(points) { points.hashCode() }
    val progress = remember(key) { Animatable(if (reduced || !drawAnimation) 1f else 0f) }
    LaunchedEffect(key) {
        if (reduced || !drawAnimation) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(400, easing = Motion.Fluid))
        }
    }

    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val lo = points.min()
        val hi = points.max()
        val span = (hi - lo).takeIf { it > 0f } ?: 1f
        val inset = 2.dp.toPx()
        val usableH = size.height - inset * 2
        val stepX = size.width / (points.size - 1)
        fun yAt(v: Float) = inset + usableH * (1f - (v - lo) / span)

        val path = Path()
        for (i in points.indices) {
            val x = i * stepX
            val y = yAt(points[i])
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (fill) {
            val area = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                area,
                brush = Brush.verticalGradient(
                    0f to direction.gradFrom().copy(alpha = 0.15f),
                    1f to Color.Transparent,
                ),
            )
        }
        val drawn = progress.value >= 0.999f
        if (drawn) {
            // Static sparkline: a plain stroke, no PathMeasure and no dash — this is the path taken
            // for every row while scrolling, so it must stay allocation-free.
            drawPath(
                path,
                color = direction.color(),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        } else {
            val measure = androidx.compose.ui.graphics.PathMeasure()
            measure.setPath(path, false)
            val length = measure.length.coerceAtLeast(1f)
            drawPath(
                path,
                color = direction.color(),
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(length * progress.value, length), 0f,
                    ),
                ),
            )
        }
    }
}
