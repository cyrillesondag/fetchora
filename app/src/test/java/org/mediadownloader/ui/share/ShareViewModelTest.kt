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
        // subscribe BEFORE download() so no emission is missed
        val job = launch { viewModel.finishEvents.collect { events.add(it) } }
        runCurrent()
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
        runCurrent()
        viewModel.download("https://x.com/status/1", VideoVariant("https://video.mp4", "720p"))
        advanceUntilIdle()

        assertEquals(1, events.size)
        job.cancel()
    }

    @Test
    fun `WorkInfo CANCELLED emits finishEvent`() = runTest {
        val info = mockk<WorkInfo>()
        every { info.state } returns WorkInfo.State.CANCELLED
        every { info.progress } returns Data.EMPTY
        every { workManager.getWorkInfoByIdFlow(any()) } returns flowOf(info)

        val events = mutableListOf<Unit>()
        val job = launch { viewModel.finishEvents.collect { events.add(it) } }
        runCurrent()
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
