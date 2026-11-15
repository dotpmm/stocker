# GitHub prior art

Reviewed 2026-11-15. All repos below were cloned read-only for reference; none of their code is copied into
Stocker. Check each project's licence before reusing anything beyond ideas and public feed URLs.

| Repo | What it is | What to borrow |
| --- | --- | --- |
| [abhisheksatapathy-learner/finsent](https://github.com/abhisheksatapathy-learner/finsent) | Indian market sentiment from RSS | Small, working feed list (Pulse, ET, Mint, NDTV Profit, BusinessLine, Moneycontrol) |
| [Varun61/NewsEdge](https://github.com/Varun61/NewsEdge) | Indian stock news aggregator | Broadest feed list: ET sections, Mint, Business Standard, BusinessLine, Pulse, Investing.com India, Google News `site:` slices |
| [kartikey1232/stock-intel](https://github.com/kartikey1232/stock-intel) | News intelligence pipeline | `config/news_sources.yaml` with per-domain rate limits (1.5 s default, 3 s for Google News), story grouping (similarity 90, 48 h window), full-text extraction thresholds, notes on which feeds are dead |
| [a-ramakrishnan/stock_news_analyzer](https://github.com/a-ramakrishnan/stock_news_analyzer) | News plus exchange data | `sources/exchange_http.py`: how to reach NSE/BSE announcements, results, shareholding and bulk deals (Chrome TLS impersonation, cookie warm-up, 1 s delay) |
| [Vipluv01/nse-multi-agent-trading](https://github.com/Vipluv01/nse-multi-agent-trading) | NSE multi-agent trading research | Independent confirmation that Moneycontrol RSS returns 403 and ET RSS works |
| [Pr0zak/stocktracker](https://github.com/Pr0zak/stocktracker) | Android stock tracker (Kotlin) | `YahooFinanceService.kt`: Yahoo chart endpoint with quote, sparkline and 52-week range from one payload; graceful degradation to empty chart; optional self-hosted price proxy as fallback |
| [BennyThadikaran/NseIndiaApi](https://github.com/BennyThadikaran/NseIndiaApi) | Python NSE client | Reference for NSE endpoints, session handling and symbol quirks |

## Other names that came up in searches (not reviewed in depth)

nse-scanner, Blue-Ribbon company-tracker, TickerWatch, indian-market-mcp, jugaad-data, nsepython,
stockpulse-android. Look at these again when building the fundamentals phase (jugaad-data and nsepython in particular).

## Takeaways

1. The same core feed set recurs across projects: ET, Mint, BusinessLine, Pulse, NDTV Profit.
2. Moneycontrol and Business Standard native RSS are not dependable. Use Google News `site:` slices.
3. Exchange filings are the highest-value and highest-risk source; every working implementation fights bot protection.
4. Dedupe by fuzzy title similarity within a 48 h window is the common, proven approach.
5. Python projects use FinBERT for sentiment. That is not practical on-device for phase 1, so use a finance lexicon
   plus event regexes instead.
6. No existing open-source project combines a polished Android UI, per-stock segregation, rule-based sentiment and a
   BYO-key AI summary. That is the gap Stocker fills.
