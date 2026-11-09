# Building Keyguard

The supported reproducible build path is GitHub Actions. The workflows do not
need Vaultwarden credentials and do not contact a production Vaultwarden.

## GitHub Actions

- `Custom Linux CI` runs on push, pull request, or manually. It uses Linux only,
  runs JVM validation, builds debug Android and Wear APKs, checks the native
  bundles, and rejects Firebase Analytics/Crashlytics in the dependency graph
  or packaged APK. Debug artifacts are retained for 7 days.
- `Custom release` runs for `v*` tags or manually. A short Linux job validates
  the persistent signing key and passwords first. Android and Wear release APKs
  build on Linux; the Compose Desktop ARM64 DMG runs on `macos-15` only after
  both APK jobs pass. All three must pass before a GitHub Release is created.

The macOS artifact is the project's native Compose Desktop DMG. It is currently
unsigned/not notarized. Gatekeeper may require opening it explicitly or removing
the quarantine attribute. Developer ID signing and notarization can be added
later without changing the package task.

The upstream checks, sync jobs, and release/deployment workflows remain
available through `workflow_dispatch`. Their branch filters are set to the
dedicated `codex-manual-only` branch where applicable, so only Custom Linux CI
runs on normal development pushes or pull requests. Publishing this custom
GitHub Release does not start external deployment jobs.

## Required repository secrets

Create these repository-level Actions secrets under GitHub Settings → Secrets and
variables → Actions. Environment secrets are not exposed to these jobs. Never
commit the files they produce:

- `ANDROID_KEYSTORE_BASE64`: base64 contents of the persistent release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore/store password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: release key password.

For this fork, the persistent PKCS12 keystore and an environment file containing
all four secret values were generated once in the ignored `.release-secrets/`
directory. Back up `keyguard-release.p12` and `release.env` securely outside the
repository and this computer. Losing them means future APKs cannot update apps
signed with this key. Do not share the environment file or put it in an artifact.
The key is self-signed for Android APK signing; it is unrelated to macOS signing.

The release job decodes the keystore only into the ephemeral runner filesystem.
It accepts direct base64, upstream's gzip + base64, and an additional base64
encoding layer. It then detects JKS or PKCS12 and checks that the alias and both
passwords can access the private key. It passes that format to Gradle through
the module-local properties files and never uploads signing files.

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
git tag v3.7.1-YYYYMMDD.N
git push origin v3.7.1-YYYYMMDD.N
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
