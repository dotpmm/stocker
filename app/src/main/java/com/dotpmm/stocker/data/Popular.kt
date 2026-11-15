package com.dotpmm.stocker.data

import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.WatchStock

/** Built-in suggestions with hand-written aliases, shown before the user types anything. */
object Popular {
    private fun nse(ticker: String, name: String, vararg aliases: String) = WatchStock(
        ticker = ticker, name = name, market = Market.India, symbol = "$ticker.NS", exchange = "NSE",
        aliases = aliases.toList(),
    )

    private fun usStock(ticker: String, name: String, exchange: String, vararg aliases: String) = WatchStock(
        ticker = ticker, name = name, market = Market.US, symbol = ticker, exchange = exchange,
        aliases = aliases.toList(),
    )

    val india: List<WatchStock> = listOf(
        nse("RELIANCE", "Reliance Industries", "Reliance", "RIL"),
        nse("TCS", "Tata Consultancy Services"),
        nse("HDFCBANK", "HDFC Bank"),
        nse("ICICIBANK", "ICICI Bank"),
        nse("INFY", "Infosys"),
        nse("BHARTIARTL", "Bharti Airtel", "Airtel"),
        nse("ITC", "ITC"),
        nse("SBIN", "State Bank of India", "SBI"),
        nse("LT", "Larsen & Toubro", "L&T"),
        nse("HINDUNILVR", "Hindustan Unilever", "HUL"),
        nse("KOTAKBANK", "Kotak Mahindra Bank", "Kotak Bank"),
        nse("AXISBANK", "Axis Bank"),
        nse("BAJFINANCE", "Bajaj Finance"),
        nse("MARUTI", "Maruti Suzuki"),
        nse("SUNPHARMA", "Sun Pharma", "Sun Pharmaceutical"),
        nse("M&M", "Mahindra & Mahindra", "M&M"),
        nse("HCLTECH", "HCL Technologies", "HCLTech"),
        nse("NTPC", "NTPC"),
        nse("ONGC", "ONGC", "Oil and Natural Gas Corporation"),
        nse("ULTRACEMCO", "UltraTech Cement"),
        nse("ADANIENT", "Adani Enterprises"),
        nse("ADANIPORTS", "Adani Ports"),
        nse("POWERGRID", "Power Grid", "Power Grid Corporation"),
        nse("WIPRO", "Wipro"),
        nse("TITAN", "Titan Company", "Titan"),
        nse("BAJAJFINSV", "Bajaj Finserv"),
        nse("COALINDIA", "Coal India"),
        nse("ASIANPAINT", "Asian Paints"),
        nse("NESTLEIND", "Nestle India"),
        nse("JSWSTEEL", "JSW Steel"),
        nse("TATASTEEL", "Tata Steel"),
        nse("TECHM", "Tech Mahindra"),
        nse("GRASIM", "Grasim Industries", "Grasim"),
        nse("HINDALCO", "Hindalco"),
        nse("CIPLA", "Cipla"),
        nse("DRREDDY", "Dr Reddy's Laboratories", "Dr Reddy's", "Dr. Reddy's"),
        nse("EICHERMOT", "Eicher Motors", "Royal Enfield"),
        nse("APOLLOHOSP", "Apollo Hospitals"),
        nse("BPCL", "Bharat Petroleum", "BPCL"),
        nse("HEROMOTOCO", "Hero MotoCorp"),
        nse("INDUSINDBK", "IndusInd Bank"),
        nse("BRITANNIA", "Britannia Industries", "Britannia"),
        nse("TRENT", "Trent"),
        nse("BEL", "Bharat Electronics"),
        nse("SBILIFE", "SBI Life Insurance", "SBI Life"),
        nse("HDFCLIFE", "HDFC Life"),
        nse("JIOFIN", "Jio Financial Services", "Jio Financial"),
        nse("TATACONSUM", "Tata Consumer Products", "Tata Consumer"),
        nse("ETERNAL", "Eternal", "Zomato"),
    )

    val us: List<WatchStock> = listOf(
        usStock("AAPL", "Apple", "NASDAQ", "Apple Inc"),
        usStock("MSFT", "Microsoft", "NASDAQ"),
        usStock("NVDA", "Nvidia", "NASDAQ"),
        usStock("GOOGL", "Alphabet", "NASDAQ", "Google"),
        usStock("AMZN", "Amazon", "NASDAQ"),
        usStock("TSLA", "Tesla", "NASDAQ"),
        usStock("META", "Meta Platforms", "NASDAQ", "Facebook"),
    )

    val all: List<WatchStock> = india + us

    fun search(query: String): List<WatchStock> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { s ->
            s.ticker.lowercase().contains(q) || s.name.lowercase().contains(q) || s.aliases.any { it.lowercase().contains(q) }
        }
    }
}
