# Stocker product spec (draft, pre-build)

Owner: Prajwal (GitHub: dotpmm). Last updated: 2026-11-15.

## Goal

A free Android app that tells you everything about the stocks you follow, starting with news. It must look and
feel like a finished consumer product (pitch black, polished, not "made for myself") because it will later be
shared with friends and others. Breadth of credible sources and the quality of the per-stock segregation are the
differentiators.

## Decisions made

| Area | Decision |
| --- | --- |
| Markets | Primarily NSE/BSE. A separate US section for news only (no US trading). |
| Source policy | As much news as possible, but credible and de-duplicated. No spam, no social media for now. |
| Polling | Background poll every 12 hours (the app holds the latest 12 hours of news even if never opened) plus a manual refresh that fetches immediately. |
| AI layer | Gemini only, user supplies their own free API key; key stays on the device. |
| Adding stocks | Search by name or ticker. CSV import later. |
| Dropped for now | Twitter/X and Reddit. |
| Backend | None. Fully on-device, so it is free to run and distribute. |

## Phase 1 scope

1. Watchlist (5-50 stocks) with name/ticker search.
2. News ingestion from the tiered source list (`docs/research/news-sources.md`).
3. Rule-based pipeline, no AI: dedupe, entity linking, sentiment, event tags, impact score, per-stock roll-up.
4. Per-stock news feed and a very clean article reader.
5. Upcoming events per stock (earnings and other scheduled catalysts).
6. Indirect-impact news via topic profiles (peers, executives, regulators, themes), e.g. a chip-maker CEO's China
   news affecting an AI-hardware stock.
7. AI summary button beside the stock name: stance (positive/negative/neutral), drivers, risks, upcoming events,
   with references to the articles used. Shows the rule-based tone next to the AI view. "Not financial advice" label.
8. Background refresh every 12 hours and manual refresh.
9. Source health screen (which feeds are alive, last fetch, error) so dead feeds are visible.

## Later phases

- Number fetcher: P/E, P/B, ROE, market cap, 52-week high/low, revenue/profit trends, price charts, shareholding
  (`docs/research/fundamentals-data.md`), then an AI button over the numbers plus news.
- CSV watchlist import.
- Home-screen widgets.
- More AI providers.
- Social sources, if a free and reliable route exists.

## Quality bar

- True black (#000) OLED theme, tabular numerals for prices, hairline separators, restrained accent colour.
- Smooth motion, haptics on key actions, skeleton loading, honest empty/error states.
- Never present a rule-based or AI verdict as advice.

## Open questions

- Native Kotlin vs Flutter (see `docs/decisions/0001-app-framework.md`).
- Design language: Material 3 Expressive vs custom pitch-black system vs Liquid-Glass-inspired.
- How to access exchange filings from Android given bot protection (best-effort only, see research).
