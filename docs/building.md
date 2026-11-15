# Building Stocker

## Requirements

- Android Studio (current stable) or JDK 17 plus the Android SDK with platform 36.
- Nothing else. There is no backend and no API key needed for the current build, which uses sample data.

## Android Studio

1. File > Open > select the repository folder.
2. Let Gradle sync. If Studio offers to upgrade the Android Gradle Plugin or Kotlin, accept; the versions in
   `gradle/libs.versions.toml` were chosen conservatively and were **not compiled locally before the first push**.
3. Run the `app` configuration on a device or emulator (API 26+).

## Command line

```
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
```

## CI

`.github/workflows/android.yml` builds the debug APK on every push to `main` and every pull request, and uploads it as
the `stocker-debug-apk` artifact. Check the Actions tab after a push; a red build means a version or code fix is needed.

## Status

Phase 0: design system, watchlist and stock-detail screens with sample data. No networking yet.
