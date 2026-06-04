# Download Progress Bar in BottomSheet — Design

**Date:** 2026-06-04  
**Status:** Approved  
**Feature:** In-BottomSheet download progress bar using WorkManager Progress API

---

## Overview

After the user selects a video quality and taps "Download", the BottomSheet remains open and displays a live progress bar (0–100 %) fed by the `DownloadWorker` via the WorkManager Progress API. When the download completes successfully, the BottomSheet closes automatically. If the download fails, the BottomSheet also closes and the existing failure notification is shown.

---

## Architecture

**Approach:** WorkManager Progress API — the worker emits progress via `setProgress()`, the ViewModel observes `WorkInfo` via `WorkManager.getWorkInfoByIdFlow()`.

### Data flow

```
DownloadWorker
  └── setProgress(workDataOf("percent" to N))   ← every chunk
  └── Result.success() / Result.failure()       ← on completion

ShareViewModel
  └── getWorkInfoByIdFlow(uuid).collect { info →
        info.progress.getInt("percent", 0)      → Downloading(progress = N)
        info.state == SUCCEEDED                 → emit finish event
        info.state == FAILED                    → emit finish event
      }

QualityBottomSheet
  └── ShareUiState.Downloading(progress) → LinearProgressIndicator
  └── finish event → Activity.finish()
```

### One-shot finish event

`ShareViewModel` exposes a `SharedFlow<Unit>` (`finishEvents`) for navigation side-effects. `ShareReceiverActivity` collects this flow in a `LaunchedEffect` and calls `finish()`. This prevents the finish from being re-triggered on recomposition.

---

## Changes per file

| File | Change |
|---|---|
| `worker/DownloadWorker.kt` | Add `setProgress(workDataOf(KEY_PERCENT to percent))` inside the download loop |
| `ui/share/ShareViewModel.kt` | Change `object Downloading` → `data class Downloading(val progress: Int)`. Store `workRequestId: UUID`, observe `WorkInfo` flow, update `Downloading(progress)` state, emit `finishEvents` on terminal state |
| `ui/share/QualityBottomSheet.kt` | Render `LinearProgressIndicator` + percentage text for `Downloading` state; disable sheet swipe-to-dismiss during download |
| `ui/share/ShareReceiverActivity.kt` | Collect `finishEvents` in a `LaunchedEffect` and call `finish()` |

No new files. No database migration required.

---

## UI Specification

### Downloading state in BottomSheet

```
┌─────────────────────────────────┐
│  Download Video                  │
│                                  │
│  Downloading… 65 %               │
│  ████████████░░░░░░░░            │
│                                  │
└─────────────────────────────────┘
```

- `LinearProgressIndicator` — full width, Material 3
- **Indeterminate** when `progress == 0` (before first chunk arrives)
- **Determinate** (`progress / 100f`) once first chunk received
- BottomSheet is **not dismissable** during download (swipe disabled)
- No Cancel button in the sheet (WorkManager cancellation available via system notification)

### State transitions

| From | Event | To |
|---|---|---|
| `Loaded` | User taps Download | `Downloading(0)` |
| `Downloading(N)` | Worker emits percent | `Downloading(N)` |
| `Downloading` | `WorkInfo.state == SUCCEEDED` | emit `finishEvent` → Activity.finish() |
| `Downloading` | `WorkInfo.state == FAILED` | emit `finishEvent` → Activity.finish() |

---

## Key: WorkManager Progress

```kotlin
// In DownloadWorker — inside the download loop
val percent = (downloaded * 100 / totalBytes).toInt()
setProgress(workDataOf(KEY_PERCENT to percent))

// In ShareViewModel — after enqueue
val id = workRequest.id
WorkManager.getInstance(appContext)
    .getWorkInfoByIdFlow(id)
    .onEach { info ->
        val pct = info.progress.getInt(DownloadWorker.KEY_PERCENT, 0)
        _uiState.value = ShareUiState.Downloading(pct)
        if (info.state.isFinished) _finishEvents.emit(Unit)
    }
    .launchIn(viewModelScope)
```

---

## Out of Scope

- Cancel button inside the BottomSheet
- Progress bar in the History screen (separate feature)
- Retry on failure from the BottomSheet
