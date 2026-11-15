<h1 align="center">Stocker</h1>

<p align="center">
  Every credible headline for every stock you follow, on one pure-black screen.<br>
  A free, on-device Android app for Indian (NSE/BSE) and US stock watchlists.
</p>

<p align="center">
  <a href="https://github.com/dotpmm/stocker/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/dotpmm/stocker/actions/workflows/android.yml/badge.svg"></a>
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.0-9DB8FF">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC97">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF">
  <img alt="License" src="https://img.shields.io/badge/license-proprietary-555">
</p>

---

## Status: v1.0.0 is a UI preview

The first release ships the finished **Obsidian** interface running on **sample data**. The stocks, prices and
headlines you see are illustrative. Live news, prices, background polling and AI summaries are the next milestones
(see the [roadmap](#roadmap)). It is installable and fully navigable, and is meant for judging the look and feel.

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
- [x] Watchlist with index strip (NIFTY 50, SENSEX), India / US tabs and trend lines
- [x] Stock detail: price, 1D to 1Y chart ranges, news-tone bar, upcoming events, news list
- [x] Floating solid navigation bar with sliding highlight and spring icons (no blur, light on the GPU)
- [x] Material-3 style motion with press feedback on every tap target
- [ ] Live news pipeline: RSS and Google News feeds, de-duplication, per-company linking
- [ ] Rule-based sentiment and event tagging (earnings, deals, regulatory, management)
- [ ] 12-hour background refresh (WorkManager) and manual refresh
- [ ] Add stocks by name or ticker, CSV import
- [ ] Clean article reader and optional Gemini summary
- [ ] Fundamentals "number fetcher": P/E, P/B, 52-week range, revenue and charts

## Design

Pure black, nothing but text and space. Selection is a sliding underline, the primary action is one solid bar, and
the only floating element is the navigation bar. The rules and the reasoning are in
[docs/design-directions.md](docs/design-directions.md).

## Tech

Kotlin 2.2, Jetpack Compose (Material 3), AGP 8.11, Gradle 8.14 with a version catalog, minSdk 26, targetSdk 36,
R8 minification and resource shrinking on release. Planned: Room, WorkManager, Ktor/OkHttp, kotlinx.serialization.
Details in [docs/architecture.md](docs/architecture.md).

## Build

```
git clone https://github.com/dotpmm/stocker.git
cd stocker
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (platform 36). More in [docs/building.md](docs/building.md). Releases are cut by
pushing a `v*` tag, see [docs/releasing.md](docs/releasing.md).

## Roadmap

1. **v1.0 (this release)**: Obsidian UI on sample data.
2. **v1.1**: data layer (Room), feed fetching, de-duplication and per-company news.
3. **v1.2**: sentiment/event tagging, upcoming events, 12-hour background refresh.
4. **v1.3**: add-stock search, article reader, Gemini summary sheet, settings and source health.
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
| [Building](docs/building.md) / [Releasing](docs/releasing.md) | Build and release process |

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) first, then open an issue or a pull request. Bug reports and news-source
suggestions are especially welcome.

## Contributors

| | Contributor | Role |
| --- | --- | --- |
| <a href="https://github.com/dotpmm"><img src="https://github.com/dotpmm.png" width="56" alt="dotpmm"></a> | [**Prajwal (@dotpmm)**](https://github.com/dotpmm) | Creator, product and design decisions, maintainer |

Stocker is built with AI assistance (Claude by Anthropic) under the direction and review of the maintainer.
Want to see your name here? See [CONTRIBUTING.md](CONTRIBUTING.md).

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
