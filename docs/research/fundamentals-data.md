# Fundamentals data (later phase: "number fetcher")

Goal: P/E, P/B, ROE, market cap, 52-week high/low, revenue/profit/EPS trends, price charts, shareholding, all free.

| Need | Candidate source | Notes and risks |
| --- | --- | --- |
| Price chart, 52-week range, sparkline | Yahoo `https://query1.finance.yahoo.com/v8/finance/chart/{SYMBOL.NS or .BO}?range=...&interval=...` | No key. Unofficial, may change or rate-limit. One payload gives quote, range and history. Use `events=div,splits` for dividends and splits. |
| P/E, P/B, ROE, market cap | Yahoo quote / fundamentals endpoints | Unofficial. Needs crumb/cookie handling at times. |
| Quarterly revenue, profit, EPS | NSE `corporates-financial-results` / results comparison | Behind bot protection (see news-sources.md) |
| Shareholding pattern | NSE `corporate-share-holdings-master` | Same caveat |
| Screener-style ratios | Screener.in | Scraping, ToS risk. Do not rely on it for a distributed app. |
| Broad screener data | TradingView scanner | Unofficial |
| Mutual funds | AMFI / mfapi.in | Public, if funds are ever added |

## Symbol rules

- NSE tickers use `.NS`, BSE use `.BO`.
- Share classes use a dash (e.g. `BAJAJ-AUTO.NS`), and URL-encode specials. Indices use `%5E` (e.g. `%5ENSEI`).
- Keep a local alias table (from NSE `EQUITY_L.csv`) mapping company names and aliases to the ticker.

## Approach

1. Fetch lazily per stock when the user opens it, cache with a TTL (prices short, fundamentals long).
2. Show a degraded view (cached or empty chart) instead of an error when an endpoint fails.
3. Keep all providers behind one interface so a source can be swapped without touching the UI.
4. The later AI button reuses the same prompt pipeline as the news summary, adding these numbers to the context.
5. Because most endpoints are unofficial, decide before public release whether to ship them or to offer a clearly
   labelled "data may be delayed or unavailable" experience.
