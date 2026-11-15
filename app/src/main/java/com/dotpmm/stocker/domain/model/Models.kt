package com.dotpmm.stocker.domain.model

enum class Tone { Positive, Neutral, Negative }

enum class Market { India, US }

data class Stock(
    val ticker: String,
    val name: String,
    val market: Market,
    /** Null for US names, which are news-only. */
    val price: Double?,
    val changePct: Double?,
    val articleCount: Int,
    val tone: Tone,
    val trend: List<Float>,
)

data class NewsItem(
    val source: String,
    val age: String,
    val headline: String,
    val tag: String,
    val tone: Tone,
    val extraSources: Int = 0,
)

data class UpcomingEvent(
    val day: String,
    val month: String,
    val label: String,
)

data class ToneCounts(val positive: Int, val neutral: Int, val negative: Int) {
    val total: Int get() = positive + neutral + negative
}

data class RangeSeries(
    val label: String,
    val caption: String,
    val changeAbs: Double,
    val changePct: Double,
    val points: List<Float>,
)
