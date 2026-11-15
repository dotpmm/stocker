# ADR 0001: Native Android (Kotlin + Jetpack Compose) vs Flutter

- Status: **Accepted** (owner confirmed 2026-11-15: native Kotlin, no Flutter)
- Date: 2026-11-15

## Context

Stocker is Android-first, runs entirely on-device, needs reliable 12-hour background work, and must look
extremely polished in a pitch-black theme. The owner will later share it with friends and others, and is already
doing Android development. Open question: Kotlin/Compose or Flutter.

## Decision: Kotlin + Jetpack Compose

1. **Background work is the core feature.** WorkManager is first-class on native Android. In Flutter, the same
   feature goes through a plugin that spins up a separate Dart isolate, which is more fragile on aggressive OEM
   battery managers and harder to debug.
2. **Android-only product.** Most of Flutter's value is one codebase for iOS and Android. A phone-only Indian
   market tool for NSE/BSE users is overwhelmingly Android, so that benefit is small for now.
3. **Native Material 3 Expressive and platform polish.** Expressive components, predictive back, edge-to-edge,
   haptics and dynamic type are native. A custom black design system is equally easy in Compose.
4. **Home-screen widgets (planned).** Jetpack Glance is native; Flutter widgets need native code anyway.
5. **Networking and parsing.** OkHttp, a hidden WebView for cookie warm-up (for exchange filings), Room and
   kotlinx.serialization are all native and well supported.
6. **Existing skills and fit.** The owner is already building Android apps; staying native avoids a second
   toolchain and keeps AI-assisted debugging simpler.

## Where Flutter would win

- If an iOS version is a firm goal soon.
- If most of the UI were custom-drawn charts and animations shared across platforms.

If iOS later becomes important, the domain logic (dedupe, scoring, prompts) can move to Kotlin Multiplatform
without rewriting the Android UI.

## Consequences

- Stack: Kotlin, Jetpack Compose (Material 3 1.4.0 stable, which includes the Expressive components), Room,
  WorkManager, OkHttp, kotlinx.serialization, Coil, Glance (later).
- Android-only for now.
