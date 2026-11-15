package com.dotpmm.stocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.ambientGlow
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal

data class NavItem(val label: String, val icon: ImageVector)

/**
 * The floating bottom navigation (design.md §10.2). A metal-lit pill lifted off the edge: the
 * selected tab gets a sliding raised plate, a white inner glow and a spring-lifted icon. Nothing
 * else on the bar glows — glow is earned by state.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalReducedMotion.current
    // The bar itself follows the metal setting; the active plate is always raised so the
    // selection stays legible even when the bar is flat.
    val settings = LocalSettings.current
    val bar = if (settings.metalNavBar) {
        Modifier.metal(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp, lifted = true)
    } else {
        Modifier.flat(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp)
    }
    Box(
        modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth()
            .height(62.dp)
            // The ambient glow is clipped to the pill, so it traces the rounded ends on all sides.
            .then(bar)
            .ambientGlow(RoundedCornerShape(999.dp), 999.dp, intensity = 0.55f),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val slot = maxWidth / items.size
            val indicatorX by animateDpAsState(
                targetValue = slot * selectedIndex,
                animationSpec = if (reduced) tween(120) else spring(
                    dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow,
                ),
                label = "navPlate",
            )
            // The sliding plate: raised surface + white rim glow on the active slot only.
            Box(
                Modifier
                    .padding(6.dp)
                    .offset(x = indicatorX)
                    .size(slot - 12.dp, 50.dp)
                    .metal(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp, lifted = true)
                    .ambientGlow(RoundedCornerShape(999.dp), 999.dp, intensity = 0.7f),
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
        spring(dampingRatio = 0.55f, stiffness = 420f)
    }
    val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, springSpec, label = "navScale")
    val iconLift by animateFloatAsState(if (selected) -1.5f else 0f, springSpec, label = "navLift")
    val color by animateColorAsState(
        targetValue = if (selected) Ink.TextPrimary else Ink.TextMuted,
        animationSpec = tween(Motion.Fast, easing = Motion.Fluid),
        label = "navColor",
    )
    Column(
        modifier = Modifier
            .size(width = slotWidth, height = 62.dp)
            .pressable(onClick = onClick, pressedScale = 0.94f, role = Role.Tab),
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
                    translationY = iconLift * density
                },
        )
        Box(Modifier.height(4.dp))
        Text(item.label, style = Type.NavLabel.copy(color = color))
    }
}
