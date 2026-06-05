# GitHub Preparation — Design

**Date:** 2026-06-05  
**Status:** Approved  
**Feature:** GitHub-ready project files (CI, publish workflow, README, LICENSE, CONTRIBUTING)

---

## Overview

Prepare the project for public GitHub hosting by adding standard open-source project files and two GitHub Actions workflows: one for continuous integration (build + tests on every push/PR) and one for automated Play Store publication on versioned tags.

---

## Files Produced

| File | Role |
|---|---|
| `.github/workflows/ci.yml` | Build + unit tests on push/PR to `main` |
| `.github/workflows/publish.yml` | Build release AAB, sign, upload to Play Store on tag `v*.*.*` |
| `README.md` | Project presentation, features, install, dev setup, secrets config |
| `LICENSE` | MIT license — Cyrille Sondag, 2026 |
| `CONTRIBUTING.md` | Contributor guide (setup, conventions, PR process, architecture overview) |

No Gradle dependencies added. No Android source files modified. Publication is handled entirely by GitHub Actions (`r0adkll/upload-google-play`).

---

## CI Workflow — `.github/workflows/ci.yml`

**Trigger:** `push` and `pull_request` targeting `main`

**Steps:**
1. `actions/checkout@v4`
2. `actions/setup-java@v4` — JDK 21, distribution `temurin`
3. `actions/cache@v4` — Gradle cache (`~/.gradle/caches`, `~/.gradle/wrapper`)
4. `./gradlew :app:testDebugUnitTest` — run unit tests
5. `./gradlew :app:assembleDebug` — verify debug build compiles

**No secrets required.**

---

## Publish Workflow — `.github/workflows/publish.yml`

**Trigger:** `push` on tags matching `v*.*.*`

**Steps:**
1. `actions/checkout@v4`
2. `actions/setup-java@v4` — JDK 21, distribution `temurin`
3. `actions/cache@v4` — Gradle cache
4. Decode keystore: `echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks`
5. `./gradlew :app:bundleRelease` with signing env vars:
   - `SIGNING_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}`
   - `SIGNING_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}`
   - `SIGNING_STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}`
6. `r0adkll/upload-google-play@v1` — upload `app/build/outputs/bundle/release/app-release.aab` to `production` track
7. `rm -f keystore.jks` — cleanup keystore (`if: always()`)

**Required GitHub Secrets:**

| Secret | Content |
|---|---|
| `KEYSTORE_BASE64` | Keystore file encoded as base64 (`base64 -i keystore.jks`) |
| `KEY_ALIAS` | Alias of the signing key |
| `KEY_PASSWORD` | Password for the signing key |
| `STORE_PASSWORD` | Password for the keystore |
| `GOOGLE_PLAY_JSON_KEY` | Full JSON content of the Google Play service account key |

**`app/build.gradle.kts` signing config** — add a `signingConfigs.release` block reading from env vars:

```kotlin
signingConfigs {
    create("release") {
        keyAlias = System.getenv("SIGNING_KEY_ALIAS")
        keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        storeFile = file("../../keystore.jks")
        storePassword = System.getenv("SIGNING_STORE_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // existing config...
    }
}
```

This is the only change to Gradle files.

---

## README.md Structure

```
# XDownloader

> Download X (Twitter) videos directly to your phone.

[CI badge] [MIT badge]

[Screenshot]

## Features
- Quality selection (all available resolutions)
- Live download progress bar
- Download history
- Configurable Cobalt instance URL

## Requirements
- Android 10+ (API 29)
- A Cobalt instance (self-hosted or public)

## Install
Download the latest APK from [Releases](../../releases) or build from source.

## Build from source
- Java 21
- Android Studio Meerkat or later
- `./gradlew :app:testDebugUnitTest` to run tests

## CI/CD Secrets Setup
[Table of 5 secrets with generation instructions]

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md)

## License
MIT — see [LICENSE](LICENSE)
```

---

## LICENSE

MIT License, year 2026, copyright holder: Cyrille Sondag. Standard MIT text.

---

## CONTRIBUTING.md Structure

```
# Contributing

## Prerequisites
- Java 21
- Android Studio Meerkat+

## Setup
git clone ...
./gradlew :app:testDebugUnitTest   # verify setup

## Commit conventions
English, imperative: `type: short description`
Types: feat / fix / docs / test / refactor / chore

## Pull Request process
1. Branch from `main`
2. Make changes, ensure tests pass
3. Open PR targeting `main`
4. CI must pass before merge

## Architecture overview
- **MVVM** with Hilt for dependency injection
- **WorkManager** for background downloads
- **Room** for download history
- **Cobalt API** for video URL extraction
- **Jetpack Compose + Material3** for UI
```

---

## Out of Scope

- Instrumentation / UI tests in CI
- Release notes automation
- Beta / alpha tracks (publish directly to production on tag)
- Dependabot configuration
