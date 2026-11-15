package com.dotpmm.stocker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

/**
 * The one primary action on a screen: a solid, almost-square white bar with black text.
 * Pressing turns it accent blue, nudges the arrow forward and scales it slightly.
 */
@Composable
fun PrimaryActionBar(
    label: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colorSpec = tween<Color>(Motion.ColorMillis, easing = Motion.Emphasized)
    val floatSpec = tween<Float>(Motion.PressMillis, easing = Motion.Emphasized)
    val background by animateColorAsState(
        if (pressed) Obsidian.Accent else Obsidian.Text, colorSpec, label = "actionBg",
    )
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, floatSpec, label = "actionScale")
    val nudge by animateFloatAsState(if (pressed) 4f else 0f, tween(Motion.ColorMillis, easing = Motion.Emphasized), label = "actionNudge")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(2.dp))
            .background(background)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(leadingIcon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Text(text = label, style = StockerText.Action)
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { translationX = nudge * density },
        )
    }
}
