package com.dotpmm.stocker.ui

import androidx.compose.ui.graphics.Color
import com.dotpmm.stocker.ui.theme.Obsidian
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    fun age(ms: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = ((now - ms) / 60_000L).coerceAtLeast(0L)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 48 * 60 -> "${minutes / 60}h"
            else -> "${minutes / (24 * 60)}d"
        }
    }

    fun updated(ms: Long): String {
        if (ms <= 0L) return "Not refreshed yet"
        val minutes = (System.currentTimeMillis() - ms) / 60_000L
        return if (minutes < 1) "Updated just now" else "Updated ${age(ms)} ago"
    }

    fun dayNumber(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()

    fun monthShort(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase()

    fun daysUntil(epochDay: Long): String {
        val d = epochDay - LocalDate.now(zone).toEpochDay()
        return when {
            d <= 0L -> "Today"
            d == 1L -> "Tomorrow"
            else -> "In $d days"
        }
    }

    fun dateTime(ms: Long): String {
        val t = Instant.ofEpochMilli(ms).atZone(zone)
        val month = t.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
        return String.format(Locale.ENGLISH, "%d %s, %02d:%02d", t.dayOfMonth, month, t.hour, t.minute)
    }
}
