package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.dotpmm.stocker.ui.theme.Motion

/**
 * Tap target with no ripple, no background and no shape: on press it shrinks and dims
 * with Material-3 emphasized easing.
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    role: Role = Role.Button,
    pressedScale: Float = 0.94f,
    pressedAlpha: Float = 0.6f,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val spec = tween<Float>(Motion.PressMillis, easing = Motion.Emphasized)
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spec, label = "pressScale")
    val alpha by animateFloatAsState(if (pressed) pressedAlpha else 1f, spec, label = "pressAlpha")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(interactionSource = source, indication = null, role = role, onClick = onClick)
}
