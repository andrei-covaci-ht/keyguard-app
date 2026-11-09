# Building Keyguard

The supported reproducible build path is GitHub Actions. The workflows do not
need Vaultwarden credentials and do not contact a production Vaultwarden.

## GitHub Actions

- `Custom Linux CI` runs on push, pull request, or manually. It uses Linux only,
  runs JVM validation, builds debug Android and Wear APKs, checks the native
  bundles, and rejects Firebase Analytics/Crashlytics in the dependency graph
  or packaged APK. Debug artifacts are retained for 7 days.
- `Custom release` runs for `v*` tags or manually. It builds signed
  `:androidApp:assembleNoneRelease` and `:wearApp:assembleNoneRelease` on Linux,
  then builds the Compose Desktop ARM64 DMG on `macos-15`. All three must pass
  before a GitHub Release is created.

The macOS artifact is the project's native Compose Desktop DMG. It is currently
unsigned/not notarized. Gatekeeper may require opening it explicitly or removing
the quarantine attribute. Developer ID signing and notarization can be added
later without changing the package task.

The upstream test, native, lint, and integration workflows remain available
through `workflow_dispatch`. Their branch filters are set to the dedicated
`codex-manual-only` branch, so they do not run on normal development pushes or
pull requests. Gradle wrapper validation and the targeted website check remain
automatic. F-Droid, Homebrew, and Scoop deployment workflows are manual-only,
so publishing this custom GitHub Release will not start external publishing
jobs. The old multi-platform upstream release workflow is also manual-only.

## Required repository secrets

Create these Actions secrets; never commit the files they produce:

- `ANDROID_KEYSTORE_BASE64`: base64 contents of the persistent release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore/store password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: release key password.

The release job decodes the keystore only into the ephemeral runner filesystem,
creates the two module-local properties files, and never uploads signing files.
The existing Keyguard build logic is used unchanged.

Create the keystore once, offline, and keep a backup. Do not run this from CI:

```bash
keytool -genkeypair -v -keystore keyguard-release.keystore \
  -alias keyguard-release -keyalg RSA -keysize 4096 -validity 10000
```

On macOS/Linux, create the secret value with:

```bash
base64 < keyguard-release.keystore | tr -d '\n'
```

## Release process

After the four secrets are configured:

```bash
git tag v3.7.0-custom.1
git push origin v3.7.0-custom.1
```

The tag starts Linux Android/Wear jobs and one Apple Silicon macOS job. The
publish job attaches `Keyguard-Android-<version>.apk`,
`Keyguard-Wear-<version>.apk`, `Keyguard-macOS-arm64-<version>.dmg`, and
`SHA256SUMS`. For a manual test, run `Custom release` and provide an existing
`v*` tag as `release_tag`.

## Local builds

Local builds require JDK 21, the Android SDK/NDK versions pinned in
`gradle/libs.versions.toml`, the Rust toolchain used by `.github/actions/setup_rust`,
and the repository's normal Gradle dependencies. The verified tasks are:

```bash
./gradlew :androidApp:assembleNoneRelease
./gradlew :wearApp:assembleNoneRelease
./gradlew :desktopApp:createReleaseDistributable
./gradlew :desktopApp:packageReleaseDmg
```

For a signed local Android build, place your own persistent
`keyguard-release.keystore` and `keyguard-release.properties` in each Android
module using the property names consumed by `buildPlugins`. Do not copy Actions
secrets into the repository.
