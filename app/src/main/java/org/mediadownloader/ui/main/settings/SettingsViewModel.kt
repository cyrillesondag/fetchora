package org.mediadownloader.ui.main.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.mediadownloader.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settings: SettingsDataStore) : ViewModel() {

    val cobaltUrl = settings.cobaltUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val folderUri = settings.folderUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onFolderSelected(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        viewModelScope.launch { settings.setFolderUri(uri.toString()) }
    }

    fun saveCobaltUrl(url: String) {
        viewModelScope.launch { settings.setCobaltUrl(url) }
    }
}
