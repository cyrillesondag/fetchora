# Twitter/X Video Downloader — Android App Design

**Date:** 2026-05-21  
**Status:** Approved  
**Distribution:** Play Store (public)  
**Min SDK:** Android 10 (API 29)

---

## Overview

An Android application that integrates with the system share intent to receive X (Twitter) links and download the associated videos to a user-configured folder. Video URL extraction is delegated to the Cobalt API. The user selects the desired quality before the download begins.

---

## Architecture

**Stack:** Kotlin, Jetpack Compose, MVVM, Hilt (dependency injection)

**Key components:**

| Component | Role |
|---|---|
| `ShareReceiverActivity` | Transparent entry point for share intents; hosts the quality-selection BottomSheet |
| `CobaltRepository` | Retrofit-based network layer; calls the Cobalt API and parses quality variants |
| `DownloadWorker` | WorkManager worker; downloads the video file via OkHttp with progress reporting |
| `SettingsDataStore` | DataStore<Preferences>; persists download folder URI and Cobalt instance URL |
| `DownloadDao` / Room | Stores download history (URL, filename, file path, size, status, timestamp) |
| `MainActivity` | Shown when the app is launched directly; hosts History and Settings tabs |

**Dependency graph:**

```
ShareReceiverActivity
  └── ShareViewModel
        └── CobaltRepository (Retrofit)
        └── DownloadWorker (WorkManager + OkHttp)
              └── SettingsDataStore
              └── DownloadDao

MainActivity
  └── HistoryViewModel
        └── DownloadDao
  └── SettingsViewModel
        └── SettingsDataStore
```

---

## User Flow

```
[X App] → Share → [ShareReceiverActivity] (transparent, BottomSheet visible)
    → Call Cobalt API (spinner ~1-2s)
    ↓
    [Single quality] → skip dialog, start download immediately (toast notification)
    [Multiple qualities] → show quality list (radio buttons)
        → user taps "Download"
    ↓
    [DownloadWorker enqueued]
    → Foreground notification with progress bar + Cancel button
    → File written to configured folder (SAF URI)
    → Success notification with "Open" action
    → Row inserted in download history (Room)
```

---

## UI Specification

### ShareReceiverActivity

- Activity theme: transparent background, no title bar
- A `ModalBottomSheet` rises from the bottom while X remains visible behind
- **Loading state:** circular progress indicator, message "Fetching video info…"
- **Quality selection state:**
  - Tweet thumbnail (if provided by Cobalt)
  - Radio button list of available qualities (e.g. `1080p`, `720p`, `480p`, `360p`)
  - "Download" primary button + "Cancel" text button
- **Error state:** descriptive message + contextual action (Retry / Open Settings)
- Dismissing the sheet (swipe down or Cancel) cancels any pending network call

### MainActivity

Two bottom-navigation tabs:

**History tab:**
- `LazyColumn` of download cards: thumbnail, filename, file size, status badge (Downloading / Done / Error)
- Tap a completed item → opens the video with the system media player
- Long-press → contextual menu: Delete file, Remove from history, Share file
- Empty state illustration when no downloads yet

**Settings tab:**
- **Download folder** — displays current folder path; tapping opens the system folder picker (SAF `ACTION_OPEN_DOCUMENT_TREE`); persisted as a URI with persistent permissions
- **Cobalt instance URL** — text field, defaults to `https://api.cobalt.tools/`; validated as a well-formed HTTPS URL on save
- **About** — app version, link to Cobalt project, copyright disclaimer

### Notifications

- Channel: "Downloads" (importance: DEFAULT)
- In-progress: indeterminate → determinate progress bar, Cancel action
- Completed: "Video saved to [folder]", Open action
- Failed: "Download failed — [reason]", Retry action

---

## Cobalt API Integration

**Endpoint:** `POST {cobaltInstanceUrl}`  
**Headers:** `Content-Type: application/json`, `Accept: application/json`

**Request body:**
```json
{ "url": "https://x.com/user/status/123456789" }
```

**Response — picker (multiple qualities):**
```json
{
  "status": "picker",
  "picker": [
    { "url": "https://...", "type": "video" },
    { "url": "https://...", "type": "video" }
  ]
}
```

**Response — single quality:**
```json
{
  "status": "stream",
  "url": "https://..."
}
```

**Response — error:**
```json
{
  "status": "error",
  "error": { "code": "error.api.link.invalid" }
}
```

Quality labels for `picker` items are extracted from the video URL path (e.g. `vid/1280x720/` → `720p`). If the resolution cannot be parsed from the URL, items are labeled `Quality 1`, `Quality 2`, etc. in the order returned by Cobalt. The highest-resolution option is pre-selected by default.

---

## Error Handling

| Situation | Behavior |
|---|---|
| No network at share time | Error state in BottomSheet: "No internet connection" + Retry |
| Cobalt returns 5xx | "Service unavailable — try changing the Cobalt instance in Settings" + Open Settings |
| Tweet has no video | "This tweet does not contain a video" — no retry |
| Invalid URL passed | "Invalid link" — dismissed automatically after 3s |
| Download interrupted | WorkManager retries once automatically |
| Configured folder inaccessible | Error notification + "Open Settings" action |
| Cobalt picker returns 0 items | Treated as "no video" error |

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Network calls (Cobalt API, video download) |
| `POST_NOTIFICATIONS` | Download progress and completion notifications (Android 13+ runtime request) |
| SAF persistent URI permission | Write access to the user-chosen download folder — no `WRITE_EXTERNAL_STORAGE` needed |

---

## Data Model (Room)

```kotlin
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,       // UUID
    val tweetUrl: String,
    val videoUrl: String,
    val fileName: String,
    val filePath: String,             // SAF URI string
    val fileSizeBytes: Long,
    val status: DownloadStatus,       // DOWNLOADING, COMPLETED, FAILED
    val createdAt: Long               // epoch millis
)

enum class DownloadStatus { DOWNLOADING, COMPLETED, FAILED }
```

---

## Play Store Considerations

- App description must clarify that users are responsible for complying with X's Terms of Service and applicable copyright law.
- No user authentication with X/Twitter is required.
- The app does not store or transmit tweet content beyond what is needed for the download.
- Target SDK: Android 15 (API 35) to satisfy Play Store requirements.

---

## Out of Scope

- Audio-only downloads
- Batch downloads (multiple URLs queued)
- In-app video player
- Login with X account
- GIF downloads (separate format, deferred)
