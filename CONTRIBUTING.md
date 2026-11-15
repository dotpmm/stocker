# Contributing to Stocker

Thanks for helping. Stocker is a free, on-device Android app that turns a stock watchlist into a clean, per-company
news feed. Read the [README](README.md) and [product spec](docs/product-spec.md) first.

## Ground rules

- **Licence of contributions.** Stocker is proprietary (see [LICENSE](LICENSE)). By opening a pull request you confirm
  you wrote the change (or have the right to submit it) and grant the maintainer a perpetual, worldwide, royalty-free,
  irrevocable licence to use, modify, sublicense and distribute it as part of Stocker, including commercially.
- **No financial advice.** Never phrase app text as a recommendation to buy or sell.
- **No secrets.** Never commit API keys, keystores or personal data. The user's Gemini key stays on the device.

## Set up

1. Install Android Studio (stable) with SDK platform 36 and JDK 17.
2. Open the repository folder and let Gradle sync, or run `./gradlew assembleDebug`.
3. Run on a device or emulator with API 26 or newer. See [docs/building.md](docs/building.md).

## Workflow

1. Open or comment on an issue first for anything non-trivial.
2. Branch from `main`: `feat/short-name`, `fix/short-name`, `docs/short-name`.
3. Keep changes small and focused. CI (`Android CI`) must pass.
4. Use [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `build:`, `refactor:`,
   `chore:`, with an optional scope, for example `feat(ui): add source health screen`.
5. Open a pull request using the template.

## Design rules (Obsidian)

The UI language is documented in [docs/design-directions.md](docs/design-directions.md). The short version:

- Pure black `#000` canvas. **No borders, outlines, cards or dividers.** Use spacing and type for structure.
- **No pill or rounded buttons.** Selection is plain text with a sliding underline; the primary action is the solid
  white bar. Corner radii stay at or below 2 dp, except the floating nav bar.
- Numbers use tabular figures; colour never carries meaning alone (pair with arrows, signs or labels).
- Every tappable thing uses the shared press feedback and Material-3 emphasized easing from `ui/theme/Motion.kt`.
- Touch targets are at least 44 dp. Text contrast is at least 4.5:1.
- The nav bar is solid. No blur, no translucency, no shader effects.

## Code style

- Kotlin official style. Compose: stateless composables, state hoisted, previews where helpful.
- Components live in `ui/components`, screens in `ui/screens`, models in `domain/model`, data in `data`.
- New dependencies need a reason in the PR description (the app must stay small and free to run).

## Reporting bugs and ideas

Use the issue templates. For news-source problems include the feed URL and what you saw.
