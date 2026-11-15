package com.dotpmm.stocker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion

/**
 * Entrance choreography for the first frame (design.md §8.4): [delayMs] staggers the header, then
 * the hero label, then the numeral. One-time, backwards fill, zero layout shift.
 */
@Composable
fun Modifier.entrance(delayMs: Int = 0, rise: Float = 12f): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            kotlinx.coroutines.delay(delayMs.toLong())
            progress.animateTo(1f, tween(Motion.Slow, easing = Motion.Emerge))
        }
    }
    this
        .graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * rise * density
        }
}

/** A one-shot "settle" that gives a panel its spring-in when it first composes. */
@Composable
fun Modifier.settleIn(scaleFrom: Float = 0.98f): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduced) progress.snapTo(1f) else progress.animateTo(1f, tween(Motion.Base, easing = Motion.Fluid))
    }
    this.graphicsLayer {
        val eased = progress.value
        scaleX = scaleFrom + (1f - scaleFrom) * eased
        scaleY = scaleFrom + (1f - scaleFrom) * eased
        alpha = eased
    }
}
