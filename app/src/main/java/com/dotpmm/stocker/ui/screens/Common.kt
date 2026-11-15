package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.dotpmm.stocker.StockerApp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.sweepShimmer
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal

/** A round icon-only tap target on metal. Presses sink and spring back. */
@Composable
fun IconTap(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    tint: Color = Ink.TextPrimary,
) {
    Box(
        modifier
            .size(size)
            .flat(Ink.Surface1, RoundedCornerShape(999.dp), 999.dp)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

/** A skeleton block with the travelling light sweep — loading must look like thinking (design.md §8.2). */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    corner: androidx.compose.ui.unit.Dp = 8.dp,
) {
    Box(modifier.fillMaxWidth().height(height).sweepShimmer(RoundedCornerShape(corner)))
}

/** Two-to-three skeleton rows used while a table or panel loads. */
@Composable
fun SkeletonRows(count: Int = 3, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) { index ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.widthFraction(0.30f).height(13.dp).sweepShimmer(RoundedCornerShape(6.dp)))
                Box(Modifier.widthFraction(0.22f).height(13.dp).sweepShimmer(RoundedCornerShape(6.dp)))
                Box(Modifier.widthFraction(0.18f).height(13.dp).sweepShimmer(RoundedCornerShape(6.dp)))
            }
        }
    }
}

private fun Modifier.widthFraction(fraction: Float): Modifier = this.fillMaxWidth(fraction)

/** An inline error card. Never a toast — errors live in the layout (FUNCTIONALITY.md §7). */
@Composable
fun ErrorCard(title: String, body: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier
            .fillMaxWidth()
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = Type.BodyStrong.copy(color = Ink.TextPrimary))
        Text(body, style = Type.Caption)
        if (onRetry != null) {
            Text(
                "Retry",
                style = Type.BodyStrong.copy(color = Ink.TextPrimary),
                modifier = Modifier.pressable(onClick = onRetry).padding(vertical = 4.dp),
            )
        }
    }
}

/** The one primary action of a screen: a metal plate with a white rim glow on press. */
@Composable
fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
) {
    Row(
        modifier
            .metal(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp, lifted = true)
            .pressable(onClick = onClick, pressedScale = 0.97f)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = Ink.TextPrimary, modifier = Modifier.size(16.dp))
        }
        Text(label, style = Type.ButtonLabel)
        if (trailing != null) {
            Icon(trailing, contentDescription = null, tint = Ink.TextMuted, modifier = Modifier.size(15.dp))
        }
    }
}

/** Remembers a value that only flips to true once — the "reveal on scroll, never re-trigger" flag. */
@Composable
fun rememberOnce(): State<Boolean> {
    val state = remember { mutableStateOf(false) }
    return state
}

/** Muted uppercase micro-label used to structure every panel. */
@Composable
fun SectionMicro(text: String, modifier: Modifier = Modifier, color: Color = Ink.TextMuted) {
    Text(text.uppercase(), style = Type.Micro.copy(color = color), modifier = modifier)
}

/** A thin hairline separator built from translucent white, never a hard border. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Ink.BorderSubtle))
}

/**
 * Runs [block] now and again whenever stored data changes (after a refresh) or any of [keys] change.
 * Keeps the previous value while reloading, so lists never flash empty.
 *
 * [stableDuringScroll] data is a version counter, so a refresh mid-scroll would otherwise re-query
 * and recompose the whole list. The value is held until the list settles.
 */
@Composable
fun <T> rememberQuery(initial: T, vararg keys: Any?, block: suspend () -> T): State<T> {
    val state = remember { mutableStateOf(initial) }
    val version by StockerApp.instance.store.dataVersion.collectAsState()
    // Key on the values, not a freshly allocated list, so an unchanged query never re-launches.
    val queryKey = keys.contentHashCode() * 31 + version
    LaunchedEffect(queryKey) { state.value = block() }
    return state
}
