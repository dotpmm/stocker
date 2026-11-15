# Stocker

A free, pitch-black Android app that watches a stock watchlist (NSE/BSE first, with a US news section) and
turns the day's news into a clean, per-company feed. No backend, no accounts: everything runs on the device.

> Status: **planning / pre-build.** No app code yet. See `docs/` for the product spec, source research and
> architecture decisions.

## What it will do

- Take a watchlist of 5-50 stocks (search by name or ticker; CSV import later).
- Poll many credible news sources in the background every 12 hours, plus a manual refresh button.
- Segregate news per company and per market using rules only (no AI): entity linking, rule-based sentiment,
  event tagging (results, guidance, orders, M&A, regulatory, dividends, management changes).
- Show upcoming events (earnings and other scheduled catalysts) and indirect-impact news.
- A very clean news reader.
- Optional "AI summary" button beside each stock using the user's own free Gemini API key.
- Later: a fundamentals "number fetcher" (P/E, P/B, 52-week range, revenue, charts) and AI over those numbers.

## Docs

| File | Contents |
| --- | --- |
| `docs/product-spec.md` | Goals, scope by phase, decisions made so far |
| `docs/research/news-sources.md` | Tiered list of Indian + US news sources and what works or is blocked |
| `docs/research/github-prior-art.md` | Existing open-source projects reviewed and what to borrow |
| `docs/research/fundamentals-data.md` | Free data options for the later number fetcher |
| `docs/architecture.md` | On-device pipeline design |
| `docs/decisions/` | Architecture decision records (ADRs) |

Not financial advice. Any AI or rule-based verdict in the app is informational only.
