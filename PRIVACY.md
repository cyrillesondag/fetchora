# Privacy Policy — Fetchora

_Last updated: June 10, 2026_

## What Fetchora does

Fetchora is an Android app that downloads videos from X (Twitter) to your device. It works by forwarding the URL you share to a [Cobalt](https://cobalt.tools) instance, which resolves a direct video stream link. The video is then downloaded and saved to a folder you choose.

## Data we collect

**We do not collect any personal data.** Fetchora has no accounts, no sign-in, no analytics, and no tracking.

### Data stored on your device

The app stores the following information locally, only on your device:

| Data | Purpose |
|---|---|
| URLs of videos you downloaded | Download history screen |
| File path and name of saved videos | Download history screen |
| Download status and file size | Download history screen |
| Your chosen download folder | Remembering your preference across sessions |
| Cobalt instance URL | Remembering your preference across sessions |

This data never leaves your device except as described below.

### Data sent to a third-party service

When you initiate a download, the URL of the X/Twitter post is sent to the Cobalt instance configured in the app (by default `https://api.cobalt.tools/`). Fetchora does not control that service. Refer to [Cobalt's privacy policy](https://cobalt.tools/about) for details on how they handle requests.

If you configure a self-hosted Cobalt instance, your requests go to that server instead.

## Permissions

| Permission | Why it is needed |
|---|---|
| `INTERNET` | To contact the Cobalt API and download the video file |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` | To keep the download running while the app is in the background |
| `POST_NOTIFICATIONS` | To show download progress and completion notifications |

## Data sharing

Fetchora does not share any data with advertisers, analytics providers, or any third party other than the Cobalt instance described above.

## Data deletion

All data is stored locally on your device. You can delete it at any time by clearing the app's storage in Android Settings, or by uninstalling the app.

## Children

Fetchora does not knowingly collect any information from children under 13.

## Contact

If you have questions about this policy, contact: cyrille.sondag@gmail.com
