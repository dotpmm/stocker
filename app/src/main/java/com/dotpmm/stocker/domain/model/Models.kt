package com.dotpmm.stocker.domain.model

enum class Tone { Positive, Neutral, Negative }

enum class Market { India, US }

/** A stock the user follows. [symbol] is the Yahoo-style symbol (RELIANCE.NS, AAPL). */
data class WatchStock(
    val ticker: String,
    val name: String,
    val market: Market,
    val symbol: String,
    val exchange: String,
    val aliases: List<String> = emptyList(),
)

data class Quote(
    val price: Double,
    val changePct: Double,
    val trend: List<Float>,
)

data class StockRow(
    val stock: WatchStock,
    val quote: Quote?,
    val articleCount: Int,
    val tone: Tone,
)

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val tag: String,
    val tone: Tone,
    val credibility: Double,
    val cluster: String,
)

/** One story in a feed: the best article of a cluster plus the other outlets that ran it. */
data class NewsItem(
    val article: Article,
    val tickers: List<String>,
    val extraSources: List<String>,
    val impact: Double,
)

data class UpcomingEvent(
    val ticker: String,
    val epochDay: Long,
    val label: String,
    val articleId: String?,
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

data class MarketIndex(
    val name: String,
    val value: Double,
    val changePct: Double,
)

data class SourceHealth(
    val id: String,
    val name: String,
    val lastAttempt: Long,
    val lastSuccess: Long,
    val lastError: String?,
    val items: Int,
)

data class RefreshState(
    val running: Boolean = false,
    val message: String = "",
    val error: String? = null,
)
