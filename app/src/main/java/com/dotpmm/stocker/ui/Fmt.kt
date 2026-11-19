package com.dotpmm.stocker.ui

import java.util.Locale
import kotlin.math.abs

/** Number and date formatting. Every price goes through here so digits stay tabular and Indian-grouped. */
object Fmt {
    /**
     * Indian digit grouping, applied by hand.
     *
     * `NumberFormat` with an `en-IN` locale delegates to ICU, whose grouping data differs between
     * the JVM and Android — the same value can render `12,40,756` on a phone and `1,240,756` in a
     * test. Lakh grouping is a product rule, so it is implemented deterministically instead:
     * the last three digits, then pairs.
     */
    fun groupIndian(integerPart: String): String {
        val negative = integerPart.startsWith("-")
        val digits = if (negative) integerPart.drop(1) else integerPart
        if (digits.length <= 3) return integerPart
        val lastThree = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val pairs = ArrayList<String>()
        while (rest.length > 2) {
            pairs.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) pairs.add(0, rest)
        val grouped = pairs.joinToString(",") + "," + lastThree
        return if (negative) "-$grouped" else grouped
    }

    private fun grouped(value: Double, digits: Int): String {
        // Format WITHOUT locale grouping — the commas must come only from groupIndian, or the
        // separators String.format inserts end up double-grouped ("₹12,3,456").
        val formatted = String.format(Locale.US, "%.${digits}f", value)
        val integerPart = formatted.substringBefore('.')
        val fraction = formatted.substringAfter('.', "")
        val sign = integerPart.startsWith("-")
        val groupedInteger = groupIndian(integerPart.removePrefix("-"))
        return buildString {
            if (sign) append('-')
            append(groupedInteger)
            if (digits > 0 && fraction.isNotEmpty()) append('.').append(fraction)
        }
    }

    /** ₹1,339.70 — the standard price rendering. */
    fun price(value: Double): String = "₹" + grouped(value, digitsFor(value))

    /** Plain grouped number with an explicit precision. */
    fun number(value: Double, digits: Int = 2): String = grouped(value, digits)

    private fun digitsFor(value: Double): Int = if (abs(value) >= 1000) 0 else 2

    /** Signed percent without a glyph (the glyph is rendered as a shape beside it). */
    fun pct(value: Double?, digits: Int = 1): String =
        if (value == null) "—" else String.format(Locale.US, "%.${digits}f%%", abs(value))

    /** Trailing P/E, or an em dash — never 0 for a missing number. */
    fun ratio(value: Double?): String = if (value == null || value <= 0) "—" else String.format(Locale.US, "%.1f", value)

    /** Market cap in ₹ crore, rounded to 0 dp (FUNCTIONALITY.md §3.2). */
    fun crore(mcapCr: Double?): String {
        if (mcapCr == null || mcapCr <= 0) return "—"
        return groupIndian(String.format(Locale.US, "%.0f", mcapCr))
    }

    /** A compact crore label for card faces: ₹4.3L Cr. */
    fun croreCompact(mcapCr: Double?): String {
        if (mcapCr == null || mcapCr <= 0) return "—"
        return when {
            mcapCr >= 100_000 -> "₹" + String.format(Locale.US, "%.1fL Cr", mcapCr / 100_000)
            mcapCr >= 1_000 -> "₹" + String.format(Locale.US, "%.1fK Cr", mcapCr / 1_000)
            else -> "₹" + groupIndian(String.format(Locale.US, "%.0f", mcapCr)) + " Cr"
        }
    }

    /** Share volume in market shorthand: 12.4M, 1.2B, 845K. */
    fun volume(shares: Long?): String {
        if (shares == null || shares <= 0L) return "—"
        val value = shares.toDouble()
        return when {
            value >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
            value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000)
            else -> groupIndian(shares.toString())
        }
    }

    /** Hero index value: grouped, no currency symbol, 1 decimal. */
    fun index(value: Double?): String =
        if (value == null) "—" else String.format(Locale.US, "%s", grouped(value, 1))

    /** "13 Nov" from "2025-11-13" or "2025-11-13 15:15". */
    fun chartDate(stamp: String): String {
        val datePart = stamp.substringBefore(' ')
        val parts = datePart.split('-')
        if (parts.size != 3) return datePart
        val month = runCatching { java.time.LocalDate.parse(datePart) }
            .map { it.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH) }
            .getOrDefault(parts[1])
        return "${parts[2].trimStart('0')} $month"
    }

    /** "09:15" for intraday stamps. */
    fun chartTime(stamp: String): String = stamp.substringAfter(' ', "")

    fun dayMonth(stamp: String): String = chartDate(stamp)

    private val zone: java.time.ZoneId = java.time.ZoneId.of("Asia/Kolkata")

    /** "now", "14m", "3h", "2d" — the age line on a story. */
    fun age(ms: Long, now: Long = System.currentTimeMillis()): String {
        val minutes = ((now - ms) / 60_000L).coerceAtLeast(0L)
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 48 * 60 -> "${minutes / 60}h"
            else -> "${minutes / (24 * 60)}d"
        }
    }

    /** "Updated just now" / "Updated 2h ago". */
    fun updated(ms: Long): String {
        if (ms <= 0L) return "Not refreshed yet"
        val minutes = (System.currentTimeMillis() - ms) / 60_000L
        return if (minutes < 1) "Updated just now" else "Updated ${age(ms)} ago"
    }

    fun dayNumber(epochDay: Long): String =
        java.time.LocalDate.ofEpochDay(epochDay).dayOfMonth.toString()

    fun monthShort(epochDay: Long): String =
        java.time.LocalDate.ofEpochDay(epochDay)
            .month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
            .uppercase()

    fun daysUntil(epochDay: Long): String {
        val days = epochDay - java.time.LocalDate.now(zone).toEpochDay()
        return when {
            days <= 0L -> "TODAY"
            days == 1L -> "TOMORROW"
            else -> "IN $days DAYS"
        }
    }

    /** "13 Nov, 09:15" in IST. */
    fun dateTime(ms: Long): String {
        val t = java.time.Instant.ofEpochMilli(ms).atZone(zone)
        val month = t.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
        return String.format(Locale.ENGLISH, "%d %s, %02d:%02d", t.dayOfMonth, month, t.hour, t.minute)
    }
}
