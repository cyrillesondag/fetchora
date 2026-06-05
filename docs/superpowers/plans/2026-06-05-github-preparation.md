# GitHub Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add all standard open-source project files (CI workflow, publish workflow, README, LICENSE, CONTRIBUTING) to make the project public-ready on GitHub.

**Architecture:** Two GitHub Actions workflows (CI on push/PR, publish on versioned tag), a Gradle signing config reading from env vars, and three documentation files. No new Android source files. No new Gradle dependencies.

**Tech Stack:** GitHub Actions, `actions/setup-java@v4`, `actions/cache@v4`, `r0adkll/upload-google-play@v1`, Kotlin DSL (Gradle), Markdown.

---

## File map

| File | Change |
|---|---|
| `.github/workflows/ci.yml` | Create — build + unit tests on push/PR |
| `.github/workflows/publish.yml` | Create — sign + upload AAB to Play Store on tag `v*.*.*` |
| `app/build.gradle.kts` | Modify — add `signingConfigs.release` reading from env vars |
| `README.md` | Create — project presentation |
| `LICENSE` | Create — MIT license |
| `CONTRIBUTING.md` | Create — contributor guide |

> **Note on commits:** A pre-commit hook blocks commits when native tasks are pending. Commit only after all tasks in a batch are marked complete. The plan groups commits at the end of each task's steps.

---

### Task 1: CI Workflow

**Goal:** Create a GitHub Actions workflow that runs unit tests and builds a debug APK on every push and pull request to `main`.

**Files:**
- Create: `.github/workflows/ci.yml`

**Acceptance Criteria:**
- [ ] Workflow triggers on `push` to `main` and `pull_request` targeting `main`
- [ ] Uses JDK 21 (temurin distribution)
- [ ] Gradle cache configured with hash key
- [ ] Runs `./gradlew :app:testDebugUnitTest`
- [ ] Runs `./gradlew :app:assembleDebug`

**Verify:** `python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml')) or sys.exit(1); print('VALID')"` → `VALID`

**Steps:**

- [ ] **Step 1: Create the `.github/workflows/` directory and `ci.yml`**

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/ci.yml` with this exact content:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-

      - name: Run unit tests
        run: ./gradlew :app:testDebugUnitTest

      - name: Build debug APK
        run: ./gradlew :app:assembleDebug
```

- [ ] **Step 2: Validate YAML syntax**

```bash
python3 -c "import yaml,sys; yaml.safe_load(open('.github/workflows/ci.yml')) or sys.exit(1); print('VALID')"
```

Expected: `VALID`

- [ ] **Step 3: Verify the workflow references correct Gradle tasks**

```bash
grep -E "testDebugUnitTest|assembleDebug" .github/workflows/ci.yml
```

Expected output (2 lines):
```
        run: ./gradlew :app:testDebugUnitTest
        run: ./gradlew :app:assembleDebug
```

---

### Task 2: Gradle Signing Config + Publish Workflow

**Goal:** Add a conditional release signing config to Gradle (reads env vars, falls back to debug signing locally) and create the Play Store publish workflow triggered on version tags.

**Files:**
- Modify: `app/build.gradle.kts` (add `signingConfigs` block, update `release` buildType)
- Create: `.github/workflows/publish.yml`

**Acceptance Criteria:**
- [ ] `signingConfigs.release` reads `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_PASSWORD` from env
- [ ] `release` buildType uses `signingConfigs.release` when env var is set, else falls back to `debug`
- [ ] `assembleRelease` succeeds locally (unsigned/debug-signed, no env vars needed)
- [ ] `publish.yml` triggers on tags matching `v*.*.*`
- [ ] `publish.yml` decodes keystore from `secrets.KEYSTORE_BASE64`, builds AAB, uploads to `production` track, cleans up keystore with `if: always()`

**Verify:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleRelease` → `BUILD SUCCESSFUL`

**Steps:**

- [ ] **Step 1: Add `signingConfigs` and update `release` buildType in `app/build.gradle.kts`**

The current `buildTypes` block in `app/build.gradle.kts` is:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

Replace the entire `android { ... }` block content with the following (preserving all existing settings, adding `signingConfigs` before `buildTypes`):

```kotlin
android {
    namespace = "org.mediadownloader"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "org.mediadownloader"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.google.dagger.hilt.android.testing.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = if (System.getenv("SIGNING_KEY_ALIAS") != null)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
```

- [ ] **Step 2: Verify release build succeeds locally (debug-signed)**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleRelease
```

Expected: `BUILD SUCCESSFUL` (signed with debug key since no env vars set)

- [ ] **Step 3: Create `.github/workflows/publish.yml`**

```yaml
name: Publish to Play Store

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-

      - name: Decode keystore
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > keystore.jks

      - name: Build release AAB
        env:
          SIGNING_KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          SIGNING_KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          SIGNING_STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        run: ./gradlew :app:bundleRelease

      - name: Upload to Play Store
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_JSON_KEY }}
          packageName: org.mediadownloader
          releaseFiles: app/build/outputs/bundle/release/app-release.aab
          track: production

      - name: Cleanup keystore
        if: always()
        run: rm -f keystore.jks
```

- [ ] **Step 4: Validate both YAML files**

```bash
python3 -c "
import yaml, sys
for f in ['.github/workflows/ci.yml', '.github/workflows/publish.yml']:
    yaml.safe_load(open(f))
    print(f'VALID: {f}')
"
```

Expected:
```
VALID: .github/workflows/ci.yml
VALID: .github/workflows/publish.yml
```

---

### Task 3: README

**Goal:** Create a comprehensive `README.md` that presents the project, documents features, explains how to install and build, and lists the CI/CD secrets required for contributors who fork the repo.

**Files:**
- Create: `README.md`

**Acceptance Criteria:**
- [ ] CI badge and MIT badge present
- [ ] Features list covers all implemented features (quality selection, progress bar, history, configurable Cobalt URL)
- [ ] Build instructions use `./gradlew :app:testDebugUnitTest`
- [ ] Secrets table lists all 5 required secrets with generation instructions
- [ ] Links to CONTRIBUTING.md and LICENSE

**Verify:** `grep -c "##" README.md` → `6` (6 section headers)

**Steps:**

- [ ] **Step 1: Create `README.md`**

Create `README.md` at the project root with this content (replace `YOUR_USERNAME/YOUR_REPO` with the actual GitHub repository path once known):

```markdown
# XDownloader

> Download X (Twitter) videos directly to your Android phone.

[![CI](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Share any X/Twitter post containing a video, select the quality you want, and the video is saved to a folder of your choice.

---

## Features

- **Quality selection** — choose from all available resolutions before downloading
- **Live progress bar** — see download progress directly in the share sheet
- **Download history** — browse all past downloads with status and file size
- **Configurable Cobalt instance** — point the app at any public or self-hosted [Cobalt](https://cobalt.tools) server

## Requirements

- Android 10+ (API level 29)
- A Cobalt instance — use the public one at `https://api.cobalt.tools/` or [self-host](https://github.com/imputnet/cobalt)

## Install

Download the latest APK from [Releases](../../releases) and install it on your device (enable "Install from unknown sources" if prompted).

## Build from source

1. Install [Android Studio](https://developer.android.com/studio) (Meerkat or later)
2. Install JDK 21 (e.g. [Temurin](https://adoptium.net/))
3. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
   cd YOUR_REPO
   ```
4. Run tests to verify the setup:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
5. Open in Android Studio and run on a device or emulator.

## CI/CD Secrets

To enable the automated Play Store publish workflow in a fork, add these secrets in **Settings → Secrets and variables → Actions**:

| Secret | Description | How to generate |
|---|---|---|
| `KEYSTORE_BASE64` | Your release keystore encoded as base64 | `base64 -i keystore.jks \| pbcopy` |
| `KEY_ALIAS` | Alias of the signing key | Set when creating the keystore |
| `KEY_PASSWORD` | Password for the signing key | Set when creating the keystore |
| `STORE_PASSWORD` | Password for the keystore file | Set when creating the keystore |
| `GOOGLE_PLAY_JSON_KEY` | Google Play service account JSON key | [Google Play Console → Setup → API access](https://play.google.com/console) |

The publish workflow runs automatically when a tag of the form `v1.2.3` is pushed.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions, commit conventions, and the PR process.

## License

MIT — see [LICENSE](LICENSE).
```

- [ ] **Step 2: Verify section count**

```bash
grep -c "^##" README.md
```

Expected: `6`

---

### Task 4: LICENSE + CONTRIBUTING + Final Commit

**Goal:** Add the MIT license file and contributor guide, then commit all new files in a single commit.

**Files:**
- Create: `LICENSE`
- Create: `CONTRIBUTING.md`

**Acceptance Criteria:**
- [ ] `LICENSE` contains standard MIT text with year 2026 and name Cyrille Sondag
- [ ] `CONTRIBUTING.md` covers prerequisites, setup verification, commit conventions, PR process, and architecture overview
- [ ] All 6 new/modified files committed successfully

**Verify:** `git log --oneline -1` → commit message contains "chore: add GitHub project files"

**Steps:**

- [ ] **Step 1: Create `LICENSE`**

Create `LICENSE` at the project root:

```
MIT License

Copyright (c) 2026 Cyrille Sondag

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 2: Create `CONTRIBUTING.md`**

Create `CONTRIBUTING.md` at the project root:

```markdown
# Contributing to XDownloader

Thank you for your interest in contributing! This guide will help you get set up and understand the project conventions.

## Prerequisites

- **JDK 21** — [Temurin 21](https://adoptium.net/) recommended
- **Android Studio Meerkat** or later
- An Android device or emulator running Android 10+ (API 29)

## Setup

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
./gradlew :app:testDebugUnitTest   # verify everything works
```

All tests should pass before you start making changes.

## Commit conventions

Write commits in **English**, in the imperative mood, using the following prefix types:

| Type | Use for |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `test` | Adding or updating tests |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `chore` | Tooling, CI, dependencies |

Examples:
```
feat: add cancel button to download sheet
fix: progress bar not reaching 100% on fast downloads
docs: update README secrets table
```

## Pull Request process

1. Fork the repository and create a branch from `main`:
   ```bash
   git checkout -b feat/my-feature
   ```
2. Make your changes and ensure all tests pass:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```
3. Push your branch and open a Pull Request targeting `main`.
4. The CI workflow must pass before the PR can be merged.

## Architecture overview

| Layer | Technology | Role |
|---|---|---|
| UI | Jetpack Compose + Material 3 | All screens and the share BottomSheet |
| State | ViewModel + StateFlow | UI state management |
| DI | Hilt 2.59 | Dependency injection throughout |
| Downloads | WorkManager 2.10 | Background downloads with progress |
| Persistence | Room 2.7 | Download history |
| Settings | DataStore Preferences | Cobalt URL, download folder |
| Network | Retrofit 2.11 + OkHttp 4.12 | Cobalt API calls |
| Video extraction | [Cobalt API](https://github.com/imputnet/cobalt) | Resolves tweet URLs to video URLs |

The entry point for downloads is `ShareReceiverActivity` → `ShareViewModel` → `DownloadWorker`.
```

- [ ] **Step 3: Commit all files**

```bash
git add \
  .github/workflows/ci.yml \
  .github/workflows/publish.yml \
  app/build.gradle.kts \
  README.md \
  LICENSE \
  CONTRIBUTING.md \
  docs/superpowers/specs/2026-06-05-github-preparation-design.md \
  docs/superpowers/plans/2026-06-05-github-preparation.md \
  docs/superpowers/plans/2026-06-05-github-preparation.md.tasks.json
git commit -m "chore: add GitHub project files (CI, publish workflow, README, LICENSE)"
```

Expected: commit succeeds, `git log --oneline -1` shows the message.
