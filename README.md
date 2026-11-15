<h1 align="center">Stocker</h1>

<p align="center">
  Every credible headline for every stock you follow, on one pure-black screen.<br>
  A free, on-device Android app for Indian (NSE/BSE) and US stock watchlists.
</p>

<p align="center">
  <a href="https://github.com/dotpmm/stocker/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/dotpmm/stocker/actions/workflows/android.yml/badge.svg"></a>
  <img alt="Version" src="https://img.shields.io/badge/version-1.5.0-9DB8FF">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC97">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF">
  <img alt="License" src="https://img.shields.io/badge/license-proprietary-555">
</p>

---

## Status: v1.5, the first working app

v1.5 replaces the sample data of v1.0 with the real thing: live news from curated feeds, per-company sorting,
tone and event tags, prices for Indian stocks, a 12-hour background refresh, a reader, and an optional Gemini
summary. It is a first build of the data layer, so some feeds will be down or blocked on any given day; the
**Source health** list in Settings shows exactly which ones worked on your phone.

Get it from the [Releases](https://github.com/dotpmm/stocker/releases) page (Android 8.0 or newer). The repository
is currently private, so releases are visible only to people with access.

## What Stocker is

You keep a watchlist of 5 to 50 companies. Stocker collects news from a curated set of credible sources, sorts it
per company **without AI** (rules for entity matching, sentiment and event tags), shows what is coming up
(earnings, board meetings, major developments), and presents everything in a clean reader. An optional button asks
Gemini, with **your own free API key**, to summarise a stock's recent news. Nothing leaves your phone except the
requests to the news feeds and to Gemini when you press the button.

| | |
| --- | --- |
| Markets | NSE / BSE first-class. US stocks are news-only. |
| Cost | Free. No account, no backend, no ads, no tracking. |
| Freshness | Refreshes every 12 hours in the background, plus pull-to-refresh. |
| AI | Optional. Bring your own Gemini key; it is stored only on the device. |

## Features

- [x] Obsidian design system: pure black canvas, no borders or cards, Inter and Space Grotesk, tabular numerals
- [x] Watchlist of NSE, BSE and US stocks, added by company name or ticker (built-in suggestions work offline)
- [x] News from about 20 sources: ET, Mint, BusinessLine, NDTV Profit, Pulse, SEBI, RBI, CNBC, MarketWatch, plus
      Moneycontrol, Business Standard, CNBC-TV18 and Financial Express through Google News
- [x] Per-company news sorted without AI: entity linking, duplicate clustering ("+3 sources"), event tags
- [x] Rule-based tone (positive / neutral / negative) per story and per stock
- [x] Upcoming events (results, board meetings, record dates) detected from news
- [x] Prices, 1D to 1Y charts and NIFTY 50 / SENSEX for Indian stocks (unofficial Yahoo Finance endpoint)
- [x] Reader view with related coverage and a link to the original
- [x] Optional AI summary with your own Gemini key, cached per set of stories
- [x] 12-hour background refresh, plus refresh on demand and when you open the app after a while
- [x] Source health screen
- [ ] Topic profiles for indirect-impact news (peers, executives, regulators)
- [ ] NSE and BSE exchange filings (bot-protected, needs a WebView cookie approach)
- [ ] CSV watchlist import, home-screen widgets
- [ ] Fundamentals "number fetcher": P/E, P/B, 52-week range, revenue and charts

## Design

Pure black, nothing but text and space. Selection is a sliding underline, the primary action is one solid bar, and
the only floating element is the navigation bar. The rules and the reasoning are in
[docs/design-directions.md](docs/design-directions.md).

## Tech

Kotlin 2.2, Jetpack Compose (Material 3), AGP 8.11, Gradle 8.14 with a version catalog, minSdk 26, targetSdk 36,
R8 minification and resource shrinking on release. Deliberately few dependencies: plain SQLite, the platform HTTP
and XML parsers, WorkManager and coroutines, so there is no annotation-processing step to break. Unit tests cover
the rule-based language pipeline. Details in [docs/architecture.md](docs/architecture.md).

## Build

```
git clone https://github.com/dotpmm/stocker.git
cd stocker
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (platform 36). More in [docs/building.md](docs/building.md). Releases are cut by
pushing a `v*` tag, see [docs/releasing.md](docs/releasing.md).

## Roadmap

1. **v1.0**: Obsidian UI on sample data.
2. **v1.5 (this release)**: live news pipeline, prices, events, reader, AI summary, background refresh.
3. **v1.6**: topic profiles for indirect-impact news, per-source on/off, notifications (off by default).
4. **v1.7**: exchange filings, CSV import, widgets.
5. **v2**: fundamentals number fetcher and AI questions over those numbers.

## Documentation

| Doc | What it covers |
| --- | --- |
| [Product spec](docs/product-spec.md) | What the app does and does not do |
| [Architecture](docs/architecture.md) | Layers, data flow, tech choices |
| [Design directions](docs/design-directions.md) | The Obsidian rules |
| [News sources research](docs/research/news-sources.md) | Credible Indian and US sources |
| [Prior art](docs/research/github-prior-art.md) | What similar open projects do |
| [Fundamentals data](docs/research/fundamentals-data.md) | Where the numbers could come from |
| [Decision 0001](docs/decisions/0001-app-framework.md) | Why native Kotlin |
| [Decision 0002](docs/decisions/0002-storage-and-dependencies.md) | Why plain SQLite and few dependencies |
| [Building](docs/building.md) / [Releasing](docs/releasing.md) | Build and release process |

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) first, then open an issue or a pull request. Bug reports and news-source
suggestions are especially welcome.

## Acknowledgements

- [Inter](https://rsms.me/inter/) and [Space Grotesk](https://fonts.google.com/specimen/Space+Grotesk), SIL Open Font License 1.1 ([licences](docs/third-party/))
- Android Jetpack, Jetpack Compose and Kotlin, Apache License 2.0
- Market and news publishers whose public feeds the app reads; articles remain their publishers' property and open in the source

## Licence

Proprietary, source-available. All rights reserved; see [LICENSE](LICENSE). Personal, non-commercial use of release
builds is allowed.

## Disclaimer

Stocker provides information only. Nothing in the app is financial advice, an offer, or a recommendation to buy or
sell any security. Sentiment tags are produced by simple rules and can be wrong.
