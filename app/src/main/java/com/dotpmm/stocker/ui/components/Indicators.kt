package com.dotpmm.stocker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.ui.theme.Obsidian

fun Tone.color(): Color = when (this) {
    Tone.Positive -> Obsidian.Positive
    Tone.Neutral -> Obsidian.Neutral
    Tone.Negative -> Obsidian.Negative
}

/** Square 6 dp marker used instead of round dots or outlined chips. */
@Composable
fun ToneMarker(tone: Tone, modifier: Modifier = Modifier) {
    Box(modifier.size(6.dp).background(tone.color()))
}

/** Thin square-ended bar showing the share of positive / neutral / negative articles. */
@Composable
fun ToneBar(positive: Int, neutral: Int, negative: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (positive > 0) Box(Modifier.weight(positive.toFloat()).fillMaxHeight().background(Obsidian.Positive))
        if (neutral > 0) Box(Modifier.weight(neutral.toFloat()).fillMaxHeight().background(Obsidian.Neutral))
        if (negative > 0) Box(Modifier.weight(negative.toFloat()).fillMaxHeight().background(Obsidian.Negative))
    }
}

/** Minimal line chart: no axes, no fill. Optional end dot. */
@Composable
fun Sparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    showEndDot: Boolean = false,
) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val max = points.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val inset = 3.dp.toPx()
        val usableH = size.height - inset * 2
        val stepX = size.width / (points.size - 1)
        fun point(i: Int) = Offset(i * stepX, inset + usableH * (1f - (points[i] - min) / range))

        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until points.size) lineTo(point(i).x, point(i).y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (showEndDot) drawCircle(color, radius = 3.dp.toPx(), center = point(points.lastIndex))
    }
}
