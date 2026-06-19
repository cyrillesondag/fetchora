package org.mediadownloader.ui.main

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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mediadownloader.data.local.datastore.SettingsDataStore

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var settings: SettingsDataStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settings = mockk()
        every { settings.onboardingDone } returns flowOf(false)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `onboardingDone reflects DataStore value`() = runTest {
        val viewModel = MainViewModel(settings)
        advanceUntilIdle()
        assertFalse(viewModel.onboardingDone.value!!)
    }

    @Test
    fun `completeOnboarding calls setOnboardingDone`() = runTest {
        coEvery { settings.setOnboardingDone() } just Runs
        val viewModel = MainViewModel(settings)
        viewModel.completeOnboarding()
        advanceUntilIdle()
        coVerify { settings.setOnboardingDone() }
    }
}
