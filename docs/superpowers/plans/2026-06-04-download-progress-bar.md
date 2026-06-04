# Download Progress Bar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a live 0–100 % progress bar inside the BottomSheet while a video downloads, then auto-close the sheet when the download finishes or fails.

**Architecture:** `DownloadWorker` emits progress via `setProgress(workDataOf(KEY_PERCENT to N))`; `ShareViewModel` observes `WorkManager.getWorkInfoByIdFlow(id)` and pushes `Downloading(progress)` state updates; `QualityBottomSheet` renders a `LinearProgressIndicator`; `ShareReceiverActivity` collects a one-shot `SharedFlow<Unit>` (`finishEvents`) and calls `finish()`.

**Tech Stack:** Kotlin, Jetpack Compose, WorkManager 2.10.1 (Progress API), Hilt 2.59.2, MockK, kotlinx-coroutines-test.

---

## File map

| File | Change |
|---|---|
| `app/src/main/java/org/mediadownloader/di/AppModule.kt` | Add `@Provides WorkManager` |
| `app/src/main/java/org/mediadownloader/worker/DownloadWorker.kt` | Add `KEY_PERCENT` + `setProgress()` call |
| `app/src/main/java/org/mediadownloader/ui/share/ShareViewModel.kt` | `Downloading(progress: Int)`, inject `WorkManager`, `finishEvents` flow |
| `app/src/main/java/org/mediadownloader/ui/share/QualityBottomSheet.kt` | `LinearProgressIndicator` + non-dismissable during download |
| `app/src/main/java/org/mediadownloader/ui/share/ShareReceiverActivity.kt` | `LaunchedEffect` collecting `finishEvents` → `finish()` |
| `app/src/test/java/org/mediadownloader/ui/share/ShareViewModelTest.kt` | New tests: progress update, finish on SUCCEEDED/FAILED |

---

### Task 1: Provide WorkManager via Hilt + emit setProgress in DownloadWorker

**Goal:** Make WorkManager injectable and have DownloadWorker report download progress via the WorkManager Progress API.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/di/AppModule.kt`
- Modify: `app/src/main/java/org/mediadownloader/worker/DownloadWorker.kt`

**Acceptance Criteria:**
- [ ] `WorkManager` is provided as a `@Singleton` in the Hilt graph
- [ ] `DownloadWorker` companion has `const val KEY_PERCENT = "percent"`
- [ ] `setProgress(workDataOf(KEY_PERCENT to percent))` is called inside the download loop whenever `totalBytes > 0`

**Verify:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

**Steps:**

- [ ] **Step 1: Add WorkManager provider to AppModule**

Replace the content of `app/src/main/java/org/mediadownloader/di/AppModule.kt`:

```kotlin
package org.mediadownloader.di

import android.content.Context
import androidx.work.WorkManager
import org.mediadownloader.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
```

- [ ] **Step 2: Add KEY_PERCENT constant to DownloadWorker companion**

In `app/src/main/java/org/mediadownloader/worker/DownloadWorker.kt`, add inside the `companion object` block (after `KEY_FOLDER_URI`):

```kotlin
const val KEY_PERCENT = "percent"
```

- [ ] **Step 3: Call setProgress inside the download loop**

In `DownloadWorker.doWork()`, find the block inside the `while` loop that already computes `percent`:

```kotlin
if (totalBytes > 0) {
    val percent = (downloaded * 100 / totalBytes).toInt()
    setForeground(ForegroundInfo(
        downloadId.hashCode(),
        notificationHelper.buildProgress(downloadId.hashCode(), percent)
    ))
}
```

Replace it with:

```kotlin
if (totalBytes > 0) {
    val percent = (downloaded * 100 / totalBytes).toInt()
    setProgress(workDataOf(KEY_PERCENT to percent))
    setForeground(ForegroundInfo(
        downloadId.hashCode(),
        notificationHelper.buildProgress(downloadId.hashCode(), percent)
    ))
}
```

- [ ] **Step 4: Verify compilation**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

---

### Task 2: ShareViewModel — Downloading(progress), WorkInfo observation, finishEvents

**Goal:** Refactor `ShareViewModel` so it injects `WorkManager`, exposes `Downloading(progress: Int)` state updated from WorkInfo, and emits a one-shot `finishEvents` flow on terminal states.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/ui/share/ShareViewModel.kt`
- Modify: `app/src/test/java/org/mediadownloader/ui/share/ShareViewModelTest.kt`

**Acceptance Criteria:**
- [ ] `ShareUiState.Downloading` is a `data class` with `val progress: Int`
- [ ] `ShareViewModel` receives `WorkManager` via constructor injection
- [ ] `download()` no longer takes a `Context` parameter
- [ ] `_uiState` is set to `Downloading(0)` immediately when `download()` is called
- [ ] `getWorkInfoByIdFlow(request.id)` is collected; each emission updates `Downloading(pct)` and emits `finishEvents` on terminal state
- [ ] `finishEvents: SharedFlow<Unit>` is exposed on the ViewModel
- [ ] All existing + new tests pass

**Verify:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests "org.mediadownloader.ui.share.ShareViewModelTest"` → BUILD SUCCESSFUL, 0 failures

**Steps:**

- [ ] **Step 1: Write failing tests first**

Replace the content of `app/src/test/java/org/mediadownloader/ui/share/ShareViewModelTest.kt`:

```kotlin
package org.mediadownloader.ui.share

import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.VideoVariant
import org.mediadownloader.worker.DownloadWorker
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: CobaltRepository
    private lateinit var settings: SettingsDataStore
    private lateinit var workManager: WorkManager
    private lateinit var viewModel: ShareViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settings = mockk()
        workManager = mockk()
        coEvery { settings.folderUri } returns flowOf("content://com.android.externalstorage/tree/primary")
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk(relaxed = true)
        viewModel = ShareViewModel(repository, settings, workManager)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.uiState.value is ShareUiState.Idle)
    }

    @Test
    fun `loadVariants emits Loaded on success`() = runTest {
        val variants = listOf(
            VideoVariant("https://video.twimg.com/1280x720/v.mp4", "720p"),
            VideoVariant("https://video.twimg.com/854x480/v.mp4", "480p")
        )
        coEvery { repository.getVariants(any()) } returns Result.success(variants)
        viewModel.loadVariants("https://x.com/user/status/1")
        val state = viewModel.uiState.value
        assertTrue(state is ShareUiState.Loaded)
        assertEquals(2, (state as ShareUiState.Loaded).variants.size)
    }

    @Test
    fun `loadVariants emits Error on failure`() = runTest {
        coEvery { repository.getVariants(any()) } returns Result.failure(Exception("error.api.link.invalid"))
        viewModel.loadVariants("https://x.com/user/status/2")
        val state = viewModel.uiState.value
        assertTrue(state is ShareUiState.Error)
        assertTrue((state as ShareUiState.Error).message.contains("error.api.link.invalid"))
    }

    @Test
    fun `download sets Downloading(0) immediately`() = runTest {
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf()
        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))
        assertEquals(ShareUiState.Downloading(0), viewModel.uiState.value)
    }

    @Test
    fun `WorkInfo RUNNING with progress updates uiState to Downloading with percent`() = runTest {
        val info = mockk<WorkInfo>()
        every { info.state } returns WorkInfo.State.RUNNING
        every { info.progress } returns workDataOf(DownloadWorker.KEY_PERCENT to 65)
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf(info)

        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))
        advanceUntilIdle()

        assertEquals(ShareUiState.Downloading(65), viewModel.uiState.value)
    }

    @Test
    fun `WorkInfo SUCCEEDED emits finishEvent`() = runTest {
        val info = mockk<WorkInfo>()
        every { info.state } returns WorkInfo.State.SUCCEEDED
        every { info.progress } returns Data.EMPTY
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf(info)

        val events = mutableListOf<Unit>()
        val job = launch { viewModel.finishEvents.collect { events.add(it) } }

        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))
        advanceUntilIdle()

        assertEquals(1, events.size)
        job.cancel()
    }

    @Test
    fun `WorkInfo FAILED emits finishEvent`() = runTest {
        val info = mockk<WorkInfo>()
        every { info.state } returns WorkInfo.State.FAILED
        every { info.progress } returns Data.EMPTY
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf(info)

        val events = mutableListOf<Unit>()
        val job = launch { viewModel.finishEvents.collect { events.add(it) } }

        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))
        advanceUntilIdle()

        assertEquals(1, events.size)
        job.cancel()
    }

    @Test
    fun `download with no folder set emits Error`() = runTest {
        coEvery { settings.folderUri } returns flowOf(null)
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf()

        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))

        val state = viewModel.uiState.value
        assertTrue(state is ShareUiState.Error)
        assertTrue((state as ShareUiState.Error).message.contains("Settings"))
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests "org.mediadownloader.ui.share.ShareViewModelTest" 2>&1 | tail -20
```

Expected: compilation error — `ShareViewModel` doesn't have a `WorkManager` parameter yet, `finishEvents` not found, `Downloading` is not a data class.

- [ ] **Step 3: Rewrite ShareViewModel**

Replace the content of `app/src/main/java/org/mediadownloader/ui/share/ShareViewModel.kt`:

```kotlin
package org.mediadownloader.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.VideoVariant
import org.mediadownloader.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    data class Loaded(val variants: List<VideoVariant>) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
    data class Downloading(val progress: Int) : ShareUiState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: CobaltRepository,
    private val settings: SettingsDataStore,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState

    private val _finishEvents = MutableSharedFlow<Unit>()
    val finishEvents: SharedFlow<Unit> = _finishEvents.asSharedFlow()

    fun loadVariants(tweetUrl: String) {
        viewModelScope.launch {
            _uiState.value = ShareUiState.Loading
            repository.getVariants(tweetUrl).fold(
                onSuccess = { variants ->
                    _uiState.value = if (variants.isEmpty())
                        ShareUiState.Error("No video found in this tweet")
                    else
                        ShareUiState.Loaded(variants)
                },
                onFailure = { _uiState.value = ShareUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun download(tweetUrl: String, variant: VideoVariant) {
        viewModelScope.launch {
            val folderUri = settings.folderUri.first()
                ?: run {
                    _uiState.value = ShareUiState.Error("No download folder set. Open Settings.")
                    return@launch
                }
            val downloadId = UUID.randomUUID().toString()
            val fileName = "xvideo_${System.currentTimeMillis()}.mp4"
            val request = DownloadWorker.buildRequest(variant.url, tweetUrl, fileName, downloadId, folderUri)
            _uiState.value = ShareUiState.Downloading(0)
            workManager.enqueue(request)
            workManager.getWorkInfoByIdFlow(request.id)
                .onEach { info ->
                    val pct = info.progress.getInt(DownloadWorker.KEY_PERCENT, 0)
                    _uiState.value = ShareUiState.Downloading(pct)
                    if (info.state.isFinished) _finishEvents.emit(Unit)
                }
                .launchIn(viewModelScope)
        }
    }
}
```

- [ ] **Step 4: Run tests — expect all passing**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests "org.mediadownloader.ui.share.ShareViewModelTest"
```

Expected: `BUILD SUCCESSFUL` — 8 tests, 0 failures.

---

### Task 3: QualityBottomSheet — LinearProgressIndicator + non-dismissable

**Goal:** Replace the "Download started!" placeholder with a live `LinearProgressIndicator` and prevent sheet dismissal during download.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/ui/share/QualityBottomSheet.kt`

**Acceptance Criteria:**
- [ ] When state is `Downloading(0)`, sheet shows an indeterminate `LinearProgressIndicator`
- [ ] When state is `Downloading(N)` where N > 0, sheet shows a determinate `LinearProgressIndicator` at `N / 100f`
- [ ] Percentage text is displayed: e.g. "Downloading… 65 %"
- [ ] Sheet cannot be swiped away or dismissed by back press during `Downloading` state
- [ ] Sheet still dismisses normally in `Loaded` / `Error` states

**Verify:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL

**Steps:**

- [ ] **Step 1: Rewrite QualityBottomSheet**

Replace the content of `app/src/main/java/org/mediadownloader/ui/share/QualityBottomSheet.kt`:

```kotlin
package org.mediadownloader.ui.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import org.mediadownloader.data.remote.model.VideoVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    state: ShareUiState,
    onDismiss: () -> Unit,
    onDownload: (VideoVariant) -> Unit
) {
    val isDownloading = state is ShareUiState.Downloading
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isDownloading }
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Download Video", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when (state) {
                is ShareUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ShareUiState.Loaded -> {
                    var selected by remember { mutableStateOf(state.variants.first()) }

                    state.variants.forEach { variant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = variant == selected, onClick = { selected = variant })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = variant == selected, onClick = { selected = variant })
                            Spacer(Modifier.width(8.dp))
                            Text(variant.qualityLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onDownload(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Download")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
                is ShareUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
                is ShareUiState.Downloading -> {
                    Text(
                        text = if (state.progress == 0) "Downloading…" else "Downloading… ${state.progress} %",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.progress == 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                else -> {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`

---

### Task 4: ShareReceiverActivity — collect finishEvents + update download() call

**Goal:** Wire the `finishEvents` flow in `ShareReceiverActivity` so the Activity auto-closes when the download finishes or fails. Also remove the now-gone `context` parameter from `download()`.

**Files:**
- Modify: `app/src/main/java/org/mediadownloader/ui/share/ShareReceiverActivity.kt`

**Acceptance Criteria:**
- [ ] A `LaunchedEffect` collects `viewModel.finishEvents` and calls `finish()`
- [ ] `onDownload` lambda calls `viewModel.download(tweetUrl, variant)` without passing `this`
- [ ] All existing tests still pass

**Verify:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL, 0 failures

**Steps:**

- [ ] **Step 1: Rewrite ShareReceiverActivity**

Replace the content of `app/src/main/java/org/mediadownloader/ui/share/ShareReceiverActivity.kt`:

```kotlin
package org.mediadownloader.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.mediadownloader.ui.theme.XDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        val tweetUrl = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { extractUrl(it) }

        if (tweetUrl == null) { finish(); return }

        viewModel.loadVariants(tweetUrl)

        setContent {
            XDownloaderTheme {
                LaunchedEffect(Unit) {
                    viewModel.finishEvents.collect { finish() }
                }

                val state by viewModel.uiState.collectAsState()
                QualityBottomSheet(
                    state = state,
                    onDismiss = { finish() },
                    onDownload = { variant -> viewModel.download(tweetUrl, variant) }
                )
            }
        }
    }

    private fun extractUrl(text: String): String? =
        Regex("""https?://[^\s]+""").find(text)?.value
}
```

- [ ] **Step 2: Run full test suite**

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, 0 test failures.

- [ ] **Step 3: Commit everything**

```bash
git add \
  app/src/main/java/org/mediadownloader/di/AppModule.kt \
  app/src/main/java/org/mediadownloader/worker/DownloadWorker.kt \
  app/src/main/java/org/mediadownloader/ui/share/ShareViewModel.kt \
  app/src/main/java/org/mediadownloader/ui/share/QualityBottomSheet.kt \
  app/src/main/java/org/mediadownloader/ui/share/ShareReceiverActivity.kt \
  app/src/test/java/org/mediadownloader/ui/share/ShareViewModelTest.kt \
  docs/superpowers/specs/2026-06-04-download-progress-bar-design.md \
  docs/superpowers/plans/2026-06-04-download-progress-bar.md \
  docs/superpowers/plans/2026-06-04-download-progress-bar.md.tasks.json
git commit -m "feat: live download progress bar in BottomSheet"
```
