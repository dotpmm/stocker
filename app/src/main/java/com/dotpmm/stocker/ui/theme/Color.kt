package com.dotpmm.stocker.ui.theme

import androidx.compose.ui.graphics.Color

/** Obsidian palette. Pure black canvas; no borders, no cards. */
object Obsidian {
    val Background = Color(0xFF000000)

    /** Floating nav bar surface (solid, never translucent). */
    val Surface = Color(0xFF131316)

    /** Sliding highlight inside the nav bar. */
    val Highlight = Color(0xFF26262C)

    val Text = Color(0xFFF4F4F5)
    val TextSecondary = Color(0xFFC9C9D1)
    val TextMuted = Color(0xFF9B9BA5)
    val TextSubtle = Color(0xFF8A8A94)

    val Positive = Color(0xFF3DDC97)
    val Negative = Color(0xFFFF6B6B)
    val Neutral = Color(0xFF55555F)

    /** Used for dates, AI and pressed states. */
    val Accent = Color(0xFF9DB8FF)
}
