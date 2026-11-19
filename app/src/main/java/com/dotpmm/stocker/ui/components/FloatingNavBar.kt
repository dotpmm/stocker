package com.dotpmm.stocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type

data class NavItem(val label: String, val icon: ImageVector)

private val barShape = RoundedCornerShape(18.dp)
private val indicatorShape = RoundedCornerShape(11.dp)

/**
 * The floating bottom navigation (design.md §10.2), now flat by design: a solid charcoal plate with
 * one hairline, a sliding surface-toned indicator and no metal gradients, glow or pill rounding.
 * The selected tab is the only raised state on the bar.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clip(barShape)
            .background(Ink.Surface2)
            .border(1.dp, Color.White.copy(alpha = 0.06f), barShape),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val reduced = LocalReducedMotion.current
            val slot = maxWidth / items.size
            val indicatorX by animateDpAsState(
                targetValue = slot * selectedIndex,
                animationSpec = if (reduced) {
                    tween(120)
                } else {
                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                },
                label = "navIndicator",
            )
            Box(
                Modifier
                    .padding(5.dp)
                    .offset(x = indicatorX)
                    .size(slot - 10.dp, 50.dp)
                    .clip(indicatorShape)
                    .background(Ink.Surface3),
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    NavTab(
                        item = item,
                        selected = index == selectedIndex,
                        slotWidth = slot,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    slotWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val springSpec = if (reduced) {
        tween<Float>(120)
    } else {
        spring(dampingRatio = 0.6f, stiffness = 420f)
    }
    val iconScale by animateFloatAsState(if (selected) 1.08f else 1f, springSpec, label = "navScale")
    val color by animateColorAsState(
        targetValue = if (selected) Ink.TextPrimary else Ink.TextMuted,
        animationSpec = tween(Motion.Fast, easing = Motion.Fluid),
        label = "navColor",
    )
    Column(
        modifier = Modifier
            .size(width = slotWidth, height = 60.dp)
            .pressable(onClick = onClick, pressedScale = 0.95f, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Box(Modifier.height(4.dp))
        Text(item.label, style = Type.NavLabel.copy(color = color))
    }
}
