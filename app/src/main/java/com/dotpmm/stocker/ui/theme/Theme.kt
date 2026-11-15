package com.dotpmm.stocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun StockerTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Obsidian.Accent,
        onPrimary = Obsidian.Background,
        background = Obsidian.Background,
        onBackground = Obsidian.Text,
        surface = Obsidian.Background,
        onSurface = Obsidian.Text,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
