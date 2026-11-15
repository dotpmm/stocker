package com.dotpmm.stocker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/** Material 3 "emphasized" motion without Material shapes or colours. */
object Motion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val PressMillis = 220
    const val SlideMillis = 380
    const val PillMillis = 450
    const val ColorMillis = 300
}
