package com.dotpmm.stocker.domain.model

/** Everything the stock detail screen needs. Real data replaces the sample source later. */
data class StockDetail(
    val stock: Stock,
    val ranges: List<RangeSeries>,
    val toneCounts: ToneCounts,
    val events: List<UpcomingEvent>,
    val news: List<NewsItem>,
)

data class MarketIndex(
    val name: String,
    val value: Double,
    val changePct: Double,
)
