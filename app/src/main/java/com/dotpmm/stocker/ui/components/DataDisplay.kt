package com.dotpmm.stocker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.color
import com.dotpmm.stocker.ui.theme.glyph
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import com.dotpmm.stocker.ui.theme.textColor
import java.util.Locale
import kotlin.math.abs

/**
 * Rolling-digit ticker (design.md §8.2). Each digit is a clipped column that translates to its new
 * value; digits that did not change do not move. Reduced motion swaps digits instantly.
 */
@Composable
fun RollingNumber(
    text: String,
    style: TextStyle = Type.Hero,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    val reduced = LocalReducedMotion.current
    val cellHeight: Dp = with(androidx.compose.ui.platform.LocalDensity.current) { (style.fontSize * 1.22f).toDp() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            if (!char.isDigit()) {
                Text(char.toString(), style = style)
            } else {
                Box(Modifier.height(cellHeight), contentAlignment = Alignment.Center) {
                    if (reduced) {
                        Text(char.toString(), style = style)
                    } else {
                        AnimatedContent(
                            targetState = char,
                            transitionSpec = {
                                val forward = (targetState.digitToIntOrNull() ?: 0) >=
                                    (initialState.digitToIntOrNull() ?: 0)
                                val enter = slideInVertically(tween(520, easing = Motion.Fluid)) {
                                    if (forward) it else -it
                                } + fadeIn(tween(240))
                                val exit = slideOutVertically(tween(520, easing = Motion.Fluid)) {
                                    if (forward) -it else it
                                } + fadeOut(tween(240))
                                enter togetherWith exit using SizeTransform(clip = true)
                            },
                            label = "digit$index",
                        ) { value ->
                            Text(value.toString(), style = style)
                        }
                    }
                }
            }
        }
    }
}

/** Verdict pill for a sector card. Colour is derived from the badge, never chosen ad hoc. */
@Composable
fun VerdictPill(badge: Verdict, modifier: Modifier = Modifier) {
    val direction = when (badge) {
        Verdict.Overheated -> Direction.Down
        Verdict.Discounted -> Direction.Up
        Verdict.Consolidating -> Direction.Flat
    }
    val label = when (badge) {
        Verdict.Overheated -> "OVERHEATED"
        Verdict.Discounted -> "DISCOUNTED"
        Verdict.Consolidating -> "CONSOLIDATING"
    }
    val color = direction.textColor()
    Row(
        modifier
            .flat(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionGlyph(direction, size = 6.dp, color = color)
        Text(label, style = Type.MicroData.copy(color = color, fontSize = 13.sp))
    }
}

/** ▲ / ▼ drawn as a shape so colour is never the only signal (design.md §9.4). */
@Composable
fun DirectionGlyph(
    direction: Direction,
    size: Dp = 7.dp,
    color: Color = direction.textColor(),
) {
    if (direction == Direction.Flat) {
        Box(Modifier.size(size, 1.5.dp).background(color))
        return
    }
    Canvas(Modifier.size(size)) {
        val path = Path()
        if (direction == Direction.Up) {
            path.moveTo(this.size.width / 2f, 0f)
            path.lineTo(this.size.width, this.size.height)
            path.lineTo(0f, this.size.height)
        } else {
            path.moveTo(this.size.width / 2f, this.size.height)
            path.lineTo(this.size.width, 0f)
            path.lineTo(0f, 0f)
        }
        path.close()
        drawPath(path, color)
    }
}

/** A signed percentage with its glyph and direction colour applied together. */
@Composable
fun ChangeText(
    value: Double?,
    modifier: Modifier = Modifier,
    style: TextStyle = Type.Data,
    fractionDigits: Int = 1,
    suffix: String = "%",
    showGlyph: Boolean = true,
) {
    val direction = when {
        value == null -> Direction.Flat
        value > 0 -> Direction.Up
        value < 0 -> Direction.Down
        else -> Direction.Flat
    }
    val number = if (value == null) {
        "—"
    } else {
        val glyph = if (showGlyph) direction.glyph().takeIf { it.isNotEmpty() }?.plus(" ").orEmpty() else ""
        glyph + String.format(Locale.US, "%.${fractionDigits}f", abs(value)) + suffix
    }
    Text(text = number, style = style.copy(color = direction.textColor()), modifier = modifier)
}

/**
 * iOS-style segmented control (design.md §8.4). Labels are transparent inside one container and a
 * single pill slides between them — transform only, never per-chip background swaps.
 */
@Composable
fun SegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .flat(Ink.Surface1, RoundedCornerShape(999.dp), 999.dp)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / labels.size
        val pillX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(320, easing = Motion.Fluid),
            label = "segmentPill",
        )
        // Slid with `offset` rather than animated padding: motion must never touch layout
        // dimensions (design.md §8.3), and offset only re-positions the draw.
        Box(
            Modifier
                .offset(x = pillX)
                .width(segmentWidth)
                .fillMaxHeight()
                .metal(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val color by animateColorAsState(
                    targetValue = if (selected) Ink.TextPrimary else Ink.TextMuted,
                    animationSpec = tween(Motion.Fast, easing = Motion.Fluid),
                    label = "segColor$index",
                )
                Box(
                    Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .pressable(onClick = { onSelect(index) }, pressedScale = 0.94f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = Type.MicroData.copy(color = color, fontSize = 13.sp))
                }
            }
        }
    }
}

/** Muted uppercase micro-label with optional trailing caption — the app's structural texture. */
@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: Color = Ink.TextMuted) {
    Text(text.uppercase(), style = Type.Micro.copy(color = color), modifier = modifier)
}

/** A one-line key/value pair for fundamentals grids. Values are mono + tabular. */
@Composable
fun StatLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ink.TextPrimary,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Type.Micro.copy(fontSize = 12.5.sp))
        Text(value, style = Type.Data.copy(color = valueColor))
    }
}
