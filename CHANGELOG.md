# Changelog

All notable changes to Stocker are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [2.1.0] - 2026-11-15

Polish pass on the v2.0 surface: legibility, scroll performance, and a lighter metal treatment.

### Changed
- **Type scale raised across the board.** Body 15→16sp, captions 12→13sp, micro-labels
  11→12sp, prices 14→15sp, hero 52→56sp. The 143 hardcoded per-screen size overrides were
  removed or raised, so nothing renders below 11sp.
- **Metal is now reserved, not repeated.** Rows, list cards, chips, tables and grouped settings
  are flat surfaces with a single hairline. Metal stays on the nav bar, the hero panel, the active
  segment pill, primary action pills, the search field and the bottom sheet — seven surfaces
  instead of thirty.
- **Less text where it was decorative.** Removed the redundant brand/live row, the caption under
  the hero numeral, the per-row "CURRENT/5Y" prefixes, the per-row "CRORE" suffix, and shortened
  every settings hint to one line.
- The **sector interdependence graph is removed** from the Markets screen and from Settings.

### Added
- **Ambient glow follows the container's rounded corners on all four sides.** One bloom plus a
  vertical and a horizontal rim pass, clipped inside the shape, so the light traces corners
  instead of leaking past them.
- **Swipe between tabs.** A decisive horizontal fling (88dp, mostly horizontal) moves one tab
  left or right, with the slide direction matching the swipe. Toggleable in Settings.
- **Smart start.** An empty watchlist opens Markets; otherwise the app opens Watchlist.
- **Metal nav bar toggle**, so the finish can be dropped from the bar independently.
- Settings rows for swipe, smart start and nav metal.

### Fixed
- **Scroll performance.** The film-grain overlay was issuing ~288,000 draw calls per frame; it is
  now a single pre-rendered tiled shader. Charts drop their PathMeasure and dash effect once the
  draw-in finishes, so a settled chart is a plain stroke. `indexOf()` inside the sector list is
  now `itemsIndexed`. `rememberQuery` no longer re-launches on an unchanged key. Palette changes
  re-root composition via `key` instead of through hundreds of snapshot reads.
- Deep-dive fundamental rows no longer wrap their values at the larger type size.

## [2.0.0] - 2026-11-15

The app gains a Coffee-Can research surface in the Monochrome Exchange design language, and
everything from v1.5 stays. News, the watchlist, the reader, events and the Gemini summary are all
still here — restyled into the same metal system, not removed.

### Added
- **Markets tab (macro research)** — NIFTY 50 hero with a rolling-digit numeral and inline
  sparkline, six macro sectors as metal plates with the market-cap-weighted trailing P/E against
  a 5Y baseline, verdict badges, and an optional sector interdependence graph (off by default,
  toggleable in Settings).
- **Sector drill-down** — every mapped Nifty-200 stock in the sector, with a persisted filter
  panel (Structure / Valuation / Growth & Size, moat and sort selects, a live "N survive" counter)
  saved under `cc-filters`.
- **Stock deep-dive sheet** — verdict banner, curated narrative, valuation gauge, fundamentals,
  noise-filter chart with 5D/1M/6M/1Y chips and earnings-date dots, and a Google Finance link.
- **Settings screen** (replaces the "More" tab) — see below.
- **Appearance settings, all live** — base tone (AMOLED / Deep / Charcoal / Graphite), metal finish
  strength, film grain, direction-accent intensity, shape language, density, motion level, ambient
  glow, border light traveller, search burst, haptics, direction glyphs.
- **Behaviour settings, all live** — dependency graph on/off, macro auto-refresh interval,
  universe peers, live search, default chart range, chart fill, chart glow, earnings dots, 52-week
  range, story window, stories per company, duplicate clustering, tone markers, event tags,
  per-source on/off, background refresh + interval, long cache, compact watchlist rows,
  alphabetical watchlist sort.
- **AMOLED mode** — a true-black wall where the plates keep a raised luminance and lit top edge, so
  the metal reads as metal instead of a flat hole.
- Ported the reference computations verbatim (sector P/E, badge thresholds, reality check, gauge
  span, own 5Y P/E, 5Y revenue CAGR, earnings-reaction bisect) and shipped the curated dataset as
  assets.
- TTL cache with stale-serve-on-failure for research payloads.

### Changed
- The whole UI is rebuilt in the metal language: one 118° beam from the top-left, top-lit plates
  with a bright upper bevel, muted `#5fa87a`/`#d3705f` direction pair, tabular mono numerals with
  Indian lakh grouping, draw-in charts, and the strictly-bounded search burst.
- The default tone is **Deep**, noticeably darker than v1.5's canvas.
- Every screen now honours reduced motion from the OS *and* the app setting.
- Numbers use hand-rolled Indian grouping, because `NumberFormat`'s `en-IN` output differs between
  the JVM and Android.

### Fixed
- Charts keep their under-curve gradient quiet (0.22 → 0) instead of rendering a saturated block.
- Percentages no longer show a doubled direction glyph.
- The hero index no longer clips its final digit on a 360 dp phone.
- Settings hints are legible at their smaller size.

## [1.5.0] - 2026-11-15

The first working version: real news, prices and events instead of sample data.

### Added
- Live news pipeline: about 20 feeds (ET, Mint, BusinessLine, NDTV Profit, Pulse, SEBI, RBI, CNBC, MarketWatch and
  Google News slices for Moneycontrol, Business Standard, CNBC-TV18, Financial Express) plus a per-company Google News
  query for every stock you follow.
- Rule-based processing, no AI: company linking by name, alias and ticker; duplicate clustering across outlets;
  event tags (Results, Rating, M&A, Order win, Regulatory, Dividend and more); keyword tone; source-credibility and
  recency-weighted ranking ("Top").
- Upcoming events detected from news (results, board meetings, record dates, AGMs).
- Prices, 1D / 1W / 1M / 3M / 1Y charts and NIFTY 50 / SENSEX for Indian stocks via Yahoo Finance.
- Add stocks by name or ticker (offline suggestions for the NIFTY 50 and large US names, online search for the rest);
  remove from the detail screen.
- Reader view with extracted text where the publisher allows it, related coverage and an "Open original" link.
- Optional AI summary with your own Gemini key (newest Flash model picked automatically, result cached).
- News, Events and Settings tabs; Settings includes the Gemini key, refresh status and a source-health list.
- Refresh every 12 hours in the background (WorkManager), on demand, and when opening the app after two hours.
- Unit tests for sentiment, linking, event extraction, duplicate detection and Gemini response parsing; CI now runs
  them and also builds the release variant.

### Changed
- Sample data and the "Coming soon" placeholders are gone.
- Storage is plain SQLite and the platform HTTP/XML stack instead of Room and a networking library (see ADR 0002).

### Known limitations
- Feeds and Yahoo endpoints could not be tested from the build environment. Any that are blocked or changed show up as
  red squares under Settings > Source health. Please report which ones fail.
- US stocks are news-only (no prices). NSE and BSE filings are not included yet.
- Google News links open the publisher through a redirect, so the in-app reader shows only the headline for those.
- Tone and event tags are simple keyword rules and can be wrong.
- Still signed with a CI-generated debug key (see docs/releasing.md), so an uninstall is needed before a future release
  signed with a permanent key.

## [1.0.0] - 2026-11-15

First installable build: the Obsidian interface running on sample data. **There is no live news, price data or AI
summary yet.** Every stock, price and headline you see is illustrative.

### Added
- Obsidian design system: pure black canvas, no borders, Inter and Space Grotesk typography with tabular numerals.
- Watchlist screen with NIFTY 50 / SENSEX strip, India and US news tabs (sliding underline), stock rows with trend lines.
- Stock detail screen with price, 1D / 1W / 1M / 3M / 1Y chart ranges, news-tone bar, AI summary action, upcoming events
  and news list.
- Floating solid navigation bar with sliding highlight and icon spring (Watchlist, News, Events, Settings).
- Material-3 style motion: emphasized easing, press feedback on every tap target, animated screen transition.
- Adaptive launcher icon and edge-to-edge system bars.
- GitHub Actions: debug build on every push, tag-triggered release build.

### Known limitations
- Sample data only; refresh, add-stock and AI summary buttons are not wired up.
- News, Events and Settings tabs are placeholders.
- Release APK is signed with a CI-generated debug key until a permanent release key is configured, so a later
  properly-signed release will require uninstalling this one first.
