package org.mediadownloader.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
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
import kotlinx.coroutines.flow.asStateFlow
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
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _finishEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
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
        if (_uiState.value is ShareUiState.Downloading) return
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
                    if (info == null) return@onEach
                    if (info.state.isFinished) {
                        if (info.state == WorkInfo.State.SUCCEEDED) {
                            _uiState.value = ShareUiState.Downloading(100)
                        }
                        _finishEvents.emit(Unit)
                    } else {
                        val pct = info.progress.getInt(DownloadWorker.KEY_PERCENT, 0)
                        _uiState.value = ShareUiState.Downloading(pct)
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
