package org.mediadownloader.ui.main.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
    private val cobaltRepository: CobaltRepository
) : ViewModel() {

    // Eagerly: WhileSubscribed stops collecting when there are no UI collectors, breaking unit tests.
    val cobaltUrl = settings.cobaltUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val cobaltApiKey = settings.cobaltApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val folderUri = settings.folderUri.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _serverInfoState = MutableStateFlow<ServerInfoState>(ServerInfoState.Idle)
    val serverInfoState = _serverInfoState.asStateFlow()

    init {
        reloadServerInfo()
    }

    fun onFolderSelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch { settings.setFolderUri(uri.toString()) }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch { settings.setCobaltApiKey(key) }
    }

    fun clearServerInfo() {
        _serverInfoState.value = ServerInfoState.Idle
    }

    fun reloadServerInfo() {
        viewModelScope.launch {
            _serverInfoState.value = ServerInfoState.Loading
            val url = settings.cobaltUrl.first()
            _serverInfoState.value = cobaltRepository.getInfo(url).fold(
                onSuccess = { ServerInfoState.Success(it) },
                onFailure = { ServerInfoState.Error(it.message ?: "Connection failed") }
            )
        }
    }

    fun testAndSave(url: String) {
        viewModelScope.launch {
            _serverInfoState.value = ServerInfoState.Loading
            cobaltRepository.getInfo(url).fold(
                onSuccess = {
                    settings.setCobaltUrl(url)
                    _serverInfoState.value = ServerInfoState.Success(it)
                },
                onFailure = {
                    _serverInfoState.value = ServerInfoState.Error(it.message ?: "Connection failed")
                }
            )
        }
    }
}
