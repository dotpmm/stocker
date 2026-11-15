# MONOCHROME EXCHANGE — Web Design Style Guide

**Product:** Stock market research app (NSE, India) — sector explorer → company list → company analytics.
**Audience for this file:** AI coding agents and human implementers. Follow every rule in
§12 (Hard Rules) exactly; everything else is the system you build within.

This style adapts **Google's "Neural Expressive"** design language (Gemini, I/O 2026) to a
monochrome financial terminal: fluid directional motion, gradients as *energy, not decoration*,
softness over sharpness, and visible "thinking" states. The palette is stripped to
**black / white / gray + red (loss) / green (gain)** — nothing else.

---

## 1. Design Philosophy (read first)

1. **Monochrome is the identity, color is the signal.** 95% of the UI is black, white, and
   gray. Red and green appear *only* to encode market direction (up/down, gain/loss). If a
   red or green pixel doesn't mean "price moved", it's a bug.
2. **Gradients are directional pointers.** Per Gemini's design system: a gradient has a
   *sharp, near-opaque leading edge* that *diffuses at the tail*. Gradients must point the
   eye toward what matters (the latest price, the largest slice, the next action). Never use
   a gradient as a flat color substitute.
3. **Motion is instructional, not decorative.** Every animation has a defined start and end
   point and mirrors a user action or a system process (loading, computing, streaming).
   Motion should answer: *what changed, where did it come from, what is still working?*
4. **Inner activity = intelligence.** When the app is computing (fetching quotes, screening
   the sector), the UI shows visible "thinking" — pulsing glow, orbiting shimmer, breathing
   gradient — so the wait feels like analysis, not lag.
5. **Blur creates depth, not vagueness.** Backdrop blur layers content behind glass; noise
   grain keeps large gradient surfaces from feeling plastic. Blur is a depth cue, noise is a
   texture — both are restrained.
6. **Softness everywhere.** Large radii, pill shapes, glow falloffs, no harsh 1px hard
   borders where a translucent border + inner glow can do the job. The interface should feel
   like light on glass in a dark terminal.
7. **Surfaces are metal, lit from one place.** The app lives in a brushed-charcoal room
   with a single 118° beam entering from the top-left (§4a). Raised panels are top-lit
   plates with a bright upper bevel — depth comes from the light's direction, never from
   hard borders. Like everything else: one light source, no chroma added.

---

## 2. Color System

### 2.1 Tokens (CSS custom properties)

```css
:root {
  /* ── Monochrome base (dark-first; app is dark by default) ── */
  --bg-void:        #000000;   /* page background, true black */
  --bg-base:        #0a0a0a;   /* app shell */
  --surface-1:      #111112;   /* cards, panels */
  --surface-2:      #18181b;   /* elevated cards, modals */
  --surface-3:      #212124;   /* hover-elevated, active chips */

  --text-primary:   #fafafa;   /* headlines, prices */
  --text-secondary: #a1a1aa;   /* labels, axis text */
  --text-muted:     #52525b;   /* captions, disabled */
  --text-ghost:     #27272a;   /* watermark numerals, grid labels */

  --border-subtle:  rgba(255,255,255,0.08);
  --border-focus:   rgba(255,255,255,0.25);
  --glass-bg:       rgba(255,255,255,0.04);
  --glass-bg-str:   rgba(255,255,255,0.08);

  /* ── Directional (the ONLY chromatic tokens) ──
     MUTED money colors — desaturated sage green / clay salmon, in the register of
     Google Finance dark mode and Linear. Saturated hues (#22c55e, #ef4444, …)
     are BANNED: they glow like default Tailwind and break the terminal mood. */
  --up:             #5fa87a;   /* muted sage — chart line, borders-on-gain   */
  --up-text:        #7cbf93;   /* +10% lightness — Δ text for legibility     */
  --up-glow:        rgba(95,168,122,0.28);
  --up-dim:         rgba(95,168,122,0.10);
  --down:           #d3705f;   /* muted clay/salmon — chart line             */
  --down-text:      #e08a7c;
  --down-glow:      rgba(211,112,95,0.28);
  --down-dim:       rgba(211,112,95,0.10);
  --flat:           var(--text-muted); /* unchanged price — gray, never color */

  /* ── Gradient stops (derived, never hardcode) ──
     Under-curve fills are QUIET: start at ≤0.22 opacity, never higher. */
  --up-grad-from:   rgba(95,168,122,0.22);
  --up-grad-to:     rgba(95,168,122,0);
  --down-grad-from: rgba(211,112,95,0.22);
  --down-grad-to:   rgba(211,112,95,0);
  --white-sweep-a:  rgba(255,255,255,0.00);
  --white-sweep-b:  rgba(255,255,255,0.10);

  /* ── Shape ── */
  --radius-sm: 8px;  --radius-md: 14px;  --radius-lg: 22px;
  --radius-pill: 999px;
}
```

### 2.2 Rules

- **Muted money colors (rule zero).** Directional hues live around `hsl(140 30% 55%)`
  (up) and `hsl(10 55% 60%)` (down) — desaturated, never neon. Saturated Tailwind
  greens/reds (`#22c55e`, `#ef4444`) and any pure `#0f0`/`#f00` are rejected on sight.
  If a color draws the eye before the number it labels does, it's too bright.
- **Never introduce another hue.** No blue links, no amber warnings. "Warning" is rendered
  as white text + glow, or red if it's risk.
- **Gray communicates neutrality.** Flat price, unselected sector, muted metric = gray.
  Do not gray-wash losses; losses are always red.
- **Opacity over tint.** Depth is expressed with white-at-low-alpha (borders, glass, glow),
  never with gray-green or gray-red mixed colors.
- **Light mode** (optional): invert the monochrome scale (`#fff` base, black text), keep
  `--up`/`--down` identical, reduce all glow alphas by 50% (glow on white reads as blur smudge).

---

## 3. Gradients — the "Perfectly Made" Rulebook

Use exactly these gradient archetypes. Copy the CSS; do not improvise values.

### 3.1 Chart under-curve fill (the signature)
Area below a price line fades from the line's color to transparent — opaque at the leading
edge (near the curve), diffusing at the tail (toward the axis).

```css
/* for an SVG <linearGradient>: */
.area-up   { stop-color: var(--up);   }  /* stop 0: opacity .22 (QUIET, never .45+) */
.area-up-2 { stop-color: var(--up);   }  /* stop 100: opacity 0 */
```

### 3.2 Directional sweep (buttons, active tabs, sector cards)
A near-invisible white sweep travels across a surface on hover —
*sharp leading edge, diffused tail, 135°*:

```css
.sweep {
  background: linear-gradient(135deg,
    var(--white-sweep-a) 0%,
    var(--white-sweep-b) 42%,   /* soft body */
    var(--white-sweep-b) 48%,   /* leading edge: brief plateau = "sharpest point" */
    var(--white-sweep-a) 62%);  /* rapid diffuse */
  background-size: 250% 250%;
  background-position: 100% 100%;
  transition: background-position 600ms var(--ease-fluid);
}
.sweep:hover { background-position: 0% 0%; }
```

### 3.3 Ambient radial glow (hero, active chart, loading states)
```css
.ambient {
  background: radial-gradient(ellipse at 50% 0%,
    rgba(255,255,255,0.06) 0%,
    rgba(255,255,255,0.015) 45%,
    transparent 70%);
}
```

### 3.4 Text gradient (hero numerals only — e.g. NIFTY 50 big price)
```css
.numeral-gradient {
  background: linear-gradient(180deg, var(--text-primary) 30%, rgba(250,250,250,0.35) 100%);
  -webkit-background-clip: text; background-clip: text;
  -webkit-text-fill-color: transparent;
}
```

**Gradient discipline:** max **one** directional sweep + **one** ambient glow per viewport
section. A card with a sweep does not also get a text gradient. When in doubt, remove one.

---

## 4. Blur & Glass

```css
:root {
  --blur-sm: 8px;   /* tooltips, chips          */
  --blur-md: 16px;  /* cards over ambient bg    */
  --blur-lg: 28px;  /* nav bar, modal backdrop  */
  --blur-xl: 44px;  /* "thinking" overlay       */
}
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--blur-md)) saturate(140%);
  -webkit-backdrop-filter: blur(var(--blur-md)) saturate(140%);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow:
    inset 0 1px 0 rgba(255,255,255,0.06),   /* top bevel: glass must have it */
    0 8px 32px rgba(0,0,0,0.45);
}
```

- **Sticky header:** `glass` at `--blur-lg`, hairline border-bottom appears only after scroll.
- **Modals / command palette:** page content dims to 35% black + `--blur-xl` behind the panel.
- **Never blur text you want read.** Blur the *background*, keep foreground at full contrast.
- Blur is expensive — cap at ~3 backdrop-filter surfaces visible at once; rasterize the rest.

---

## 4a. Metal Finish (the "machined aluminum" surface system)

The app sits inside a brushed-charcoal wall lit by one diagonal beam. Panels read as
top-lit metal plates: light catches the upper edge and falls away down the face. This
replaces flat `#000` — depth comes from light direction, not borders.

```css
:root {
  /* body = charcoal wall, light falls from top-left */
  --bg-void:#0c0c0d; --bg-base:#131315;
  --surface-1:#161618; --surface-2:#1d1d20; --surface-3:#26262b;

  /* the light: top edge bright, falls away down the face */
  --metal-lit:linear-gradient(180deg, rgba(255,255,255,.06) 0%,
    rgba(255,255,255,.022) 9%, rgba(255,255,255,.006) 20%, transparent 34%);
  --metal-rake:linear-gradient(115deg, rgba(255,255,255,.028) 0%, transparent 38%);
  --metal-edge:inset 0 1px 0 rgba(255,255,255,.10), inset 0 0 0 1px rgba(255,255,255,.015);
  --metal-shadow:0 1px 2px rgba(0,0,0,.5), 0 18px 44px rgba(0,0,0,.38);
}
body {
  background: linear-gradient(118deg, #19191c 0%, #121214 30%, #0e0e10 58%, #131315 100%);
  background-attachment: fixed;
}
.ambient {   /* one wide 118° beam + a bloom where it lands */
  background:
    linear-gradient(118deg, rgba(255,255,255,.075) 0%, rgba(255,255,255,.028) 18%,
      rgba(255,255,255,.008) 34%, transparent 52%),
    radial-gradient(ellipse 55% 42% at 12% -8%, rgba(255,255,255,.10) 0%,
      rgba(255,255,255,.025) 42%, transparent 72%);
}
.panel, .scard, .stat, .sec-spark, .chart-panel-deep, .deep {
  background: var(--surface-1);
  background-image: var(--metal-lit), var(--metal-rake);
  box-shadow: var(--metal-edge), var(--metal-shadow);
}
```

- **Exactly one beam.** 118°, entering top-left, gone by ~52% across. Never a second
  light source; never a vertical-only wash — the diagonal is what reads as "metal".
- **Every raised surface is top-lit** with `--metal-lit` + `--metal-rake` over its base
  color, and carries `--metal-edge` (bright 1px top bevel) + `--metal-shadow`. A flat
  `surface-1` block without the light gradient is rejected.
- **Header glass darkens, not lightens:** `rgba(12,12,13,.58)` + `--blur-lg` so the beam
  passes behind it, with a faint top bevel of its own.
- **Hover lifts light with the card:** raise `--metal-shadow`'s second layer
  (e.g. `0 22px 52px rgba(0,0,0,.46)`) — the plate rises toward the beam.
- **Grain still applies** (§5, ≤0.07) on top of metal surfaces; metal without grain bands.
- Directional color (§9.4) is unchanged — the red/green verdict accents sit on the metal
  exactly as before, and remain the only chroma on the page.

---

## 5. Noise ("Noisy Blur")

All large gradient/ambient surfaces get a film-grain overlay so gradients don't band.

```css
/* 8×8 repeating SVG turbulence, inlined once, referenced everywhere */
.grain::after {
  content: ""; position: absolute; inset: 0; pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='120' height='120' filter='url(%23n)' opacity='0.5'/%3E%3C/svg%3E");
  opacity: 0.05;              /* 0.04–0.07 range */
  mix-blend-mode: overlay;
}
```

- Apply `.grain` to: hero background, chart panels, sector-card ambient glows, modal scrims.
- **Never** apply grain to body text, table rows, or sparklines — it kills legibility.
- Noise opacity stays ≤ 0.07. If you can consciously "see" the noise, it's too strong.

---

## 6. Glow & Underlight

```css
:root {
  --glow-up:    0 0 16px var(--up-glow),   0 0 48px rgba(95,168,122,0.10);
  --glow-down:  0 0 16px var(--down-glow), 0 0 48px rgba(211,112,95,0.10);
  --glow-white: 0 0 20px rgba(255,255,255,0.20), 0 0 60px rgba(255,255,255,0.08);
}
```

- **Chart line underglow** (required): the price line carries a drop-shadow of its own
  direction color — `filter: drop-shadow(0 0 4px var(--up-glow))` — so the curve appears to
  emit light onto the gradient below it. This is the app's signature visual; see §9.1.
- **Focus rings:** `box-shadow: 0 0 0 1px var(--border-focus), 0 0 24px rgba(255,255,255,0.12)`
  — white glow, never blue.
- **Active sector chip:** white inner glow + sweep; the *selected* state glows, the rest don't.
- Glow is earned by state (selected / live / rising) — ambient inactive elements never glow.

---

## 7. Typography

```css
--font-display: "Space Grotesk", ui-sans-serif, system-ui, sans-serif;
--font-body:    "Inter", ui-sans-serif, system-ui, sans-serif;
--font-mono:    "IBM Plex Mono", ui-monospace, monospace;  /* tickers, prices, tables */
```

| Role | Font | Size / Weight | Notes |
|---|---|---|---|
| Hero index price | mono | clamp(3rem–5rem) / 500 | `numeral-gradient`, tabular-nums, ticker animation |
| Section titles | display | 1.75rem / 600 | tracking-tight, sentence case |
| Tickers (RELIANCE) | mono | 0.8125rem / 600 | uppercase, letter-spacing 0.04em |
| Prices & % | mono | 0.875rem / 500 | **tabular-nums always** (digits must not jitter while animating) |
| Body / descriptions | body | 0.9375rem / 400 | line-height 1.6 |
| Axis / meta | body | 0.6875rem / 500 | `--text-muted`, uppercase micro-labels |

- Uppercase micro-labels (`SECTOR · NIFTY IT · 51 COMPANIES`) at 0.6875rem, muted, are the
  monochrome texture of the app — use them generously on metadata, never on actions.
- Prices never render in a proportional font. Ever.

---

## 8. Motion — Neural Expressive System

The Gemini motion principles, translated: **directional flow** (every animation travels
from a start to an end that mirrors the user's action), **anticipation → release** (fast
approach, soft landing), **inner activity** (visible "thinking" while computing), and
**softness** (springy, never snappy).

### 8.1 Tokens

```css
--ease-fluid:    cubic-bezier(0.32, 0.72, 0, 1);      /* Gemini's fluid easing */
--ease-emerge:   cubic-bezier(0.22, 1, 0.36, 1);      /* anticipation → settle */
--ease-exit:     cubic-bezier(0.4, 0, 1, 1);          /* departures: accelerate away */

--dur-instant: 120ms;  --dur-fast: 240ms;  --dur-base: 420ms;
--dur-slow:    700ms;  --dur-cinematic: 1100ms;   /* hero moments only */

--spring-soft:  { stiffness: 170, damping: 24 };     /* framer-motion default-ish   */
--spring-fluid: { stiffness: 120, damping: 18, mass: 0.9 };  /* cards, panels      */
--spring-pop:   { stiffness: 420, damping: 28 };      /* chips, toggles, ticks      */
```

### 8.2 Core patterns

**Staggered emergence (lists, sector grid).** Children enter upward 12px + fade + slight
scale(0.97→1), 60ms stagger, `--ease-emerge`. When a new sector is selected, the *old* list
exits first (fade + slide toward the direction of navigation), then the new list staggers in —
directional flow, not crossfade.

**Price ticker.** Hero price flips via rolling digit columns (each digit is a vertical strip
that translates to the new value). Digits that didn't change don't move. Color pulses
`--up`/`--down` once (600ms) on each tick, then returns to primary white.

**Chart draw-in (required).** Lines draw via `stroke-dashoffset` over `--dur-cinematic`
with `--ease-fluid`; the under-curve gradient *fades in after* the line completes (250ms
delay, 400ms fade); the glow fades in last. The latest price dot pops with `--spring-pop`
and its label counts up from 0 to value.

**Number count-up.** Every KPI and pie-slice percentage counts up 800ms with `ease-out`
while the tabular-nums font prevents layout shift.

**Directional page transition.** Drill into a company: the sector view scales to 0.98 and
fades back; the company view emerges from the *right* (or from the clicked card's position
via shared layout). Back reverses it. Root of all navigation = direction, not opacity.

**Hover physics.** Cards lift: translateY(-2px) + border to `--border-focus` + glow —
spring `--spring-soft`, 200ms. Chart tooltips follow the crosshair with a 1-frame lag
(spring 300) so they feel liquid, not glued.

**Inner activity ("thinking" states, required).** While fetching/computing:
- skeleton rows get a **sweep** gradient traveling through them (1.4s loop, linear);
- the section's ambient glow **breathes** (opacity 0.5→1, 2.4s ease-in-out infinite);
- an **orbiter**: a small white dot travels along the panel's rounded border (offset-path),
  conveying "processing" without a spinner. Spinners are banned; orbiters and sweeps only.
- streaming text/quotes appear word-by-word (or row-by-row) with a 30ms cadence — the
  Gemini "laying out the response" feel, used for the company summary and market notes.

**Border light traveler (required on the main chart panel).** A short, blurred light
streak (≈20% width, 2px tall) travels along the **top edge** of the hero panel, left →
right, in the panel's direction color, on a slow 6–8s loop — the Google Finance "light
running along the border" effect. Implemented with `background-position` travel on a
full-width hairline (no layout animation); fades in only while the panel is the active
view. It reads as "this stream is live" — never use more than one per viewport.

**Dot-field aura & sparkles (Neural Expressive signature — reserved for the ask/search
surface, burst-driven).** When the surface **activates** — focus, press, hover-enter,
typing, or the overlay/box first appearing — a burst plays, then everything vanishes:

- A **halftone dot field** fades in around the pill (grid of ~1px white dots in a
  ~28px band), density and opacity **falling off quadratically with distance from
  the border** — bright at the edge, dissolving outward.
- Every dot is modulated by a per-dot **shimmer** (phase varies with x/y) and an
  **expanding ripple** (`sin(distance·k − t)` rings radiating from the pill);
  ~2% of dots twinkle briefly to full white.
- 4-point star **sparkles** (7–12px) pop at randomized perimeter points while the
  burst is live (scale 0→1→0, ~20° rotation, max 3 concurrent).
- The pill's **glow pulses in sync** with the envelope: border brightens, white
  glow swells, then relaxes.

The whole burst lives **0.8–1.2s** with an ease-in → peak → ease-out envelope, then
fades to nothing — **never a permanent loop**; at rest the surface is quiet. This marks
the AI/entry surface; never apply to tables, cards, or buttons. Canvas (`rAF` runs
only during the burst) or a dot-matrix `box-shadow` grid both work; keep it ≤ ~1k dots.

**Micro-interactions.** Chip selection pops (`--spring-pop`, scale 1→1.06→1). Toggle knobs
use `--spring-fluid`. Pie slices re-tween angle + radius on data change (`--spring-fluid`),
largest slice eases first. Sort changes re-order rows with layout animation (FLIP), 320ms.

### 8.3 Discipline

- Never animate `width`/`top`/`left`; animate `transform` and `opacity` only (except SVG
  stroke-dashoffset and framer-motion layout animations).
- One hero moment per view (the big chart draw). Everything else stays ≤ `--dur-base`.
- Infinite animations (breathing, orbiting) must be subtle and stop when data arrives.
- **`prefers-reduced-motion`:** all transforms become instant opacity fades ≤ 150ms; charts
  render fully drawn; ticker digits swap without rolling; orbiters/breathing disabled.

### 8.4 Apple-grade interaction polish (required)

The finish layer — small, physical, constantly responsive:

- **Press feedback.** Every interactive surface scales to 0.96–0.98 while `:active`
  and springs back. Nothing on the page is ever "dead" under the cursor.
- **Segmented controls slide.** Range chips (1M/3M/1Y) are transparent inside one
  container; a single pill slides between them (transform only, `--ease-fluid`,
  420ms) like the iOS segmented control. Never per-chip background swaps.
- **Scroll-linked header.** Condenses (denser glass + shadow) after 8px of scroll;
  hides on scroll-down past ~220px, reveals on any scroll-up — spring, not snap.
- **Reveal on scroll.** Below-the-fold sections observe the viewport and emerge
  (24px rise + fade, 750ms), once; never re-trigger after shown.
- **Card tilt ≤ 4°.** Bento cards rotate subtly toward the cursor with a radial
  glare that tracks it; transforms reset with a spring on leave. Fully disabled
  under reduced motion.
- **Liquid tooltips.** The chart crosshair + glass tooltip follow the pointer via a
  rAF lerp (factor ≈ 0.18) so they trail like a fluid, never teleport.
- **Entrance choreography.** On load: header drops in, then hero label and numeral
  rise in sequence (~100ms offsets). One-time, `backwards` fill, zero layout shift.
- **Momentum everywhere.** `scroll-behavior: smooth`; the ambient background drifts
  at ~4% of scroll speed (parallax ≤ 0.05 — more feels broken, less feels flat).

## 9. Data Visualization

### 9.1 Time-series (price) chart — the centerpiece

- SVG or Canvas (Recharts/ECharts/lightweight-charts all fine — style them with tokens).
- Line: 2px, `--text-primary` white **or** direction color (`--up`/`--down`) for the period's
  net change. Only the line may be colored; axes/grid stay monochrome.
- **Underglow (required):** `filter: drop-shadow(0 0 4px var(--up-glow))` on the line.
- **Under-curve gradient (required):** area fill from §3.1, opacity .22 → 0, vertical.
- Grid: horizontal hairlines `rgba(255,255,255,0.05)`; no vertical gridlines.
- Crosshair: 1px vertical white @ 12% + tooltip `glass` + `--blur-sm`, spring-following.
- Range selector (1D · 1W · 1M · 1Y · 5Y): pill chips; the active chip has the white sweep.
  Switching ranges **re-draws** the line (draw-in again, 700ms), it does not snap.
- Intraday green/red coloring: line color flips exactly once, where the period crosses
  its opening value. Never multi-color the line.
- Volume bars (optional sub-pane): monochrome gray, highlight the hovered bar white.

### 9.2 Sector allocation — donut (not full pie)

- Donut, inner radius ≈ 62% of outer; center shows the active metric (e.g. total mkt cap).
- Slices are **grayscale by rank** (brightest = largest: #fafafa → #52525b → #27272a), each
  with a faint white rim glow. On hover: slice eases outward 6px, turns full white, glow.
- The **direction** of each sector's day-change appears as a thin outer arc in `--up`/`--down`
  *only when filtered to "today"* — color stays data-bound.
- Slice re-tween on data change with `--spring-fluid`; hover grows the center readout.
- Exploded labels sit outside with hairline leader lines, muted text.

### 9.3 Other charts

- **Bar (top gainers/losers):** horizontal, bars gray; value labels colored `--up`/`--down`;
  bar fill stays monochrome — color labels, not bars, except the extreme (max/min) bar which
  may glow in its direction color.
- **Heatmap (sector × day):** monochrome intensity grid for size; only text/percentages
  inside cells carry red/green.
- **Sparklines (table rows):** 24×8px, 1.25px line in direction color with a *mini* gradient
  under-fill (opacity .15→0). No glow at this size. They draw in with a fast 400ms dash.
- Every chart animates its entrance. A chart that appears fully drawn instantly is a defect.

### 9.4 Semantics beyond color (accessibility, mandatory)

Red/green alone fails deuteranopia. Every directional element carries a **shape** too:
▲/▼ glyphs before percentages, arrows on ticker flips, position (left/right) in the heatmap
legend, and up/down wording in labels. Screen-reader text always says "up 2.4 percent",
not "green".

---

## 10. Layout & Components

### 10.1 App flow

```
[Header: glass, logo · tabs · search / command palette]
        │
        ├─ VIEW 1  Macro
        │    · hero: NIFTY 50 numeral + ambient glow + grain + inline sparkline
        │    · sector grid (bento): cards with sparkline + P/E + 1y Δ + verdict badge
        │    · sector interdependence graph (§10.4) BELOW the sector cards
        │
        ├─ VIEW 2  Sector Drill-Down (chosen sector; grows out of the clicked card)
        │    · sector header (name, count, Δ, index sparkline panel)
        │    · filter sidebar (§10.5): editable toggles/selects, persisted in
        │      localStorage, "N survive" counter, reset-to-defaults
        │    · company table: ticker · name · mktcap · P/E vs own 5Y · 5Y CAGR ·
        │      moat pills — curated constituents FIRST, then the rest of the
        │      mapped Nifty-200 universe; every row opens the deep-dive
        │
        └─ VIEW 3  Company Deep-Dive (slide-out panel over any screen)
        │    · header: name + ticker + sector tag ("uncurated" when generic)
        │    · verdict banner + narrative (streamed feel) + valuation gauge
        │    · noise-filter chart: 5D/1M/6M/1Y segmented chips (§9.1) + earnings dots
        │    · KPI row: price/mktcap/P.E/own 5Y P.E/sector P.E/5Y CAGR (count-up)
        │    · "Open in Google Finance ↗" action pill
```

### 10.2 Component inventory & rules

- **Header/nav:** glass `--blur-lg` over the charcoal wall, pill search field, hairline
  bottom border + darker glass after scroll; hides on scroll-down (§8.4).
- **Sector cards (bento):** metal-lit `surface-1` (§4a), radius-lg, name + mono count,
  current/5Y P/E line, sparkline, verdict badge. Hover = lift toward the beam + sweep.
  Selected = white rim glow + sweep persists.
- **Cursor-following glow (all screens):** one 600px fixed radial
  (`rgba(255,255,255,.05) → transparent`, `mix-blend-mode:screen`) lerping behind the
  pointer at ~0.055 ease. It is *ambience, not an effect*: never brighter than the beam
  (§4a), never blocking, pointer-events none, and fully disabled on coarse pointers and
  under `prefers-reduced-motion`. It must not read as a spotlight or follow instantly.
- **Deep-dive slide-out:** fixed right panel (`surface-2` + metal light), scrim 35%,
  panel above scrim but below the header; slides from the right with `--ease-fluid`.
- **Range chips (noise-filter chart):** iOS-style segmented pill — one `surface-3` pill
  slides between 5D / 1M / 6M / 1Y labels; the active label brightens; switching refetches
  history and re-runs the chart's draw-in. 5D uses intraday timestamps.
- **Action pill (Google Finance):** full-width centered pill at the deep-dive's foot,
  `surface-3`, mono/display face, press = scale .97. External links open in a new tab.
### 10.3 Spacing & grid

Container max 1280px, gutter 24px. Spacing scale: 4 · 8 · 12 · 16 · 24 · 32 · 48 · 64 · 96.
Cards pad 24px, section gap 48px. Bento grid: 12-col, gap 16px. Density in tables is high —
row height 44px — but panels stay airy.

### 10.4 Sector interdependence graph (below the sector cards)

A fixed-layout flow diagram on its own panel, always the last block of the Macro screen —
the economic story sits *under* the numbers, never beside them.

- **Nodes = sectors**, rendered as circular plates (`surface-2` fill, 1.6px ring colored by
  the sector's verdict — up/down/neutral) showing short name, live P/E, and 1y Δ.
- **Edges = provider → consumer** ("whose output feeds whose earnings"), quadratic curves
  whose stroke width maps to dependency strength (1px + weight×6). Each edge has a dim
  base pass and a brighter flow pass; both draw in via `stroke-dashoffset` once, on load.
- **Hover isolates**: hovering a node drops every non-adjacent edge to 10% opacity —
  you see exactly who feeds that sector and who it feeds. A `<title>` per edge carries
  the plain-language note ("consumer credit", "project & capex finance").
- **Click = drill into the sector** with the same press → recede → grow-out transition
  as the sector cards.
- Hand-laid node coordinates are fine (readability beats auto-layout), but edge data must
  come from the backend, not the CSS.

### 10.5 Sector drill-down filters (editable + persisted)

- A sticky sidebar of toggles grouped **Structure / Valuation / Growth & Size**, plus
  "Moat type" and "Sort by" selects, and a big "N survive" counter with a reset pill.
- Every control is live: rows filter and re-sort with directional motion (§8.1/§8.4),
  no page reload, no spinners.
- **State persists in localStorage** under one key (`cc-filters`) and restores on the next
  visit; defaults are: Coffee-Can Core ON, everything else off, sort by mktcap.
- The table shows **all** mapped-universe stocks of the sector, not just curated ones —
  curated rows are discoverable via the "Coffee-Can Core" toggle (monopoly / cultural
  moat / defensive names), not by hiding the rest of the market.

---

## 11. NSE Sector Taxonomy (default data model)

Seed the app with these sectors (NSE / Nifty classifications). Every listed company must
resolve to a real NSE symbol; fetch quotes live where possible, fall back to the symbol.

| Sector | Example NSE-listed constituents |
|---|---|
| Banking | HDFCBANK, ICICIBANK, SBIN, KOTAKBANK, AXISBANK, INDUSINDBK, BANKBARODA, PNB |
| Financial Services | BAJFINANCE, BAJAJFINSV, SBILIFE, HDFCLIFE, ICICIGI, SHRIRAMFIN, CHOLAFIN, LICI |
| Information Technology | TCS, INFY, HCLTECH, WIPRO, TECHM, LTIM, PERSISTENT, COFORGE |
| Oil, Gas & Energy | RELIANCE, ONGC, IOC, BPCL, GAIL, NTPC, POWERGRID, ADANIGREEN, TATAPOWER |
| Automobile & Auto Components | MARUTI, TATAMOTORS, M&M, BAJAJ-AUTO, HEROMOTOCO, EICHERMOT, TVSMOTOR, ASHOKLEY |
| FMCG / Consumer Foods | HINDUNILVR, ITC, NESTLEIND, BRITANNIA, DABUR, MARICO, GODREJCP, TATACONSUM |
| Pharmaceuticals & Healthcare | SUNPHARMA, DRREDDY, CIPLA, DIVISLAB, LUPIN, AUROPHARMA, ALKEM, ZYDUSLIFE |
| Metals & Mining | TATASTEEL, JSWSTEEL, HINDALCO, VEDL, JINDALSTEL, COALINDIA, NMDC, SAIL |
| Cement & Construction | ULTRACEMCO, AMBUJACEM, ACC, SHREECEM, LT, PNCINFRA, NCC, GRASIM |
| Capital Goods / Manufacturing | SIEMENS, ABB, BHEL, HAL, BEL, CGPOWER, POLYCAB, THERMAX |
| Consumer Durables & Apparel | TITAN, ASIANPAINT, HAVELLS, VOLTAS, DIXON, POLYCAB*, BATAINDIA, PAGEIND |
| Realty | DLF, GODREJPROP, OBEROIRLTY, PRESTIGE, PHOENIXLTD, BRIGADE, LODHA, SOBHA |
| Telecom & Media | BHARTIARTL, IDEA, INDUSTOWER, TATACOMM, ZEEL, PVRINOX, SUNTV, SAREGAMA |
| Chemicals & Fertilizers | PIDILITIND, ASIANPAINT*, SRF, PIIND, UPL, DEEPAKNTR, COROMANDEL, TATACHEM |
| Services & Others | DMART, INDIGO, NAUKRI, IRCTC, DELHIVERY, CONCOR, JUBLFOOD, TRENT |

(*A symbol may map once; where starred, pick the canonical sector — e.g. ASIANPAINT →
Consumer Durables, POLYCAB → Capital Goods, TRENT → Retail under Services.)

Data model per company: `symbol, name, sector, ltp, change, change_pct, mktcap, volume,
sparkline[30], high52, low52, pe`. Model per sector: `name, nifty_index_ref, companies[],
aggregate_sparkline[30], day_change_pct`.

Screening defaults inside a sector view: sort by mktcap (desc), search by ticker/name —
and that search spans the **full Nifty-200 universe** (any listed symbol is deep-diveable,
even when its NSE industry doesn't map onto the six macro sectors; the table shows curated
constituents plus every mapped peer). Quick filters are the persisted set of §10.5.

---

## 12. Hard Rules (violations are bugs)

1. **Palette is closed.** Only the tokens in §2. Any new hue = rejection.
2. **Red/green are market-direction-only**, and always paired with ▲/▼ or wording (§9.4).
3. **Every price is mono + tabular-nums.** No proportional numerals for data.
4. **Charts must have:** the under-curve gradient, the line underglow, and a draw-in
   animation. Static charts are rejected.
5. **Gradients point somewhere.** Sharp leading edge, diffused tail (§3). Flat fills using a
   gradient token are rejected.
6. **Max one sweep + one ambient glow per section; noise ≤ 0.07 opacity.**
7. **Spinners are banned.** Use sweeps, breathing glow, orbiters (§8.2).
8. **Motion = transform/opacity only** (plus stroke-dashoffset/layout animation).
9. **`prefers-reduced-motion` fully honored** (§8.3) — not a partial implementation.
10. **No hard black borders.** Depth = translucent white borders + bevel + shadow.
11. **Every list transition is directional** (enter from where it came, exit where it goes).
12. **Uninstallable/dev-respect:** all styling via tokens — no hardcoded hex outside
    the `:root` block except the noise SVG and the four muted directional hex stops.
13. **The dot-field aura is burst-only** (§8.2): it exists only during a 0.8–1.2s
    activation burst and vanishes completely — a permanently looping aura, or the
    effect applied to any surface other than the ask/search box, is a defect.
14. **Metal is directional (§4a):** exactly one 118° beam from the top-left; every raised
    surface carries `--metal-lit` + `--metal-rake` + `--metal-edge` + `--metal-shadow`.
    A flat black panel, a second light source, or a bottom-lit plate is rejected.
15. **The cursor glow is felt, not seen (§10.2):** ≤0.05 alpha radial, lerped, disabled
    on coarse pointers / reduced-motion. If the user can tell where the "light" is
    coming from without moving the mouse, it is too strong.
16. **Filter state survives reloads** (§10.5) via localStorage; resetting restores the
    shipped defaults, never a blank table.
17. **No watchlist surface exists.** Saving-to-watch, kanban boards, and watchlist APIs
    were removed by design; stocks are researched in place (deep-dive + Google Finance
    link), not collected.

---

## 13. Copy-paste Snippets

**Line + gradient + glow (SVG):**
```svg
<defs>
  <linearGradient id="areaUp" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%"  class="area-up"   stop-opacity="0.22"/>
    <stop offset="100%" class="area-up-2" stop-opacity="0"/>
  </linearGradient>
</defs>
<path class="area" fill="url(#areaUp)" d="…" />
<path class="line" fill="none" stroke="var(--up)" stroke-width="2"
      stroke-linecap="round" stroke-linejoin="round"
      style="filter: drop-shadow(0 0 4px var(--up-glow))"
      pathLength="1" stroke-dasharray="1" stroke-dashoffset="1" />
```
Animate `stroke-dashoffset: 1 → 0` over `--dur-cinematic` with `--ease-fluid`.

**framer-motion list entrance:**
```tsx
const container = { hidden: {}, show: { transition: { staggerChildren: 0.06 } } };
const item = {
  hidden: { opacity: 0, y: 12, scale: 0.97 },
  show:   { opacity: 1, y: 0, scale: 1,
            transition: { duration: 0.42, ease: [0.22, 1, 0.36, 1] } },
};
```

**Ticker digit flip:** render each digit as a `span` with `overflow: hidden; height: 1em`,
containing a column of 0–9 translated `translateY(-digit * 1em)` with
`transition: transform 500ms var(--ease-fluid)`; unchanged digits keep their position.

**Dot-field burst (interaction-only aura):**
```js
const BURST_MS = 1050;
let burstStart = -1, rafOn = false;
function startBurst(){                       // called on focus/press/hover-enter/typing
  burstStart = performance.now();
  pill.classList.add("glow");                // glow pulse synced to the envelope
  if(!rafOn){ rafOn = true; requestAnimationFrame(drawField); }
}
function drawField(t){
  const e = t - burstStart;
  if(e >= BURST_MS){                         // burst over: vanish completely
    ctx.clearRect(0, 0, W, H); pill.classList.remove("glow");
    rafOn = false; return;
  }
  const env = Math.sin(Math.PI * e / BURST_MS);       // in → peak → out
  for (dot of dotGridAround(pill, BAND = 28)) {       // signed distance to pill border
    const base    = Math.pow(1 - d/BAND, 1.7);       // quadratic falloff
    const shim    = 0.5 + 0.5*Math.sin(t*0.0014 + x*0.11 + y*0.087);
    const ripple  = 0.5 + 0.5*Math.sin(d*0.38 - t*0.0022);   // rings expand outward
    const twinkle = hash(x, y) < 0.02 ? 1 : 0;               // ~2% dots twinkle hot
    paint(dot, alpha = min(0.6, base*(0.28 + 0.34*shim + 0.38*ripple) + twinkle*base*0.9) * env);
  }
  if (env > 0.25 && occasionally) spawnSparkle(pill);  // max 3 concurrent
  requestAnimationFrame(drawField);
}
```
At rest the surface is completely quiet — no loop, no glow. Sparkle = 4-point star,
`scale 0→1→0` with ~20° rotation over 450–900ms, removed from the DOM after 1s.

---

## 14. Quality Bar — "Does it feel Neural Expressive?"

Ship checklist:
- [ ] First paint: hero numeral counts up while ambient glow breathes and grain sits quietly.
- [ ] Sector click: old view exits toward the click, new view staggers in — you can *feel* the direction.
- [ ] Chart lands with draw-in → gradient fade → glow → price-dot pop, in that order.
- [ ] Live tick: digits roll, one color pulse, then calm. Nothing else on screen moves.
- [ ] Loading: sweeps travel, orbiter circles — the app looks like it's *thinking*, not stuck.
- [ ] Blur layers stack correctly: header over cards over ambient, grain unifying them.
- [ ] Press a chip or a sector card: it sinks ~3% and springs back. Nothing is inert.
- [ ] Drag across the main chart: crosshair + glass tooltip trail the cursor like liquid.
- [ ] Scroll down: header condenses then hides; scroll up: it returns. Ambient drifts slowly.
- [ ] The search box is quiet at rest. Focus/press/hover it: glow pulses, a dot-field aura + sparkles burst for ~1s, then everything fades away completely.
- [ ] Move the mouse across the page and *stop*: a faint light pools near the cursor, drifts in lazily, and never outshines the ambient beam. On a touch device it's simply absent.
- [ ] The page reads as metal: one diagonal beam from the top-left, every panel carrying a bright upper bevel that falls away down its face — no flat black slabs anywhere.
- [ ] Sector interdependence graph sits below the cards; hover a node and only its flows stay lit.
- [ ] Toggle filters, reload: your mix survives. Reset: back to Coffee-Can Core only.
- [ ] Deep-dive: switch 5D/1M/6M/1Y and the pill slides, the chart redraws with its full draw-in choreography; "Open in Google Finance" is one press away.
- [ ] Turn on reduced motion: everything still readable, instantly, with fades.
- [ ] Grayscale test: screenshot with color removed — the app should still be fully
      navigable and hierarchy intact. Color should be seasoning, not structure.
