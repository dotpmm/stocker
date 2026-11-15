# Coffee-Can Research Dashboard — Functionality Specification & Handoff

**Version:** 1.0 · 2026-11-15
**Purpose:** Complete, implementation-level description of the working web dashboard so the
Stocker Android app can be redesigned around the same functionality. Everything here is
shipped, deployed, and verified — this is a description of real behaviour, not aspirations.
**Design language:** see [`design.md`](design.md) in this repository (Monochrome Exchange +
§4a Metal Finish). `design.md` is the controlling visual spec and **supersedes** the Obsidian
tokens in `docs/design-directions.md` where they conflict (the two agree on: monochrome-only
surface, direction-only red/green, tabular numerals, draw-in motion).

---

## 1. What the product does

A macro-first Indian equities research surface ("Coffee Can" style):

1. **Macro screen** — NIFTY 50 hero + six curated macro sectors as metal cards with a live
   market-cap-weighted P/E vs a 5Y historical baseline, a verdict badge
   (Overheated / Discounted / Consolidating), a 1Y index sparkline, and a
   **sector interdependence flow graph** below the cards.
2. **Sector drill-down** — a table of **every mapped-universe stock** in the sector
   (curated constituents first, then the rest of the Nifty-200 peers), refined by an
   editable, **persisted** filter sidebar.
3. **Deep-dive** — slide-out panel per stock: verdict banner ("reality check" gauge),
   curated narrative, fundamentals, a noise-filter price chart with **5D/1M/6M/1Y chips**
   and **earnings-date dots**, and a **Google Finance** deep link.
4. **Search** — the full NSE Nifty-200 universe by ticker or company name; any hit opens the
   deep-dive, curated or not.

The philosophy throughout: **the economic cycle first, stocks last; the valuation gauge and
the 5Y revenue CAGR are the noise filter.** A stock is researched in place — there is no
watchlist, no collecting, no save-to-watch surface anywhere in the product (this is a
deliberate design rule, see `design.md` §12 rule 17).

---

## 2. Data model

### 2.1 Curated layer (static, in code — `web/dashboard/data.py`)

Six macro sectors; order matters (card order):

| sid | Name | Yahoo index (1Y sparkline) | `pe_5y_avg` baseline | Curated cos |
|---|---|---|---|---|
| `fmcg` | FMCG | `^CNXFMCG` | 38.0 | 8 |
| `it` | Information Technology | `^CNXIT` | 26.0 | 8 |
| `power` | Power & Capital Goods | `^CNXINFRA` | 30.0 | 10 |
| `retail` | Retail & Discretionary | `^CNXCONSUM` | 42.0 | 8 |
| `banking` | Banking & Financials | `^NSEBANK` | 16.0 | 8 |
| `pharma` | Pharma & Healthcare | `^CNXPHARMA` | 30.0 | 8 |

Each curated company carries:
- `symbol` (NSE ticker), `name`, `moat` ∈ {Brand Trust, Cost Advantage, Network Effect,
  Switching Cost, Regulatory Barrier, Scale, None}
- flags: `monopoly` (monopoly/duopoly economics), `cultural` (cultural moat),
  `defensive` (defensive anchor) — together these define **"Coffee-Can Core"**
- optional `note` (e.g. RELIANCE: "Conglomerate proxy — the listed way to own Reliance
  Retail's organized-transition story.")

Each sector carries an editorial `blurb` (one to two sentences of thesis + risk, shown in
the drill-down header) and `pe_5y_avg` — an **editable, documented estimate** (free
historical sector P/E doesn't exist). Curated names also get hand-written narratives where
available (`NARRATIVES` dict); everything else falls back to a template:

> "{name} operates in {sector} with a {moat} of moat type '{moat}'.{flags} Its Coffee-Can
> merit rests on whether that moat compounds revenue above inflation — check the 5Y revenue
> CAGR and the valuation gauge before believing it."

`{flags}` appends sentences for monopoly / cultural / defensive as applicable.

### 2.2 Universe layer (`~/.trading_platform/nifty200.json`, 200 stocks)

Fields per stock: `symbol`, `company`, `industry` (NSE classification).
`INDUSTRY_MAP` folds NSE industries into the six macro sectors:

- Financial Services → banking; Information Technology, Telecommunication → it
- Power, Capital Goods, Oil Gas & Consumable Fuels, Construction, Construction Materials → power
- Fast Moving Consumer Goods → fmcg
- Consumer Services, Consumer Durables, Automobile and Auto Components, Textiles → retail
- Healthcare → pharma

Industries **not** in this map (Metals, Realty, Chemicals, Services, …) are still
searchable and deep-diveable — they simply don't sit in a sector table.

For uncurated deep-dives, Yahoo's own sector label picks a gauge baseline from
`YF_SECTOR_PE` (Financial Services 16, Technology 26, Healthcare 30, Consumer Defensive 38,
Consumer Cyclical / Durables / Services 42, Energy 12, Utilities 20, Industrials 30,
Basic Materials 25, Real Estate 40, Communication Services 30, Automobile 24; default 25).

### 2.3 Interdependence graph (static weights, rendered from data)

Edges are `provider → dependent` ("whose output feeds whose earnings"), weight 0–1:

| from | to | w | plain-language note |
|---|---|---|---|
| banking | power | 0.8 | project & capex finance |
| banking | retail | 0.7 | consumer credit |
| it | banking | 0.6 | core banking software & services |
| retail | fmcg | 0.5 | shared consumption cycle |
| banking | fmcg | 0.4 | channel & working-capital finance |
| retail | pharma | 0.4 | the chemist channel |
| it | retail | 0.3 | e-commerce & digital rails |
| power | fmcg | 0.2 | rural electrification → demand |

---

## 3. Screens — exact behaviour

### 3.1 Screen 1 · Macro

**Hero.** `NIFTY 50 · NSE · <as_of>` micro-label; huge tabular numeral (last close of
`^NSEI` 1Y daily series) with a vertical white gradient fill; a ▲/▼ badge with
"(chg)% today" — today's change is computed **within the series** (last vs previous point);
an inline 210×42 sparkline of the last 60 daily closes.

**Sector cards.** Grid `repeat(auto-fill, minmax(300px, 1fr))`, 16px gap. Each card:

- name + mono `n_companies` "CO" count (curated count)
- fundamentals line: `Current <P/E> · 5Y Avg <pe_5y>` + `▲/▼ x.x% 1y` in direction color
  (the 1Y change of the sector's index series)
- foot: verdict pill + 60-point index sparkline (up→green line, down→red line, either way
  with a soft gradient under-fill and a one-shot draw-in)
- verdict badge rules (exact):
  - `pe_current is null` → **Consolidating** (neutral)
  - `pe_current ≥ pe_5y_avg × 1.12` → **Overheated** (red)
  - `pe_current ≤ pe_5y_avg × 0.92` → **Discounted** (green)
  - else → **Consolidating**

`pe_current` is the **market-cap-weighted trailing P/E of the sector's curated
constituents** (`Σ pe×mcap / Σ mcap`, only pairs with both values present), cached 6h.

Card hover: lift 3px, focus border, metal shadow deepens, a light sweep runs across the
face once. Card press: scale 1.045 + 2px lift; all sibling cards recede (opacity .22,
scale .95); then the sector screen **grows out of the clicked card's exact screen
position** (transform-origin = card center as % of viewport) over 340ms. Reduced-motion:
no press phase, instant switch.

**Interdependence graph** (its own panel, always the last block of the macro screen):

- 600×400 viewBox, hand-laid node coordinates (readability over auto-layout):
  fmcg(135,100) retail(300,60) pharma(470,100) power(110,300) banking(300,345) it(490,300)
- node = 41px-radius circle, `surface-2` fill, ring colored by that sector's badge
  (overheated red / discounted green / consolidating white 30%), showing short name
  (FMCG, IT, POWER, RETAIL, BANKS, PHARMA), live `P/E x.x`, and `▲/▼ x.x%` 1y in
  direction color
- edge = quadratic Bézier (per-edge perpendicular bend, values in the layout table), two
  strokes: dim base pass (width `1 + w×6`) + brighter flow pass; both draw in once via
  `stroke-dashoffset` on load; each edge carries a `<title>` tooltip
  ("{from} feeds {to} · {note} · strength {w×100}%")
- **hover isolates**: all edges not touching the hovered node drop to 10% opacity
- **click** drills into the sector with the same press → recede → grow-out transition

### 3.2 Screen 2 · Sector drill-down

**Header:** "← Back to Macro" pill; micro-label `NSE · {n_universe} companies
({n_curated} curated) · P/E 5Y baseline {pe_5y_avg}`; sector name; editorial blurb; and a
metal panel with a canvas sparkline of the sector index (120px tall, glow line, gradient
under-fill, draw-in 1100ms, last close labeled at the endpoint, endpoint dot).

**Table** (all stocks of the sector — curated first by construction, then mapped universe
peers, initially sorted mktcap desc server-side; client re-sorts per filter state):

| column | content |
|---|---|
| Ticker | mono symbol + muted company name underneath + note if present |
| Mkt Cap (₹Cr) | `mcap / 1e7` rounded to 0 dp; "—" if missing |
| Trailing P/E | value in green when below own 5Y avg; muted "5Y x.x" sub-line |
| 5Y Rev CAGR | `x.x%` colored by direction |
| Moat Type | moat pill (highlighted when ≠ None) + Monopoly/Cultural/Defensive pills |

Every row opens the deep-dive. Rows stagger in (40ms base, 14ms step).

**Filter sidebar** (sticky, 250px):

| control | type | semantics | default |
|---|---|---|---|
| Coffee-Can Core | toggle | row is monopoly OR cultural OR defensive | **ON** |
| Monopolies / Duopolies | toggle | `monopoly == true` | off |
| Has Cultural Moat | toggle | `cultural == true` | off |
| Defensive Anchor | toggle | `defensive == true` | off |
| P/E Below Own 5Y Avg | toggle | `pe_below_own_avg == true` | off |
| P/E Below 20 | toggle | `pe < 20` | off |
| Rev CAGR Above 15% | toggle | `rev_cagr_5y > 15` | off |
| Large Cap · ₹50k Cr+ | toggle | `mcap_cr ≥ 50000` | off |
| Moat Type | select | exact moat match ("Any moat" = no constraint) | Any |
| Sort By | select | mktcap ↓ (default) / P/E ↑ / CAGR ↓ / Ticker A–Z | mcap |

- Toggles group under micro-headers **Structure / Valuation / Growth & Size**, then Moat
  Type and Sort By selects.
- Filtering is instant, client-side, with directional row exit (opacity+collapse) — no
  spinners, no reload. A large counter shows **"N survive"** with a reset pill.
- **The entire filter state persists in localStorage under key `cc-filters`** and restores
  on next visit (missing keys merge over defaults). Reset restores the shipped defaults
  (Core only, sort mktcap) and confirms with a toast.
- Missing data never silently passes a filter: P/E filters treat null as "worst"
  (`pe` sort asc → 1e9; CAGR/large-cap comparisons → −1e9/0), so nulls sort away.

### 3.3 Screen 3 · Stock deep-dive (slide-out)

Fixed right panel, `min(620px, 100vw)`, over a 45% scrim; z-order: header 100 > panel 90 >
scrim 80 — the panel slides under the sticky header. Close via ✕, scrim tap, or Escape.
The panel never lingers across tab switches.

Content, top to bottom:

1. **Micro-label**: `{sector}` + ` · uncurated` when generic + ` · {note}` if present.
   Display name (`Space Grotesk` 26px).
2. **Verdict banner ("Reality check")** — full-width tinted strip (green/white/red by
   band) with the verdict word and `P/E {pe} vs sector {sector_avg}`.
   Exact rules (`_reality_check`):
   - no P/E → `NO DATA`, neutral band, gauge parked at 50%
   - `pe < own_avg × 0.92` (own avg known) → **DISCOUNTED** (under band)
   - `pe ≥ sector_avg × 1.25` → **PRICED FOR PERFECTION** (over band)
   - `pe ≤ sector_avg × 0.9` → **DISCOUNTED** (under band)
   - else → **FAIR VALUE** (mid band, white/grey — no yellow anywhere)
3. **The Narrative** — curated hand-written text, template fallback, or the generic
   uncurated text: "{name} is an NSE-listed {sector} business outside the curated
   Coffee-Can set. Judge it on the gauge and the 5Y revenue CAGR below — the noise filter
   still applies."
4. **The Valuation Gauge** — 200×118 semi-circle speedometer: three 1/3 arcs
   (green UNDER / white-grey fair / red PERFECTION), 7 tick marks, needle from −90°
   rotating to `−90 + pct × 180` where
   `pct = clamp((pe − 0.55×sector_avg) / (1.85×sector_avg − 0.55×sector_avg), 0, 1)`.
   Needle animates into place 420ms after the panel opens.
5. **Fundamentals** — key-value grid: Price (`₹ltp` + ▲/▼ x.xx%), Market Cap (₹Cr),
   Trailing P/E, 5Y Avg P/E (own; shows "sector {x}" when own unknown), Sector Avg P/E,
   5Y Rev CAGR.
6. **The Noise Filter chart** with **range chips 5D / 1M / 6M / 1Y** (default 1Y):
   iOS-style segmented control, one `surface-3` pill slides between labels (320ms);
   switching fetches `/stock/{sym}/history?range=` and re-runs the full chart choreography
   (line draw-in 1100ms → gradient area fade at 780ms → earnings dots pop from 1000ms,
   +90ms each).
   - Chart = close-line with soft glow (`drop-shadow`), gradient under-fill, 5 hairline
     gridlines, hi/lo axis labels, last value labeled at the endpoint, 3 x-axis date labels.
   - **Earnings dots**: black circles with red ring at each earnings date's price point;
     hover tooltip: "Earnings missed by x.x%" / "Earnings beat by x.x%" (surprise ≥ 0 =
     beat) + " — price dropped x.x%" / "rose x.x%" when a realized reaction exists
     (close 3 trading days after vs on the date, next-trading-day start, bisect match);
     plain "Earnings date" when surprise is unknown.
7. **Action bar** — centered pill button **"Open in Google Finance ↗"** →
   `https://www.google.com/finance/quote/{SYMBOL}:NSE` (new tab).

### 3.4 Search (header command box)

- Always-visible pill: "⌕ Ask for a company…", `/` keyboard shortcut focuses it from
  anywhere (the `/` keypress is swallowed).
- Clicking spawns an editable input in place; typing queries `/search?q=` (debounce not
  required; results ≤ 12) → dropdown of `SYMBOL — name · sector` rows.
- Curated matches first, then universe matches (max 12). Sector label for unmapped
  industries is the raw NSE industry name.
- Enter opens the first hit; any hit opens the deep-dive **over whatever screen is
  active**; Escape blurs + closes.
- **Burst-only aura (signature moment, strictly bounded):** on focus/press/hover/typing
  (and one intro burst 900ms after load) a dot-field aura shimmers around the pill for
  **exactly ~1.05s** (sine envelope in/out) with ≤3 concurrent 4-point sparkles, then is
  completely removed — no loop, no residual glow at rest. This is a hard rule
  (`design.md` §12 rule 13).

---

## 4. API contract (deployed reference: `http://192.168.29.21:8877/dashboard/api`)

FastAPI router, prefix `/dashboard/api`, all GET, all JSON. Read-only.

| endpoint | returns | notes |
|---|---|---|
| `/health` | `{ok:true, service:"coffee-can-dashboard"}` | liveness |
| `/sectors` | `{hero:{value,chg_pct,sparkline[≤60]}, sectors[6], dependencies[8], as_of}` | card payload, §3.1 |
| `/sector/{sid}` | `{id,name,pe_5y_avg,blurb,sparkline[1y],companies[],n_curated,n_universe,as_of}` | §3.2; 404 unknown sid |
| `/stock/{symbol}` | deep-dive payload (§3.3) for **any** NSE symbol | 404 only on hard failure; generic sector derivation otherwise |
| `/stock/{symbol}/history?range=5d\|1m\|6m\|1y` | `{symbol,range,price_history[],earnings_dots[]}` | §3.3 chip refetch |
| `/search?q=` | `{results[≤12]{symbol,name,sector,sector_id?}}` | universe-wide |

Deep-dive payload shape:

```json
{ "symbol": "TITAN", "name": "Titan Company", "sector": "Retail & Discretionary",
  "sector_id": "retail", "curated": true, "note": "",
  "quote": {"ltp": 3412.5, "chg_pct": 0.84},
  "pe": 38.2, "pe_5y_own": 44.1, "pe_sector_avg": 42.0, "mcap_cr": 302000,
  "rev_cagr_5y": 21.4, "moat": "Brand Trust", "monopoly": false,
  "cultural": true, "defensive": false, "narrative": "…",
  "reality": {"verdict": "FAIR VALUE", "band": "mid", "pct": 0.47},
  "price_history": [{"t": "2025-11-15", "c": 3120.0}, …],
  "earnings_dots": [{"date": "2025-10-30", "surprise": -4.2, "drop_pct": -2.1}],
  "sparkline": [last 60 pts of 1y], "as_of": "…" }
```

`price_history` points are `{"t": "YYYY-MM-DD"}` for daily ranges and
`{"t": "YYYY-MM-DD HH:MM"}` for 5D (15-minute bars) — the client detects intraday by the
space in `t`.

**Caching TTLs** (process memo + on-disk JSON; on fetch failure a stale disk copy is
served — the UI keeps working through Yahoo outages):

| key | ttl | key | ttl |
|---|---|---|---|
| quote `q:{sym}` | 120s | index history `idx:{sym}` | 12h |
| stock info `info:{sym}` | 12h | stock 1y history | 6h |
| financials `fin:{sym}` | 24h | stock 5y history | 7h |
| earnings `earn:{sym}` | 24h | history 5d / 1m / 6m / 1y | 15m / 30m / 90m / 6h |
| sector weighted P/E | 6h | universe list | 24h |

**Cold start:** first load of a sector fetches `info` + `income_stmt` per company in a
thread pool (8 workers) — the biggest sector (banking, 49 companies) takes ~30s cold;
afterwards it is ~0.1s. All six sector caches can be pre-warmed by GETting each
`/sector/{sid}` once.

---

## 5. Exact computations (port these, don't re-derive)

- **Sector current P/E** = mcap-weighted mean of curated constituents' trailing P/E.
- **Verdict badge** = thresholds in §3.1 (1.12× / 0.92× of `pe_5y_avg`).
- **Reality check** = thresholds in §3.3 (own 0.92×, sector 1.25× / 0.9×; gauge span
  0.55×–1.85× of sector avg).
- **Own 5Y avg P/E** (approximation, documented as such): from the income statement take
  4 fiscal years of diluted (else basic) EPS; for each year with EPS > 0 pair the
  **last close of that calendar year** from the 5Y daily series; P/E set = those ratios;
  need ≥ 2 valid years, else `null` (UI falls back to the sector baseline and the reality
  check uses sector thresholds only).
- **5Y revenue CAGR**: last vs oldest of the available annual "Total Revenue" (fallback
  "Operating Revenue") values, oldest→newest, `((vₙ/v₀)^(1/years) − 1) × 100`, `years ≥ 2`
  and `v₀ > 0`; EPS history kept for future use.
- **1y change (cards, nodes)** = last vs first close of the 1Y index series.
- **Today change (hero, quotes)** = last vs previous close (`fast_info.previous_close`
  when available).
- **Earnings dot drop_pct**: on the sorted series of chart dates, `i0 = bisect_left(dates,
  earnings_date)`; if `i0 < n and i0+3 < n`: `(close[i0+3] / close[i0] − 1) × 100`. Dates
  often fall on non-trading days — the bisect naturally starts at the next trading day.
- **Surprise column**: Yahoo's earnings-dates frame spells the column `Surprise(%)` —
  match any column whose name *contains* "urprise"; NaN → null.
- **History wire format**: downsample every series to ≤ 140 points (`out[::max(1,len//140)]`).
- **Yahoo symbol rule**: stocks get `.NS` appended; **index symbols (`^NSEI`, `^CNXIT`,
  …) are raw and must never go through the `.NS`-appending helper.** Tz-aware history rows
  are made naive with `replace(tzinfo=None)`, not `tz_localize(None)`.

---

## 6. Motion & interaction spec (durations are shipped values)

- **Easings:** `--ease-fluid: cubic-bezier(.32,.72,0,1)`; `--ease-emerge:
  cubic-bezier(.22,1,.36,1)`; durations 240 / 420 / 1100ms.
- **Entrances:** header fade-down 700ms; hero row fade-up 800ms @120ms; sector cards
  stagger 90ms base / 70ms step; table rows 40/14; stat plates 50/45; graph nodes 120/60.
- **Scroll:** header condenses past 8px, hides past 420px of downward scroll, returns on
  any upward scroll; macro data auto-refreshes every 10 min while visible.
- **Zoom transition (card → sector):** press feedback 210ms (card grows 4.5%, siblings
  recede) → sector screen scales in from the card's viewport position, 340ms.
- **Charts:** SVG spark draw-in 900ms; canvas charts 1100–1200ms with eased point count
  (`1 − (1−k)³`), under-fill fades in at 55% progress, endpoint dot + label on completion;
  gauge needle at +420ms; hero numeral counts up 900ms ease-out-cubic.
- **Burst aura:** §3.4 — 1050ms, sine envelope, then gone.
- **Reduced motion:** every animation above collapses to instant state (cards/rows shown,
  charts fully drawn, needle parked, no burst, no cursor glow) — a full implementation,
  not partial.

---

## 7. Loading, error & empty states

- **Loading:** skeleton blocks with a travelling light sweep (1.4s loop); skeleton table
  row 200px tall; deep-dive shows two skeleton blocks. Spinners are banned everywhere.
- **Macro fetch failure:** inline error card in the grid with the message + "the cache
  will serve stale data meanwhile" note. Search failures are silent (never block typing).
- **Deep-dive load failure:** error banner inside the panel; panel still closes normally.
- **Empty data:** "—" for any missing number (never 0); "no price history available"
  message in the chart box.
- **Race guards:** switching screens mid-fetch discards the stale sector render; opening
  another stock mid-fetch discards the first deep-dive payload.

---

## 8. Handoff notes for the Stocker redesign

### 8.1 Existing Stocker (what's already there, as of v1.5)

- Kotlin 2.2, **Jetpack Compose** (Material 3 under the hood), minSdk 26 / target 36,
  AGP 8.11, deliberately few dependencies (no Room, no OkHttp, no DI): plain SQLite
  (`StockerDb`), platform HTTP (`data/net/Http.kt`), WorkManager (`RefreshWorker`).
- Screens today: Watchlist, News/Events, StockDetail, Reader, AiSummary, AddStock,
  Settings — string-keyed navigation in `ui/StockerRoot.kt`; floating nav bar component.
- Reusable for this redesign: **`data/net/Yahoo.kt`** already talks to the (unofficial)
  Yahoo endpoint for symbol search and chart data with ranges/intervals — the same host
  serves everything §5 needs (`quote` via `fast_info`, `.info` for P/E/mcap/sector,
  `income_stmt` for EPS/revenue, `earnings_dates` for dots); `ui/theme/*` owns colors,
  type, motion; `ui/components/Pressable.kt` implements the global press feedback.
- The existing product is news-centric (feed pipeline, entity linking, sentiment). The
  Coffee-Can functionality above is **orthogonal and additive**: it can coexist as a new
  top-level area (e.g. a "Research"/"Markets" tab beside News) or absorb the stock-detail
  screen. That IA decision belongs to the app agent.

### 8.2 Design

- Follow `design.md` end-to-end: monochrome + muted `#5fa87a`/`#d3705f` direction pair
  only, metal §4a surfaces, burst-only search aura, draw-in choreography, reduced-motion
  completeness, hard rules §12 (14–17 included).
- Mobile adaptations of web-only behaviours (decided; not open):
  - **Cursor glow** → drop it. Touch has no hover; the ambient beam in `.ambient` carries
    the atmosphere. Do not substitute a tap-following glow.
  - **Hover isolates (graph)** → tap-and-hold or a two-tap "focus mode" toggle on a node;
    edge `<title>` notes become a tap sheet.
  - **Filter sidebar** → bottom sheet or collapsible section above the table; the
    persisted-state contract (§3.2) is unchanged (SharedPreferences/DataStore instead of
    localStorage).
  - **`/` shortcut & pill-input search** → a plain search field with the same dropdown,
    result shape, deep-dive hand-off, and the **same strictly-bounded burst aura**.
  - **Slide-out deep-dive** → bottom sheet (≥ 90% height) or full-screen destination;
    keep the scrim, z-order discipline, and Escape-equivalent (back gesture).
- The gauge, the verdict thresholds, the filter semantics, the interdependence graph data,
  and all copy in §2–§5 are **product logic** — port them verbatim; they are not styling
  choices.

### 8.3 Data strategy (pick one; both are valid)

1. **On-device (fits Stocker's architecture).** Port §5 computations to Kotlin over the
   Yahoo endpoints Stocker already uses; ship `data.py`'s curated dataset (sectors, cos,
   flags, narratives, `pe_5y_avg`, dependency graph) as a versioned JSON asset in the APK;
   ship the Nifty-200 list as an asset (refresh it opportunistically). TTL-cache in
   SQLite like `chart_cache` already does, with stale-serve-on-failure semantics (§4).
2. **LAN API.** Consume the reference API in §4 directly for phone-at-home use. Only
   viable on the homelab network; not a distribution path.

Recommended: (1), keeping (2) as a debug facility. The reference implementation to port
from lives at `user@192.168.29.21:~/india-trade-cli/web/dashboard/`
(`data.py` = curated layer, `engine.py` = fetch/compute layer, `routes.py` = API shapes,
`static/app.js` = all client behaviour incl. exact timings).

### 8.4 Suggested build order

1. Theme re-skin to `design.md` (tokens, metal surfaces, motion curves) — mechanical.
2. Macro screen: hero + sector cards + badges (needs: quotes, `.info` P/E, index series).
3. Deep-dive: reality check + gauge + fundamentals + noise-filter chart + range chips +
   earnings dots + Google Finance link.
4. Universe + search (asset list + Yahoo search already exists).
5. Sector drill-down + persisted filters.
6. Interdependence graph (static layout + live node stats).
7. States & motion polish pass against §6–§7 and the `design.md` §14 checklist.

### 8.5 Explicit non-goals

- **No watchlist surface** — no save-to-watch, no kanban, no collection state
  (`design.md` §12 rule 17). Research happens in place.
- No notifications, no orders, no portfolio, no AI summary requirement for this surface
  (Stocker's existing news/AI stack is untouched by this redesign).
