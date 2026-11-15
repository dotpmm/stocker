package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.research.SearchResult
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Metal
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * The ask/search surface (design.md §8.2, §12 rule 13).
 *
 * At rest it is completely quiet — no loop, no residual glow. On focus, press or typing a burst
 * plays: a halftone dot field with quadratic falloff from the pill, per-dot shimmer and expanding
 * ripples, plus up to 3 four-point sparkles. It lives 1050 ms and then vanishes entirely.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ask for a company…",
    results: List<SearchResult> = emptyList(),
    onPick: (SearchResult) -> Unit = {},
    showResults: Boolean = false,
    onSubmit: () -> Unit = {},
) {
    val reduced = LocalReducedMotion.current
    val settings = com.dotpmm.stocker.ui.theme.LocalSettings.current
    val burstEnabled = settings.searchBurst
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var burstStart by remember { mutableStateOf(-1L) }
    val burstTime = remember { Animatable(0f) }

    fun fireBurst() {
        if (reduced || !burstEnabled) return
        burstStart = System.currentTimeMillis()
    }

    LaunchedEffect(focused, query) {
        if (focused || query.isNotEmpty()) fireBurst()
    }
    LaunchedEffect(burstStart) {
        if (burstStart < 0) return@LaunchedEffect
        burstTime.snapTo(0f)
        burstTime.animateTo(1f, tween(1050, easing = Motion.Emerge))
        burstStart = -1L
    }

    val envelope = if (burstStart >= 0) sin(Math.PI * burstTime.value).toFloat() else 0f

    BoxWithConstraints(modifier) {
        val pillHeight = 46.dp
        // The burst canvas sits behind the pill and paints only while the envelope is live.
        if (envelope > 0.01f) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(pillHeight + 72.dp)
                    .offset(y = (-36).dp),
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val band = 34.dp.toPx()
                val spacing = 9.dp.toPx()
                val halfW = size.width / 2f
                val halfH = pillHeight.toPx() / 2f
                val phase = burstTime.value * 1050f

                var y = 0f
                while (y < size.height) {
                    var x = 0f
                    while (x < size.width) {
                        // Signed distance from the pill's edge, so density falls off outward.
                        val dx = abs(x - cx) - halfW
                        val dy = abs(y - cy) - halfH
                        val outside = Math.hypot(
                            dx.coerceAtLeast(0f).toDouble(),
                            dy.coerceAtLeast(0f).toDouble(),
                        ).toFloat()
                        val d = maxOf(outside, maxOf(dx, dy)) + 1f
                        if (d in 0f..band) {
                            val base = Math.pow((1f - d / band).toDouble(), 1.7).toFloat()
                            val shim = 0.5f + 0.5f * sin(phase * 0.0014f + x * 0.11f + y * 0.087f)
                            val ripple = 0.5f + 0.5f * sin(d * 0.38f - phase * 0.0022f)
                            val twinkle = if (hash01(x, y) < 0.02f) 1f else 0f
                            val alpha = (base * (0.28f + 0.34f * shim + 0.38f * ripple) + twinkle * base * 0.9f)
                                .coerceAtMost(0.6f) * envelope
                            if (alpha > 0.012f) {
                                drawCircle(
                                    Color.White.copy(alpha = alpha),
                                    radius = 0.9.dp.toPx(),
                                    center = Offset(x, y),
                                )
                            }
                        }
                        x += spacing
                    }
                    y += spacing
                }

                // Sparkles: 4-point stars on the pill's perimeter, max 3 concurrent.
                if (envelope > 0.25f) {
                    val rng = Random(burstStart.toInt())
                    repeat(3) {
                        val t = 0.15f + rng.nextFloat() * 0.7f
                        val px = size.width * t
                        val py = cy + if (rng.nextBoolean()) -halfH else halfH
                        val arm = 8.dp.toPx() * envelope
                        if (arm > 0.8f) {
                            sparkle(Offset(px, py), arm, Color.White.copy(alpha = 0.5f * envelope))
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .metal(Ink.Surface1, RoundedCornerShape(999.dp), 999.dp, lifted = focused)
                .then(
                    if (envelope > 0.01f) {
                        Modifier.background(Color.White.copy(alpha = 0.03f * envelope))
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", style = Type.Data.copy(color = if (focused) Ink.TextPrimary else Ink.TextMuted, fontSize = 15.sp))
            Box(Modifier.padding(start = 10.dp).weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(placeholder, style = Type.BodyTight.copy(color = Ink.TextMuted))
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = Type.BodyTight.copy(color = Ink.TextPrimary),
                    cursorBrush = SolidColor(Ink.TextPrimary),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(); onSubmit() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) fireBurst() },
                )
            }
            if (query.isNotEmpty()) {
                Text(
                    "✕",
                    style = Type.Caption.copy(color = Ink.TextMuted),
                    modifier = Modifier.pressable(onClick = onSearch, pressedScale = 0.9f),
                )
            }
        }

        if (showResults && results.isNotEmpty()) {
            Box(
                Modifier
                    .padding(top = 54.dp)
                    .fillMaxWidth()
                    .flat(Ink.Surface2, RoundedCornerShape(18.dp), 18.dp),
            ) {
                androidx.compose.foundation.layout.Column {
                    results.forEach { hit ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { onPick(hit) }, pressedScale = 0.99f)
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(hit.symbol, style = Type.Ticker)
                            Text(
                                "  ${hit.name}",
                                style = Type.Caption.copy(color = Ink.TextSecondary),
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Text(hit.sector, style = Type.MicroData.copy(fontSize = 13.sp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.sparkle(center: Offset, arm: Float, color: Color) {
    val thin = arm * 0.22f
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - arm)
        quadraticTo(center.x + thin, center.y - thin, center.x + arm, center.y)
        quadraticTo(center.x + thin, center.y + thin, center.x, center.y + arm)
        quadraticTo(center.x - thin, center.y + thin, center.x - arm, center.y)
        quadraticTo(center.x - thin, center.y - thin, center.x, center.y - arm)
        close()
    }
    drawPath(path, color)
    drawCircle(color.copy(alpha = 0.8f), radius = 1.4.dp.toPx(), center = center)
}

private fun hash01(x: Float, y: Float): Float {
    val h = sin(x * 12.9898f + y * 78.233f) * 43758.5453f
    return h - kotlin.math.floor(h)
}
