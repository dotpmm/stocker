<h1 align="center">Stocker</h1>

<p align="center">
  India's macro cycle first, stocks last.<br>
  A Coffee-Can research surface for NSE equities, in the Monochrome Exchange design language.
</p>

<p align="center">
  <a href="https://github.com/dotpmm/stocker/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/dotpmm/stocker/actions/workflows/android.yml/badge.svg"></a>
  <img alt="Version" src="https://img.shields.io/badge/version-2.1.0-9DB8FF">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC97">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF">
  <img alt="License" src="https://img.shields.io/badge/license-proprietary-555">
</p>

---

## Status: v2.1, news plus research

v2.0 added the Coffee-Can macro research surface described in [`FUNCTIONALITY.md`](FUNCTIONALITY.md),
rendered in the metal-and-blur language of [`design.md`](design.md). **Everything from v1.5 is still
here** — the watchlist, the news pipeline, the reader, events and the optional Gemini summary — all
restyled into the same system. v2.1 is a polish pass: larger type, a lighter metal treatment and much
smoother scrolling.

## What Stocker is

Two halves that share one design language:

**Your watchlist and its news.** You keep 5 to 50 companies. Stocker collects news from about 20
curated sources, sorts it per company without AI (entity linking, duplicate clustering, rule-based
tone and event tags), shows what is coming up, and presents it in a clean reader. An optional
button asks Gemini, with your own free key, to summarise a stock's recent news.

**The macro research surface.** NIFTY 50 hero and six macro sectors, each judged by its
market-cap-weighted trailing P/E against a 5Y baseline. Tap a sector for every mapped Nifty-200
stock in it, filtered by the persisted Coffee-Can toggles. Tap a row for the deep-dive: verdict,
valuation gauge, fundamentals, and a noise-filter chart with earnings dots.

The research philosophy: **the economic cycle first, stocks last. The valuation gauge and the 5Y
revenue CAGR are the noise filter.**

| | |
| --- | --- |
| Markets | NSE / BSE first-class. US stocks are news-only. |
| Cost | Free. No account, no backend, no ads, no tracking. |
| Freshness | Background refresh on your schedule, plus refresh on demand. |
| AI | Optional. Bring your own Gemini key; it is stored only on the device. |
| Data | Yahoo Finance's public endpoints, TTL-cached with stale-serve-on-failure. |
| Design | Monochrome + metal, direction-only red/green, tabular numerals, draw-in motion. |

## The design language

**Monochrome is the identity, colour is the signal.** 95% of the interface is black, white and
gray. The muted sage `#5fa87a` and clay `#d3705f` appear only to encode market direction, and
always carry a ▲/▼ glyph so colour is never the only signal. Every surface is a perfectly neutral
gray — depth comes from luminance, never from a hue.

**Surfaces are metal, lit from one place.** The app sits in a brushed-charcoal room with a single
118° beam entering from the top-left. Every raised panel is a top-lit plate with a bright upper
bevel that falls away down its face. Depth comes from the light's direction, never from borders.

**Motion is instructional.** The chart draws in line → gradient → glow → endpoint dot. Numerals
count up. Digits roll. The search box fires a dot-field aura for exactly 1050ms and then goes
completely quiet. While data is in flight the UI looks like it is *thinking* — sweeping skeletons,
a breathing glow, a dot orbiting the panel border. Spinners are banned.

**Reduced motion is complete, not partial.** Every transform collapses to an instant fade, charts
render fully drawn, the ticker stops rolling, and the burst never fires.

## Features

**Watchlist and news**
- [x] Watchlist of NSE, BSE and US stocks, added by name or ticker (offline suggestions included)
- [x] News from about 20 sources: ET, Mint, BusinessLine, NDTV Profit, Pulse, SEBI, RBI, CNBC,
      MarketWatch, plus Moneycontrol, Business Standard, CNBC-TV18 and Financial Express
- [x] Per-company sorting without AI: entity linking, duplicate clustering, event tags
- [x] Rule-based tone per story and per stock
- [x] Upcoming events (results, board meetings, record dates) detected from news
- [x] Prices, 1D–1Y charts and NIFTY 50 / SENSEX
- [x] Reader view with related coverage and a link to the original
- [x] Optional AI summary with your own Gemini key, cached per story set
- [x] Background refresh, plus refresh on demand
- [x] Source health list

**Macro research**
- [x] NIFTY hero, six sector plates, verdict badges, market-cap-weighted P/E vs 5Y baseline
- [x] Sector drill-down with persisted filters (Structure / Valuation / Growth & Size)
- [x] Deep-dive: reality-check gauge, narrative, fundamentals, noise-filter chart, earnings dots
- [x] Universe-wide search by ticker or company name
- [x] Google Finance deep link per stock

**Craft**
- [x] Metal + blur design system with the full Neural Expressive motion set
- [x] AMOLED mode that keeps the metal readable
- [x] Extensive appearance and behaviour settings, all live
- [x] TTL cache with stale-serve-on-failure
- [x] 267 unit tests, including reference-parity and render harnesses
- [ ] Light mode (the palette inverts; the direction pair is unchanged)
- [ ] Home-screen widget for the macro verdicts

## Tech

Kotlin 2.2, Jetpack Compose, AGP 8.11, Gradle 8.14 with a version catalog, minSdk 26, targetSdk 36,
R8 minification and resource shrinking on release. Deliberately few dependencies: plain SQLite for
the cache, the platform HTTP stack, and coroutines — no Room, no OkHttp, no DI, no annotation
processing.

The curated dataset (sectors, constituents, moats, narratives, dependency weights) and the
Nifty-200 list ship as versioned JSON assets. All computations are ported verbatim from the
reference implementation and pinned by tests in `ReferenceParityTest`.

## Build

```
git clone https://github.com/dotpmm/stocker.git
cd stocker
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 267 tests
```

Requires JDK 17+ and the Android SDK (platform 36). Releases are built by GitHub Actions, see
[docs/releasing.md](docs/releasing.md).

### Verifying without a device

`./gradlew testDebugUnitTest` renders the real screens through Robolectric and writes PNGs to
`app/build/screenshots/`, so the visual language can be inspected on a machine where the Android
emulator cannot run. The render harnesses also assert that charts survive degenerate series
(empty, single-point, perfectly flat) and that reduced motion leaves every glyph in place.

## Documentation

| Doc | What it covers |
| --- | --- |
| [Functionality spec](FUNCTIONALITY.md) | Every screen, computation and API payload this app implements |
| [Design guide](design.md) | The Monochrome Exchange + Metal Finish system and its hard rules |
| [Architecture](docs/architecture.md) | Layers, data flow, tech choices |
| [Building](docs/building.md) / [Releasing](docs/releasing.md) | Build and release process |

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) first, then open an issue or a pull request. Bug reports and news-source
suggestions are especially welcome.

## Credits

- [@woterr](https://github.com/woterr): Markets tab, sector filters, deep-dive sheet, settings, and the v2 design
  system (pull request #1)

## Licence

Proprietary, source-available. All rights reserved; see [LICENSE](LICENSE). Personal, non-commercial
use of release builds is allowed.

## Disclaimer

Stocker provides information only. Nothing in the app is financial advice, an offer, or a
recommendation to buy or sell any security. Verdicts are mechanical thresholds, not opinions, and
can be wrong. Sector P/E baselines are documented estimates; free historical sector P/E does not
exist.
