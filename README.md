# Fetchora

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

## Install

Download the latest APK from [Releases](https://github.com/YOUR_USERNAME/YOUR_REPO/releases) and install it on your device (enable "Install from unknown sources" if prompted).

You need:
- Android 10+ (API level 29)
- A Cobalt instance — use the public one at `https://api.cobalt.tools/` or [self-host](https://github.com/imputnet/cobalt)

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
| `KEYSTORE_BASE64` | Your release keystore encoded as base64 | `base64 keystore.jks` (Linux) or `base64 -i keystore.jks` (macOS) |
| `KEY_ALIAS` | Alias of the signing key | Set when creating the keystore |
| `KEY_PASSWORD` | Password for the signing key | Set when creating the keystore |
| `STORE_PASSWORD` | Password for the keystore file | Set when creating the keystore |
| `GOOGLE_PLAY_JSON_KEY` | Google Play service account JSON key | [Google Play Console → Setup → API access](https://play.google.com/console) |

The publish workflow runs automatically when a tag of the form `v1.2.3` is pushed.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions, commit conventions, and the PR process.

## License

MIT — see [LICENSE](LICENSE).
