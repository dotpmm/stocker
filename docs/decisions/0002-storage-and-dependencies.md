# 0002: Plain SQLite and a minimal dependency set

Status: Accepted (2026-11-15)

## Context

The app needs a small local database, periodic background work, HTTP and an XML feed parser. The build environment
used for development has no Android SDK or Maven access, so the only compile check is CI, and every extra Gradle
plugin or annotation processor (Room with KSP, kotlinx.serialization, Hilt) is a way for the build to break in ways
that cannot be debugged locally.

## Decision

- Storage: `SQLiteOpenHelper` with hand-written SQL (`data/db/StockerDb.kt`). Watchlist and settings live in
  SharedPreferences as JSON.
- Network: `HttpURLConnection` with a per-host politeness gate (`data/net/Http.kt`). No OkHttp, Ktor or Retrofit.
- Parsing: the platform `XmlPullParser` for RSS and Atom, `org.json` for JSON.
- Only two third-party libraries: WorkManager (12-hour refresh) and kotlinx-coroutines.
- UI state: a single `StockerStore` exposing StateFlows plus suspend queries, read through a small `rememberQuery`
  helper. No ViewModel or DI framework.

## Consequences

- Fewer moving parts and a smaller APK; the release build compiled on the first try with R8 on.
- More boilerplate than Room. If the schema grows, migrating to Room later is straightforward because all SQL is in
  one file.
- No automatic cancellation tied to screen lifetimes; refresh runs in the store's own scope and is guarded by a lock.
