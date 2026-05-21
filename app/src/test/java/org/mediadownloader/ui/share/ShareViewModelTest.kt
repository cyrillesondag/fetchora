package org.mediadownloader.ui.share

import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.VideoVariant
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var viewModel: ShareViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settings = mockk()
        coEvery { settings.folderUri } returns flowOf("file:///tmp")
        viewModel = ShareViewModel(repository, settings)
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
}
