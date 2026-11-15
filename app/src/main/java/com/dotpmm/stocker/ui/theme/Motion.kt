package com.dotpmm.stocker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Neural Expressive motion (design.md §8). Durations are the shipped values from FUNCTIONALITY.md §6.
 * Nothing animates width/top/left — transform and opacity only.
 */
object Motion {
    /** Gemini's fluid easing: fast approach, soft landing. */
    val Fluid = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Anticipation → settle, for entrances. */
    val Emerge = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Departures accelerate away. */
    val Exit = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    const val Instant = 120
    const val Fast = 240
    const val Base = 420
    const val Slow = 700
    const val Cinematic = 1100

    val springSoft = spring<Float>(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
    val springFluid = spring<Float>(dampingRatio = 0.72f, stiffness = 120f)
    val springPop = spring<Float>(dampingRatio = 0.55f, stiffness = 420f)

    fun <T> fluid(duration: Int = Base): FiniteAnimationSpec<T> = tween(duration, easing = Fluid)
    fun <T> emerge(duration: Int = Base): FiniteAnimationSpec<T> = tween(duration, easing = Emerge)
    fun <T> exit(duration: Int = Fast): FiniteAnimationSpec<T> = tween(duration, easing = Exit)
}

