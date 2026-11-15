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

## Design rules

Since v2.0 the UI follows the Monochrome Exchange + Metal Finish language in [design.md](design.md). The short version:

- Monochrome first. Colour (sage and clay) appears only to show market direction and always comes with a ▲/▼ glyph.
- Metal surfaces are reserved for a few important containers (nav bar, hero, primary actions, search, sheet). Everything
  else is flat.
- Numbers use tabular figures; colour never carries meaning alone.
- Interactions use the shared press feedback and motion tokens in `ui/theme/Motion.kt`, and every animation must have a
  reduced-motion path.
- Touch targets are at least 44 dp. Text contrast is at least 4.5:1.
- Appearance settings apply to every surface, including the nav bar.

## Code style

- Kotlin official style. Compose: stateless composables, state hoisted, previews where helpful.
- Components live in `ui/components`, screens in `ui/screens`, models in `domain/model`, data in `data`.
- New dependencies need a reason in the PR description (the app must stay small and free to run).

## Reporting bugs and ideas

Use the issue templates. For news-source problems include the feed URL and what you saw.
