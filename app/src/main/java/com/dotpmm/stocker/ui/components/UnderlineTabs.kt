package com.dotpmm.stocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText
import kotlin.math.roundToInt

/**
 * Plain text choices with a short underline that slides to the selected label.
 * Replaces pill/segmented buttons. Use [fillWidth] for evenly spaced centred items
 * (e.g. chart ranges) and the default for left-aligned headline tabs (e.g. India / US news).
 */
@Composable
fun UnderlineTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = StockerText.TabSmall,
    fillWidth: Boolean = false,
    itemSpacing: Dp = 28.dp,
    indicatorWidth: Dp = 16.dp,
) {
    val xs = remember { mutableStateMapOf<Int, Float>() }
    val ws = remember { mutableStateMapOf<Int, Float>() }
    val ready = xs.size == labels.size && ws.size == labels.size
    val indicatorPx = with(LocalDensity.current) { indicatorWidth.toPx() }

    val target = if (ready) {
        val x = xs[selectedIndex] ?: 0f
        val w = ws[selectedIndex] ?: 0f
        if (fillWidth) x + (w - indicatorPx) / 2f else x
    } else {
        0f
    }

    val animatedX = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target, ready) {
        if (!ready) return@LaunchedEffect
        if (!placed) {
            animatedX.snapTo(target)
            placed = true
        } else {
            animatedX.animateTo(target, tween(Motion.SlideMillis, easing = Motion.Emphasized))
        }
    }

    Box(modifier) {
        Row(horizontalArrangement = if (fillWidth) Arrangement.Start else Arrangement.spacedBy(0.dp)) {
            labels.forEachIndexed { index, label ->
                val color by animateColorAsState(
                    targetValue = if (index == selectedIndex) Obsidian.Text else Obsidian.TextSubtle,
                    animationSpec = tween(Motion.ColorMillis, easing = Motion.Emphasized),
                    label = "tabColor",
                )
                Box(
                    modifier = Modifier
                        .then(if (fillWidth) Modifier.weight(1f) else Modifier)
                        .height(44.dp)
                        .pressable(onClick = { onSelect(index) }, role = Role.Tab)
                        .onGloballyPositioned { coords ->
                            val x = coords.positionInParent().x
                            val w = coords.size.width.toFloat()
                            if (xs[index] != x) xs[index] = x
                            if (ws[index] != w) ws[index] = w
                        },
                    contentAlignment = if (fillWidth) Alignment.Center else Alignment.CenterStart,
                ) {
                    Text(text = label, style = textStyle.copy(color = color))
                }
                if (!fillWidth && index < labels.lastIndex) Spacer(Modifier.width(itemSpacing))
            }
        }
        if (ready) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 4.dp)
                    .offset { IntOffset(animatedX.value.roundToInt(), 0) }
                    .size(indicatorWidth, 2.dp)
                    .background(Obsidian.Text),
            )
        }
    }
}
