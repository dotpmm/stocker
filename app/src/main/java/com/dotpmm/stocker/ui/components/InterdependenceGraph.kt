package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.research.Dependency
import com.dotpmm.stocker.research.SectorCard
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.color

/**
 * Sector interdependence graph (design.md §10.4, FUNCTIONALITY.md §3.1).
 *
 * Nodes are circular plates ringed by the sector's verdict; edges are provider → consumer quadratic
 * Béziers whose width maps dependency strength. Both edge passes draw in once via stroke-dashoffset.
 * Tapping a node isolates its flows — the mobile stand-in for hover isolation — and a second tap
 * opens the sector.
 */
@Composable
fun InterdependenceGraph(
    sectors: List<SectorCard>,
    dependencies: List<Dependency>,
    onOpenSector: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 340.dp,
) {
    val reduced = LocalReducedMotion.current
    val drawProgress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) drawProgress.snapTo(1f) else drawProgress.animateTo(1f, tween(1400, easing = Motion.Fluid))
    }
    var focus by remember { mutableStateOf<String?>(null) }
    val measurer = rememberTextMeasurer()
    val byId = remember(sectors) { sectors.associateBy { it.id } }

    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(sectors) {
                    detectTapGestures { offset ->
                        val node = nodeAt(offset, size.width.toFloat(), size.height.toFloat(), NODE_POSITIONS)
                        when {
                            node == null -> focus = null
                            focus == node -> {
                                focus = null
                                onOpenSector(node)
                            }
                            else -> focus = node
                        }
                    }
                },
        ) {
            if (sectors.isEmpty()) return@Canvas
            val sx = size.width / 600f
            val sy = size.height / 400f
            val scale = minOf(sx, sy)
            val radius = 42.dp.toPx()

            fun position(id: String): Offset {
                val (x, y) = NODE_POSITIONS[id] ?: (300f to 200f)
                return Offset(x * sx + (size.width - 600f * sx) / 2f, y * sy)
            }

            // Edges: dim base pass + brighter flow pass, both drawn in by dash offset.
            for (dep in dependencies) {
                val from = position(dep.from)
                val to = position(dep.to)
                val dimmed = focus != null && dep.from != focus && dep.to != focus
                val alpha = if (dimmed) 0.10f else 1f
                val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
                val dx = to.x - from.x
                val dy = to.y - from.y
                val length = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
                val bend = when (PERPENDICULAR_BEND[dep.from to dep.to]) {
                    true -> 26f
                    false -> -26f
                    null -> 18f
                }
                val control = Offset(
                    mid.x - dy / length * bend,
                    mid.y + dx / length * bend,
                )
                val path = Path().apply {
                    moveTo(from.x, from.y)
                    quadraticTo(control.x, control.y, to.x, to.y)
                }
                val measure = androidx.compose.ui.graphics.PathMeasure()
                measure.setPath(path, false)
                val total = measure.length.coerceAtLeast(1f)
                val dash = PathEffect.dashPathEffect(floatArrayOf(total * drawProgress.value, total), 0f)
                val width = 1.dp.toPx() + dep.weight.toFloat() * 5.dp.toPx()

                drawPath(
                    path,
                    color = Ink.TextPrimary.copy(alpha = 0.07f * alpha),
                    style = Stroke(width = width, cap = StrokeCap.Round, pathEffect = dash),
                )
                drawPath(
                    path,
                    color = Ink.TextPrimary.copy(alpha = 0.24f * alpha),
                    style = Stroke(width = width * 0.45f, cap = StrokeCap.Round, pathEffect = dash),
                )
            }

            // Nodes.
            for (card in sectors) {
                val center = position(card.id)
                val isFocused = focus == card.id
                val ring = when (card.badge) {
                    Verdict.Overheated -> Ink.Down
                    Verdict.Discounted -> Ink.Up
                    Verdict.Consolidating -> Ink.TextSecondary.copy(alpha = 0.5f)
                }
                drawCircle(Ink.Surface2, radius = radius, center = center)
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.07f),
                        1f to Color.Transparent,
                        endY = center.y + radius,
                        startY = center.y - radius,
                    ),
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = ring.copy(alpha = if (isFocused) 1f else 0.75f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (isFocused) 2.4.dp.toPx() else 1.6.dp.toPx()),
                )
                if (isFocused) {
                    drawCircle(ring.copy(alpha = 0.14f), radius = radius * 1.12f, center = center)
                }

                val short = SHORT_NAMES[card.id] ?: card.name.take(6).uppercase()
                drawCentered(measurer, short, Offset(center.x, center.y - radius * 0.34f), Type.MicroData.copy(fontSize = 12.5.sp, color = Ink.TextPrimary))
                val pe = card.peCurrent?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—"
                drawCentered(measurer, "P/E $pe", Offset(center.x, center.y + radius * 0.12f), Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextSecondary))
                val change = card.yearChangePct
                val glyph = when {
                    change == null -> ""
                    change > 0 -> "▲ "
                    change < 0 -> "▼ "
                    else -> ""
                }
                val text = glyph + (change?.let { String.format(java.util.Locale.US, "%.1f%%", kotlin.math.abs(it)) } ?: "—")
                drawCentered(
                    measurer,
                    text,
                    Offset(center.x, center.y + radius * 0.56f),
                    Type.MicroData.copy(
                        fontSize = 13.sp,
                        color = when {
                            change == null -> Ink.TextMuted
                            change > 0 -> Ink.UpText
                            change < 0 -> Ink.DownText
                            else -> Ink.TextMuted
                        },
                    ),
                )
            }
        }
    }
    if (focus != null) {
        val card = byId[focus]
        Column(Modifier.padding(top = 4.dp)) {
            Text(
                "${SHORT_NAMES[focus] ?: focus} · flows isolated · tap again to open",
                style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextMuted),
            )
            card?.blurb?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = Type.Caption, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCentered(
    measurer: TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    drawText(
        layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
    )
}

private fun nodeAt(offset: Offset, width: Float, height: Float, positions: Map<String, Pair<Float, Float>>): String? {
    val sx = width / 600f
    val sy = height / 400f
    val radius = 52f
    return positions.entries.firstOrNull { (_, pos) ->
        val x = pos.first * sx
        val y = pos.second * sy
        kotlin.math.hypot(offset.x - x, offset.y - y) <= radius
    }?.key
}

/** Hand-laid coordinates (FUNCTIONALITY.md §3.1) — readability beats auto-layout. */
private val NODE_POSITIONS: Map<String, Pair<Float, Float>> = mapOf(
    "fmcg" to (135f to 100f),
    "retail" to (300f to 62f),
    "pharma" to (470f to 100f),
    "power" to (110f to 300f),
    "banking" to (300f to 345f),
    "it" to (490f to 300f),
)

private val SHORT_NAMES: Map<String, String> = mapOf(
    "fmcg" to "FMCG",
    "it" to "IT",
    "power" to "POWER",
    "retail" to "RETAIL",
    "banking" to "BANKS",
    "pharma" to "PHARMA",
)

/** Per-edge perpendicular bend so paths don't overlap (§3.1 layout table). */
private val PERPENDICULAR_BEND: Map<Pair<String, String>, Boolean> = mapOf(
    ("banking" to "power") to true,
    ("banking" to "retail") to true,
    ("it" to "banking") to true,
    ("retail" to "fmcg") to false,
    ("banking" to "fmcg") to true,
    ("retail" to "pharma") to true,
    ("it" to "retail") to false,
    ("power" to "fmcg") to false,
)
