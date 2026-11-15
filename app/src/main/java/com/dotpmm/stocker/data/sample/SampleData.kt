package com.dotpmm.stocker.data.sample

import com.dotpmm.stocker.domain.model.MarketIndex
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.RangeSeries
import com.dotpmm.stocker.domain.model.Stock
import com.dotpmm.stocker.domain.model.StockDetail
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.domain.model.ToneCounts
import com.dotpmm.stocker.domain.model.UpcomingEvent

/**
 * Illustrative placeholder content so the UI can be built and reviewed before the news
 * pipeline exists. None of these figures, headlines or dates are real market data.
 */
object SampleData {
    const val UPDATED_LABEL = "Updated 2h ago · next refresh in 10h"

    val indices = listOf(
        MarketIndex("NIFTY 50", 25412.30, 0.52),
        MarketIndex("SENSEX", 83104.75, 0.48),
    )

    val stocks = listOf(
        Stock("RELIANCE", "Reliance Industries", Market.India, 2914.60, 1.24, 9, Tone.Positive,
            listOf(1f, 2f, 1.8f, 3.5f, 3f, 4.5f, 5.2f)),
        Stock("HDFCBANK", "HDFC Bank", Market.India, 1682.15, -0.42, 6, Tone.Neutral,
            listOf(5f, 4.6f, 4.8f, 3.8f, 4f, 3f, 2.8f)),
        Stock("TCS", "Tata Consultancy Services", Market.India, 3841.90, 0.18, 4, Tone.Positive,
            listOf(2f, 1.5f, 2.5f, 2.3f, 3.5f, 3.2f, 4f)),
        Stock("INFY", "Infosys", Market.India, 1512.30, -1.05, 7, Tone.Negative,
            listOf(5f, 4.2f, 4.5f, 3f, 2.5f, 1.6f, 1f)),
        Stock("SBIN", "State Bank of India", Market.India, 812.40, 0.31, 5, Tone.Positive,
            listOf(2f, 2.6f, 2.3f, 3.4f, 3.1f, 4.2f, 3.9f)),
        Stock("NVDA", "Nvidia", Market.US, null, null, 12, Tone.Positive, emptyList()),
        Stock("AAPL", "Apple", Market.US, null, null, 8, Tone.Neutral, emptyList()),
        Stock("MSFT", "Microsoft", Market.US, null, null, 5, Tone.Negative, emptyList()),
    )

    private fun fromY(vararg y: Int): List<Float> = y.map { 72f - it }

    private val sharedRanges = listOf(
        RangeSeries("1W", "Past week", 62.10, 2.18,
            fromY(60, 54, 58, 46, 50, 40, 44, 34, 38, 28, 32, 22, 26, 14, 10)),
        RangeSeries("1M", "Past month", -48.35, -1.63,
            fromY(14, 20, 16, 26, 22, 32, 28, 38, 34, 46, 42, 52, 48, 58, 56)),
        RangeSeries("3M", "Past 3 months", 210.40, 7.78,
            fromY(58, 56, 50, 54, 44, 48, 36, 40, 30, 34, 22, 28, 18, 20, 8)),
        RangeSeries("1Y", "Past year", 402.15, 16.0,
            fromY(64, 60, 62, 52, 56, 44, 48, 38, 42, 30, 34, 24, 20, 14, 6)),
    )

    private val sampleNews = listOf(
        NewsItem("Economic Times", "1h ago", "Reliance Industries board to consider quarterly results on 22 November",
            "Results", Tone.Neutral, extraSources = 3),
        NewsItem("Mint", "3h ago", "Brokerages raise target prices ahead of the festive-quarter numbers",
            "Upgrade", Tone.Positive),
    )

    private val sampleEvents = listOf(
        UpcomingEvent("22", "NOV", "Q2 results"),
        UpcomingEvent("29", "NOV", "Dividend record date"),
    )

    fun detailFor(stock: Stock): StockDetail {
        val price = stock.price ?: 0.0
        val pct = stock.changePct ?: 0.0
        val dayAbs = price - price / (1 + pct / 100.0)
        val day = RangeSeries("1D", "Today", dayAbs, pct, stock.trend)
        return StockDetail(
            stock = stock,
            ranges = listOf(day) + sharedRanges,
            toneCounts = ToneCounts(positive = 6, neutral = 2, negative = 1),
            events = sampleEvents,
            news = sampleNews,
        )
    }
}
