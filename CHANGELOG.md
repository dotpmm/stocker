# Changelog

All notable changes to Stocker are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
