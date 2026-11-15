# Releasing

Releases are built by GitHub Actions when a version tag is pushed, or when the Release workflow is run manually (Actions tab > Release > Run workflow, enter the version; the tag is created for you). The workflow is `.github/workflows/release.yml`.

## Cut a release

1. Move the "Unreleased" notes in `CHANGELOG.md` under a new `## [X.Y.Z] - YYYY-MM-DD` heading.
2. Bump `versionName` (and increment `versionCode`) in `app/build.gradle.kts`.
3. Commit, push to `main`, wait for `Android CI` to pass.
4. Tag and push: `git tag -a vX.Y.Z -m "Stocker vX.Y.Z" && git push origin vX.Y.Z`.
5. The workflow builds `Stocker-vX.Y.Z.apk`, writes a SHA-256 file, and publishes a GitHub Release whose notes come from
   the matching CHANGELOG section.

## Signing

By default the workflow signs with a throwaway debug key. That installs fine, but every build has a different
signature, so Android refuses to update one build with the next (uninstall first). For stable updates, set up a
permanent release key **once**:

```
keytool -genkeypair -v -keystore stocker-release.jks -alias stocker -keyalg RSA -keysize 4096 -validity 36500
base64 -w0 stocker-release.jks > stocker-release.jks.b64        # macOS: base64 -i stocker-release.jks
```

Add these four repository secrets (Settings > Secrets and variables > Actions):

| Secret | Value |
| --- | --- |
| `STOCKER_KEYSTORE_BASE64` | contents of `stocker-release.jks.b64` |
| `STOCKER_KEYSTORE_PASSWORD` | the keystore password |
| `STOCKER_KEY_ALIAS` | `stocker` |
| `STOCKER_KEY_PASSWORD` | the key password |

When `STOCKER_KEYSTORE_BASE64` is present the workflow signs with it and says so in the release notes.

**Back up the keystore and its passwords somewhere safe and never commit them.** If the key is lost, installed copies
can no longer be updated. The folder `release-keys/` is git-ignored for local storage.

## Local release build

`./gradlew assembleRelease` signs with the debug key unless `STOCKER_KEYSTORE_FILE`, `STOCKER_KEYSTORE_PASSWORD`,
`STOCKER_KEY_ALIAS` and `STOCKER_KEY_PASSWORD` are set in the environment.
