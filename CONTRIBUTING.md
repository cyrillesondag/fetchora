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
