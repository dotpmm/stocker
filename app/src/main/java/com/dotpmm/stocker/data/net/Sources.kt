package com.dotpmm.stocker.data.net

import com.dotpmm.stocker.domain.model.Market
import java.net.URLEncoder

enum class SourceKind { Official, Publisher, GoogleNews }

data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val credibility: Double,
    val market: Market,
    val kind: SourceKind,
)

/** The curated feed list. Every entry is health-checked on each refresh and shown in Settings. */
object Sources {
    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    fun googleNewsUrl(query: String, market: Market): String {
        val locale = if (market == Market.India) "hl=en-IN&gl=IN&ceid=IN:en" else "hl=en-US&gl=US&ceid=US:en"
        return "https://news.google.com/rss/search?q=${enc(query)}&$locale"
    }

    private fun site(id: String, name: String, domain: String, terms: String) = FeedSource(
        id = id,
        name = "$name (via Google News)",
        url = googleNewsUrl("site:$domain ($terms) when:2d", Market.India),
        credibility = 0.9,
        market = Market.India,
        kind = SourceKind.GoogleNews,
    )

    val generic: List<FeedSource> = listOf(
        FeedSource("sebi", "SEBI", "https://www.sebi.gov.in/sebirss.xml", 1.0, Market.India, SourceKind.Official),
        FeedSource("rbi", "RBI", "https://www.rbi.org.in/pressreleases_rss.xml", 1.0, Market.India, SourceKind.Official),
        FeedSource(
            "et-markets", "Economic Times · Markets",
            "https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource(
            "et-stocks", "Economic Times · Stocks",
            "https://economictimes.indiatimes.com/markets/stocks/rssfeeds/2146842.cms", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource(
            "et-stock-news", "Economic Times · Stock news",
            "https://economictimes.indiatimes.com/markets/stocks/news/rssfeeds/2146843.cms", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource(
            "et-corporate", "Economic Times · Corporate trends",
            "https://economictimes.indiatimes.com/news/company/corporate-trends/rssfeeds/2143429.cms", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource("mint-markets", "Mint · Markets", "https://www.livemint.com/rss/markets", 0.9, Market.India, SourceKind.Publisher),
        FeedSource("mint-companies", "Mint · Companies", "https://www.livemint.com/rss/companies", 0.9, Market.India, SourceKind.Publisher),
        FeedSource(
            "bl-markets", "BusinessLine · Markets",
            "https://www.thehindubusinessline.com/markets/feeder/default.rss", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource(
            "bl-companies", "BusinessLine · Companies",
            "https://www.thehindubusinessline.com/companies/feeder/default.rss", 0.9, Market.India, SourceKind.Publisher,
        ),
        FeedSource("pulse", "Pulse by Zerodha", "https://pulse.zerodha.com/feed.php", 0.75, Market.India, SourceKind.Publisher),
        FeedSource("ndtv-profit", "NDTV Profit", "https://feeds.feedburner.com/ndtvprofit-latest", 0.85, Market.India, SourceKind.Publisher),
        FeedSource("investing-in", "Investing.com India", "https://in.investing.com/rss/news_25.rss", 0.75, Market.India, SourceKind.Publisher),
        site("gn-moneycontrol", "Moneycontrol", "moneycontrol.com", "stocks OR shares OR results"),
        site("gn-business-standard", "Business Standard", "business-standard.com", "stocks OR shares OR results"),
        site("gn-cnbctv18", "CNBC-TV18", "cnbctv18.com", "stocks OR shares OR results"),
        site("gn-financial-express", "Financial Express", "financialexpress.com", "stocks OR shares OR results"),
        FeedSource("cnbc-us", "CNBC · Top news", "https://www.cnbc.com/id/100003114/device/rss/rss.html", 0.9, Market.US, SourceKind.Publisher),
        FeedSource(
            "marketwatch-us", "MarketWatch · Top stories",
            "https://feeds.content.dowjones.io/public/rss/mw_topstories", 0.9, Market.US, SourceKind.Publisher,
        ),
    )

    const val COMPANY_SOURCE_ID = "gn-company"
    const val COMPANY_SOURCE_NAME = "Google News · per company"
    const val PRICES_SOURCE_ID = "yahoo-prices"
    const val PRICES_SOURCE_NAME = "Yahoo Finance · prices"

    /** Per-company query. Quoted name plus finance words keeps unrelated namesakes out. */
    fun companyUrl(name: String, market: Market): String {
        val terms = if (market == Market.India) "shares OR stock OR results OR earnings" else "stock OR shares OR earnings"
        return googleNewsUrl("\"$name\" ($terms) when:7d", market)
    }
}
