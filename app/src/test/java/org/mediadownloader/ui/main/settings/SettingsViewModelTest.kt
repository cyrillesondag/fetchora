package org.mediadownloader.ui.main.settings

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.CobaltInfo
import org.mediadownloader.data.remote.model.GitInfo

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: SettingsDataStore
    private lateinit var repository: CobaltRepository

    private val fakeCobaltInfo = CobaltInfo(
        version = "10.0",
        url = "https://api.cobalt.tools/",
        startTime = 0L,
        services = emptyArray(),
        git = GitInfo(branch = "main", commit = "abc", remote = "origin")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settings = mockk()
        repository = mockk()
        every { settings.cobaltUrl } returns flowOf("https://api.cobalt.tools/")
        every { settings.cobaltApiKey } returns flowOf(null)
        every { settings.folderUri } returns flowOf(null)
        coEvery { repository.getInfo(any()) } returns Result.success(fakeCobaltInfo)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `cobaltApiKey StateFlow reflects settings value`() = runTest {
        // Create a new mock with the desired value before creating the ViewModel
        val cobaltApiKeyFlow = MutableStateFlow<String?>("my-key")
        val settingsWithKey = mockk<SettingsDataStore>()
        every { settingsWithKey.cobaltUrl } returns flowOf("https://api.cobalt.tools/")
        every { settingsWithKey.cobaltApiKey } returns cobaltApiKeyFlow
        every { settingsWithKey.folderUri } returns flowOf(null)
        coEvery { repository.getInfo(any()) } returns Result.success(fakeCobaltInfo)

        val viewModel = SettingsViewModel(settingsWithKey, repository)

        // Collect a value to trigger the upstream subscription
        val values = mutableListOf<String?>()
        val job = launch { viewModel.cobaltApiKey.collect { values.add(it) } }
        advanceUntilIdle()

        // The StateFlow should have the value from settings
        assertEquals("my-key", viewModel.cobaltApiKey.value)
        assertEquals(listOf("my-key"), values)

        job.cancel()
    }

    @Test
    fun `saveApiKey delegates to settings`() = runTest {
        coEvery { settings.setCobaltApiKey(any()) } just Runs
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.saveApiKey("new-key")
        advanceUntilIdle()
        coVerify { settings.setCobaltApiKey("new-key") }
    }
}
