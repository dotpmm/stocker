package com.dotpmm.stocker.domain.nlp

import java.time.LocalDate
import java.time.ZoneId

/** Rule-based event tag for a story (Results, Rating, M&A, ...). */
object EventTagger {
    private val rules: List<Pair<String, Regex>> = listOf(
        "Board meeting" to Regex("board meeting|board to consider|board to meet", RegexOption.IGNORE_CASE),
        "Dividend" to Regex("dividend|\\bbonus (issue|shares)|stock split|buyback|buy-back|record date|ex-date", RegexOption.IGNORE_CASE),
        "Rating" to Regex("upgrade|downgrade|target price|price target|initiates coverage|reiterates|brokerage|overweight|underweight", RegexOption.IGNORE_CASE),
        "M&A" to Regex("acquire|acquisition|merger|demerger|takeover|buyout|joint venture|divest|open offer|\\bstake in\\b", RegexOption.IGNORE_CASE),
        "Order win" to Regex("\\border(s)? (worth|of|win|book)|bags|wins (an? )?(order|contract)|secures|letter of award|awarded|contract", RegexOption.IGNORE_CASE),
        "Regulatory" to Regex("\\bsebi\\b|\\brbi\\b|\\bcci\\b|\\bnclt\\b|tribunal|penalty|\\bprobe\\b|\\bcbi\\b|show.cause|investigation|antitrust|\\bfda\\b|\\bsec\\b|court", RegexOption.IGNORE_CASE),
        "Fundraise" to Regex("\\bipo\\b|\\bqip\\b|rights issue|fund.?rais|\\bncd\\b|\\bfpo\\b|\\bofs\\b|preferential (issue|allotment)", RegexOption.IGNORE_CASE),
        "Block deal" to Regex("block deal|bulk deal|stake sale|promoter (sells|selling)", RegexOption.IGNORE_CASE),
        "Management" to Regex("\\bceo\\b|\\bcfo\\b|chairman|resigns|resignation|appoints|appointed|steps down|managing director", RegexOption.IGNORE_CASE),
        "Results" to Regex("\\bq[1-4]\\b|quarterly results?|financial results?|earnings|net profit|net loss|profit after tax|\\brevenue\\b", RegexOption.IGNORE_CASE),
        "Guidance" to Regex("guidance|outlook|forecast|expects|projects", RegexOption.IGNORE_CASE),
    )

    fun tag(title: String, summary: String): String {
        for ((label, regex) in rules) if (regex.containsMatchIn(title)) return label
        for ((label, regex) in rules) if (regex.containsMatchIn(summary)) return label
        return "News"
    }
}

/** Finds scheduled corporate events (results, board meetings, record dates) mentioned in a story. */
object EventExtractor {
    private const val MONTHS =
        "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|" +
            "sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?"

    private val dayMonth = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+($MONTHS)\\b(?:,?\\s+(\\d{4}))?", RegexOption.IGNORE_CASE)
    private val monthDay = Regex("\\b($MONTHS)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b(?:,?\\s+(\\d{4}))?", RegexOption.IGNORE_CASE)

    private val kinds: List<Pair<String, Regex>> = listOf(
        "Board meeting" to Regex("board meeting|board to meet|board to consider", RegexOption.IGNORE_CASE),
        "Record date" to Regex("record date", RegexOption.IGNORE_CASE),
        "Ex-dividend" to Regex("ex-?dividend|ex-?date", RegexOption.IGNORE_CASE),
        "AGM" to Regex("\\bagm\\b|annual general meeting", RegexOption.IGNORE_CASE),
        "Results" to Regex("(q[1-4]|quarterly|financial|annual|half.yearly) results?|earnings (on|date|call|report)|results (on|date)", RegexOption.IGNORE_CASE),
    )

    private val past = Regex("\\b(reported|posted|declared|announced|clocked|logged|delivered|recorded)\\b", RegexOption.IGNORE_CASE)

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun monthNumber(token: String): Int = when (token.take(3).lowercase()) {
        "jan" -> 1
        "feb" -> 2
        "mar" -> 3
        "apr" -> 4
        "may" -> 5
        "jun" -> 6
        "jul" -> 7
        "aug" -> 8
        "sep" -> 9
        "oct" -> 10
        "nov" -> 11
        else -> 12
    }

    /** Returns (label, epochDay) for the first scheduled event found, or null. */
    fun extract(title: String, summary: String, publishedAt: Long): Pair<String, Long>? {
        val text = "$title. $summary"
        val label = kinds.firstOrNull { it.second.containsMatchIn(text) }?.first ?: return null
        if (label == "Results" && past.containsMatchIn(title)) return null
        val pubDay = java.time.Instant.ofEpochMilli(publishedAt).atZone(zone).toLocalDate()

        fun build(day: Int, month: Int, yearText: String): LocalDate? = try {
            val explicitYear = yearText.toIntOrNull()
            var date = LocalDate.of(explicitYear ?: pubDay.year, month, day)
            if (explicitYear == null && date.isBefore(pubDay.minusDays(1))) date = date.plusYears(1)
            date
        } catch (e: Exception) {
            null
        }

        val candidates = ArrayList<LocalDate>()
        dayMonth.findAll(text).forEach { m ->
            build(m.groupValues[1].toInt(), monthNumber(m.groupValues[2]), m.groupValues[3])?.let { candidates.add(it) }
        }
        monthDay.findAll(text).forEach { m ->
            build(m.groupValues[2].toInt(), monthNumber(m.groupValues[1]), m.groupValues[3])?.let { candidates.add(it) }
        }
        val date = candidates.firstOrNull { !it.isBefore(pubDay) && !it.isAfter(pubDay.plusDays(120)) } ?: return null
        return label to date.toEpochDay()
    }
}
