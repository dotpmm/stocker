# Architecture

Fully on-device. No server, no accounts, no tracking. The only network traffic is to Yahoo
Finance's public endpoints, and only when you open a screen that needs a number.

## Shape

```
assets/curated.json     six sectors, 50 constituents, moat taxonomy, 16 narratives, 8 dependency edges
assets/nifty200.json    200 NSE names with their NSE industry labels
        │
        ▼
research/CuratedData      the static layer + industry→sector folding + narrative templates
research/ResearchAssets   asset loaders; also the Point/EarningsDot value types
research/ResearchMath     every computation, pure and unit-tested
        │
        ▼
data/net/YahooResearch    the Yahoo endpoints, with the cookie+crumb handshake
data/net/YahooParsers     pure payload parsers (tested against recorded responses)
        │
        ▼
data/db/StockerDb         one table: a TTL cache of research payloads
        │
        ▼
research/ResearchRepository   TTL policy, stale-serve-on-failure, payload assembly
        │
        ▼
ui/screens + ui/components    watchlist, markets, news, events, settings + detail screens
```

## Layers

**`research/CuratedData`** — the qualitative layer, shipped in the APK as JSON. Sector order,
5Y P/E baselines, moat types, Coffee-Can flags, hand-written narratives and the dependency graph
weights all live here. `INDUSTRY_MAP` folds the raw NSE industry labels onto the six macro
sectors; industries outside the map (Metals, Realty, Chemicals) stay searchable and
deep-diveable, they just don't appear in a sector table.

**`research/ResearchMath`** — every number the product depends on, as pure functions: the
market-cap-weighted sector P/E, the badge thresholds, the reality check and gauge position, the
own-5Y-P/E approximation, the revenue CAGR, and the earnings-reaction bisect. Kept pure so
`ResearchMathTest` can pin the exact formulas and `ReferenceParityTest` can replay recorded
payloads through them.

**`data/net/YahooResearch`** — the only place that talks to the network. Charts need no auth;
`quoteSummary` and the batch quote endpoint need a cookie + crumb pair, fetched once and
refreshed lazily on a 401/403. Index symbols (`^NSEI`, `^CNXIT`) pass through untouched — they
must never get `.NS` appended.

**`data/net/YahooParsers`** — separated from the fetching so a field rename by Yahoo fails
`YahooParserTest` rather than silently blanking a screen. The parsers are defensive: a missing
close is skipped, never coerced to zero, and a malformed body returns null.

**`data/db/StockerDb`** — the news/watchlist schema from v1.5 (articles, links, events, quotes,
chart/ai caches, source health) plus a `research_cache` table for the research payloads. The two
halves are independent: news is the v1.5 pipeline, research is the newer macro surface.

**`research/ResearchRepository`** — TTL policy and payload assembly. Every fetch goes through
`cached(key, ttl)`: memory first, then SQLite inside the TTL, then the network, then any stale
copy at all. That last step is why an outage degrades to old numbers instead of an empty screen.
`macro()` never throws; it returns a snapshot carrying an explanatory note.

## Data flow, one screen

Opening a sector:

1. The macro card is already in memory, so navigation is instant.
2. `sector(id)` fetches the index sparkline, then one **batched** quote call for every
   constituent and peer, instead of one request per symbol.
3. Each row additionally needs its own 5Y P/E, which needs EPS (24h TTL) and a 5Y close series
   (7h TTL) — both cached hard, so the second visit is a SQLite read.
4. Rows are sorted by market cap server-side; the client re-sorts per the filter state.

## Computations

Ported verbatim from the reference implementation and pinned by tests:

- **Sector P/E** = Σ(pe × mcap) / Σ(mcap) over constituents carrying both values.
- **Badge** = ≥1.12× baseline → overheated, ≤0.92× → discounted, else consolidating.
- **Reality check** = own 0.92× → discounted; sector 1.25× → priced for perfection; sector
  0.9× → discounted; else fair value.
- **Gauge** = `clamp((pe − 0.55×avg) / (1.30×avg), 0, 1)` — the 0.55×–1.85× span.
- **Own 5Y P/E** = mean of each fiscal year's positive EPS against that calendar year's last
  close, needing ≥2 valid years.
- **5Y CAGR** = `((vₙ/v₀)^(1/years) − 1) × 100` over oldest→newest annual revenue.
- **Earnings reaction** = bisect to the next trading day, then close[i+3] vs close[i].

## Presentation

**Theme** — `Ink` holds the closed palette; `Metal` holds the brushes for the single 118° beam.
`Modifier.metal()` draws a raised top-lit plate (base, light falloff, raking sheen, bright upper
bevel, fall-away shadow) and is the only way to make a surface. `Modifier.glass()` builds
translucency from layers rather than a real backdrop blur, because Compose has no backdrop-filter
and blurring a panel's own content would defeat the point.

**Motion** — `Motion` carries the shipped easings and durations. Components take their spec from
`LocalReducedMotion`, which is read from the OS animation scale, so reduced motion is obeyed
rather than assumed. Every animated transform is transform/opacity only; the segmented control's
pill uses `offset`, not animated padding.

**Screens** — each screen is split into a stateful wrapper (owns fetching) and a stateless
`*Content` composable. That split exists so `ScreenRenderTest` can render the real layout with
fixture data on a JVM, which is how this app is visually verified without a device.

## Settings

`settings/AppSettings` is the single source of truth for appearance and behaviour, persisted under
`app-settings`. Every field is read by the theme or by a screen — there are no decorative toggles,
which `SettingsTest` enforces by asserting the derived tokens actually change.

The palette is state-backed (`Ink`, `Metal`), so choosing a tone repaints the whole tree without
any screen knowing about it. Shapes and spacing travel through `LocalShape` / `LocalSpacing`, so a
radius change reaches every plate and pill.

## Verification

`./gradlew testDebugUnitTest` runs 259 tests and writes PNGs to `app/build/screenshots/`:

| Suite | What it protects |
|---|---|
| `ResearchMathTest` | Every threshold and formula, including boundary values |
| `ReferenceParityTest` | Recomputed numbers agree with recorded reference payloads |
| `CuratedDataTest` | The shipped dataset matches the documented table |
| `SectorFilterTest` | Filter semantics, including "nulls never pass" |
| `FormattingTest` | Direction glyphs, em dashes, Indian lakh grouping |
| `YahooParserTest` | Yahoo field names, against recorded responses |
| `ResearchCacheTest` | TTL serving, stale-serve, disk persistence |
| `ComposeRenderTest` / `LayoutHarnessTest` | Charts survive empty, single-point and flat series |
| `ScreenRenderTest` / `ScreenshotTest` | The three real screens draw correctly |
| `ReducedMotionTest` | Nothing disappears and no ticker rolls under reduced motion |
| `InteractionTest` | Chips move the selection and stay tappable |
| `SettingsTest` | Defaults, persistence, clamping, and that every appearance token really changes |
| `LiveDataTest` | Opt-in (`-Dstocker.live=true`); skips itself when Yahoo throttles |

## Deliberate omissions

- No DI framework, no Room, no OkHttp, no annotation processing. The app is small enough that a
  hand-written repository and `HttpURLConnection` are less machinery, not more.
- No portfolio, orders or notifications. The watchlist, news feed, reader and AI summary from
  v1.5 are retained and restyled — the web dashboard's "no watchlist" rule was written for that
  product, not this one.
- No offline-first sync: the caches exist to survive an outage and to make the second visit fast,
  not to be a database of record.
