package com.dotpmm.stocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

data class NavItem(val label: String, val icon: ImageVector)

/**
 * Floating, rounded bottom bar lifted off the screen edge. Deliberately solid (no blur,
 * no translucency) so it costs almost nothing to draw. A highlight slides under the active
 * tab and the active icon springs up slightly.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Obsidian.Surface),
    ) {
        val tabWidth = (maxWidth - 12.dp) / items.size
        val pillX by animateDpAsState(
            targetValue = 6.dp + tabWidth * selectedIndex,
            animationSpec = tween(Motion.PillMillis, easing = Motion.Emphasized),
            label = "navPill",
        )
        Box(
            Modifier
                .offset(x = pillX, y = 6.dp)
                .size(tabWidth, 52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Obsidian.Highlight),
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            items.forEachIndexed { index, item ->
                NavTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavTab(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val springSpec = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
    val iconScale by animateFloatAsState(if (selected) 1.14f else 1f, springSpec, label = "navIconScale")
    val iconLift by animateFloatAsState(if (selected) -2f else 0f, springSpec, label = "navIconLift")
    val color by animateColorAsState(
        targetValue = if (selected) Obsidian.Text else Obsidian.TextSubtle,
        animationSpec = tween(Motion.ColorMillis, easing = Motion.Emphasized),
        label = "navColor",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .pressable(onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = iconLift * density
                },
        )
        Box(Modifier.height(3.dp))
        Text(text = item.label, style = StockerText.NavLabel.copy(color = color))
    }
}
