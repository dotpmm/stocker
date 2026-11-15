# Changelog

All notable changes to Stocker are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
