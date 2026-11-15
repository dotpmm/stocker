# Design directions (2026-11-15)

Three directions were drawn on the same stock-detail screen so they can be compared fairly. The canvas is a private
design artifact; this file records the decisions and the proposed tokens.

| Option | Idea | Strengths | Trade-offs |
| --- | --- | --- | --- |
| **A. Obsidian** (custom pitch-black) | True #000, hairline borders, big tabular numerals, one cool accent, tone shown as a thin bar | Reads as a premium finance product, best battery on OLED, highest data density, easiest to keep consistent | Needs a custom component set (we own the polish) |
| **B. Expressive** (Material 3 Expressive on black) | Large tonal shapes, bold type, pill navigation, mixed corner radii | Feels native to Android, Material 3 components come ready-made | More colourful and "friendly"; lower data density; looks like many other M3 apps |
| **C. Glass** (Liquid-Glass-inspired) | Frosted translucent panels over soft glows | Striking in screenshots | No native Android equivalent, so blur is faux and costs GPU/battery; glows break the pitch-black brief; legibility risk over moving content |

## Recommendation: A, Obsidian

It matches the brief (pitch black, polished, "not self-made") and puts the data first. Material 3 Expressive can still
be used under the hood for behaviour (navigation, motion, accessibility) while Obsidian supplies the look.

## Proposed Obsidian tokens (starting point)

| Token | Value |
| --- | --- |
| background | `#000000` |
| surface | `#08080A` |
| hairline | `#1C1C20` |
| text | `#F4F4F5` |
| text-muted | `#9B9BA5` (about 7:1 on black) |
| up / positive | `#3DDC97` (always paired with a ▲ and a sign) |
| down / negative | `#FF6B6B` (always paired with a ▼) |
| neutral | `#55555F` |
| accent (AI, links, dates) | `#9DB8FF` on `#0B1020` with `#3A4A7A` border |
| UI font | Inter (400/500/600/700) |
| numeric font | Space Grotesk 500/600, tabular figures |
| radii | cards 16-20, chips 10-22, buttons 14, hit targets >= 44 dp |
| nav | 4 tabs: Watchlist, News, Events, Settings |

Rules: colour never carries meaning alone (arrows, signs, labels accompany it); prices use tabular numerals;
secondary text stays at or above 4.5:1 contrast.

## Screens drawn so far

Stock detail (price, trend, tone bar, AI summary button, upcoming events, news) for A, B and C, and the Obsidian
watchlist. Still to design after the direction is confirmed: article reader, AI summary sheet, add-stock search,
news feed, events list, settings and source health, and empty/loading/error states.

All content in the mockups is sample data.
