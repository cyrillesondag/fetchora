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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        every { settings.cobaltApiKey } returns flowOf("my-key")
        val viewModel = SettingsViewModel(settings, repository)
        advanceUntilIdle()
        assertEquals("my-key", viewModel.cobaltApiKey.value)
    }

    @Test
    fun `saveApiKey delegates to settings`() = runTest {
        coEvery { settings.setCobaltApiKey(any()) } just Runs
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.saveApiKey("new-key")
        advanceUntilIdle()
        coVerify { settings.setCobaltApiKey("new-key") }
    }

    @Test
    fun `clearServerInfo sets serverInfoState to Idle`() = runTest {
        val viewModel = SettingsViewModel(settings, repository)
        advanceUntilIdle()
        viewModel.clearServerInfo()
        assertEquals(ServerInfoState.Idle, viewModel.serverInfoState.value)
    }

    @Test
    fun `reloadServerInfo sets Loading then Success on success`() = runTest {
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.clearServerInfo()
        viewModel.reloadServerInfo()
        advanceUntilIdle()
        assertTrue(viewModel.serverInfoState.value is ServerInfoState.Success)
    }

    @Test
    fun `reloadServerInfo sets Error on failure`() = runTest {
        coEvery { repository.getInfo(any()) } returns Result.failure(Exception("timeout"))
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.clearServerInfo()
        viewModel.reloadServerInfo()
        advanceUntilIdle()
        val state = viewModel.serverInfoState.value
        assertTrue(state is ServerInfoState.Error)
        assertEquals("timeout", (state as ServerInfoState.Error).message)
    }

    @Test
    fun `testAndSave saves URL and sets Success on success`() = runTest {
        coEvery { settings.setCobaltUrl(any()) } just Runs
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.testAndSave("https://new.example.com/")
        advanceUntilIdle()
        coVerify { settings.setCobaltUrl("https://new.example.com/") }
        assertTrue(viewModel.serverInfoState.value is ServerInfoState.Success)
    }

    @Test
    fun `testAndSave does not save URL and sets Error on failure`() = runTest {
        coEvery { repository.getInfo("https://bad.example.com/") } returns Result.failure(Exception("unreachable"))
        val viewModel = SettingsViewModel(settings, repository)
        viewModel.testAndSave("https://bad.example.com/")
        advanceUntilIdle()
        coVerify(exactly = 0) { settings.setCobaltUrl(any()) }
        assertTrue(viewModel.serverInfoState.value is ServerInfoState.Error)
    }
}
