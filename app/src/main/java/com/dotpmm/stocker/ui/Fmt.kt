package com.dotpmm.stocker.ui

import androidx.compose.ui.graphics.Color
import com.dotpmm.stocker.ui.theme.Obsidian
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Number formatting. Indian digit grouping, always paired with an arrow or sign. */
object Fmt {
    private val indian: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val number: NumberFormat = NumberFormat.getNumberInstance(indian).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun price(value: Double): String = "₹" + number.format(value)

    fun plain(value: Double): String = number.format(value)

    fun percent(value: Double): String = String.format(Locale.US, "%.2f%%", abs(value))

    fun arrowPercent(value: Double): String = (if (value >= 0) "▲ " else "▼ ") + percent(value)

    fun signed(value: Double): String = (if (value >= 0) "+" else "−") + number.format(abs(value))

    fun changeColor(value: Double): Color = if (value >= 0) Obsidian.Positive else Obsidian.Negative
}
