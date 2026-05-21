package org.mediadownloader.ui.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.data.remote.CobaltRepository
import org.mediadownloader.data.remote.model.VideoVariant
import org.mediadownloader.worker.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    data class Loaded(val variants: List<VideoVariant>) : ShareUiState()
    data class Error(val message: String) : ShareUiState()
    object Downloading : ShareUiState()
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: CobaltRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Idle)
    val uiState: StateFlow<ShareUiState> = _uiState

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

    fun download(context: Context, tweetUrl: String, variant: VideoVariant) {
        viewModelScope.launch {
            val folderUri = settings.folderUri.first()
                ?: run {
                    _uiState.value = ShareUiState.Error("No download folder set. Open Settings.")
                    return@launch
                }
            val downloadId = UUID.randomUUID().toString()
            val fileName = "xvideo_${System.currentTimeMillis()}.mp4"
            WorkManager.getInstance(context).enqueue(
                DownloadWorker.buildRequest(variant.url, tweetUrl, fileName, downloadId, folderUri)
            )
            _uiState.value = ShareUiState.Downloading
        }
    }
}
