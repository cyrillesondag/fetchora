package org.mediadownloader.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mediadownloader.data.local.datastore.SettingsDataStore
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val settings: SettingsDataStore) : ViewModel() {

    private val _onboardingDone = MutableStateFlow<Boolean?>(null)
    val onboardingDone: StateFlow<Boolean?> = _onboardingDone.asStateFlow()

    init {
        viewModelScope.launch {
            settings.onboardingDone.collect { value ->
                _onboardingDone.value = value
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }
}
