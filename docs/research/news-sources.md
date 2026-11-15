# News source research

Research date: 2026-11-15.

**How this was verified.** The author's machine could not reach feed hosts (network allowlist), so direct liveness
tests were not possible. Instead, status was taken from: (a) one live fetch of the Pulse feed, and (b) the feed
configs and notes of open-source projects that run these feeds today (see `github-prior-art.md`). Treat every URL
below as "likely good" until the in-app **source health check** confirms it on a real device.

## Tier 1: official (authoritative, low volume)

| Source | Use | Notes |
| --- | --- | --- |
| SEBI RSS | Regulatory orders, circulars | Official, stable |
| RBI RSS | Policy, press releases | Official, stable |
| NSE/BSE corporate announcements | Results, board meetings, orders, filings | Best data, hardest to reach. Both sit behind Akamai bot protection; ordinary HTTP clients are rejected. See "Exchange filings" below. Best-effort only. |

## Tier 2: verified publisher RSS (works)

| Source | Feed | Evidence |
| --- | --- | --- |
| Economic Times, Markets | `https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms` | Used by several projects |
| Economic Times, Stocks | `https://economictimes.indiatimes.com/markets/stocks/rssfeeds/2146842.cms` | NewsEdge |
| Economic Times, Stocks news | `https://economictimes.indiatimes.com/markets/stocks/news/rssfeeds/2146843.cms` | stock-intel |
| Economic Times, Corporate trends | `https://economictimes.indiatimes.com/news/company/corporate-trends/rssfeeds/2143429.cms` | NewsEdge |
| Mint, Markets | `https://www.livemint.com/rss/markets` | finsent, NewsEdge |
| Mint, Companies | `https://www.livemint.com/rss/companies` | NewsEdge |
| BusinessLine, Markets / Companies / Stock markets | `https://www.thehindubusinessline.com/markets/feeder/default.rss` and sibling feeds | finsent, NewsEdge |
| Pulse by Zerodha (curated aggregator) | `https://pulse.zerodha.com/feed.php` (also `feed.xml`) | **Verified live by direct fetch** |
| NDTV Profit | `https://feeds.feedburner.com/ndtvprofit-latest` | finsent |
| Investing.com India | `https://in.investing.com/rss/news_25.rss` | NewsEdge |

## Tier 3: no usable native RSS, so go through Google News

| Publisher | Problem | Workaround |
| --- | --- | --- |
| Moneycontrol | Native `/rss/*.xml` frozen since Apr 2024 or returns 403 to scripted clients | Google News `site:moneycontrol.com` query |
| Business Standard | `/rss/*.rss` returns 403 to non-browser clients | Google News `site:business-standard.com` |
| CNBC-TV18 | No dependable RSS | Google News `site:cnbctv18.com` |
| Financial Express | No dependable RSS | Google News `site:financialexpress.com` |

Google News RSS patterns:

- Per company: `https://news.google.com/rss/search?q="<Company Name>" when:1d&hl=en-IN&gl=IN&ceid=IN:en`
- Per publisher slice: same endpoint with `site:<domain>` plus market keywords.
- Rate limit politely: about 1.5 s between requests per domain, about 3 s for `news.google.com`.
- Google News links are redirect wrappers; resolve to the publisher URL for dedupe and for the reader.

Also available per ticker: Yahoo Finance per-symbol news (unofficial endpoint).

## US news section (candidates, not yet verified)

CNBC, MarketWatch, Nasdaq RSS categories, Seeking Alpha, SEC EDGAR filings feeds.

## Exchange filings (NSE / BSE)

- BSE: `https://api.bseindia.com/BseIndiaAPI/api/AnnSubCategoryGetData/w`
- NSE: `https://www.nseindia.com/api/corporate-announcements`, `.../api/corporates-corporateActions`,
  `.../api/corporates-financial-results`, `.../api/corporate-share-holdings-master`,
  `.../api/historicalOR/bulk-block-short-deals`
- A working Python implementation needs a Chrome-grade TLS fingerprint (`curl_cffi`, `impersonate="chrome"`), a
  warm-up GET on the exchange's HTML page for cookies, and about 1 s between calls.
- On Android, plain OkHttp is likely to be blocked. Option: warm cookies through a hidden WebView, then reuse them.
- These are unofficial endpoints. Treat as best-effort with graceful degradation, and weigh the ToS risk before
  shipping to other users.

## Ingestion rules

1. Poll every 12 hours (WorkManager periodic work) plus manual refresh. 12 hours is well inside WorkManager limits.
2. Conditional requests (ETag / If-Modified-Since) wherever the server supports them.
3. Per-domain rate limiting and a descriptive User-Agent.
4. Dedupe syndicated copies: token-set fuzzy ratio >= 90 within a 48 h window, or Jaccard on title tokens.
5. Keep a per-source health record (last success, last error, items per fetch) and show it in-app.
6. Prefer title/summary only in the feed list; fetch the full article only when the user opens it, and link out to
   the publisher.
