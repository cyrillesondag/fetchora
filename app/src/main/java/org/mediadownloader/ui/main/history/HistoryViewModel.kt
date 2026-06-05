package org.mediadownloader.ui.main.history

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import org.mediadownloader.data.local.db.DownloadDao
import org.mediadownloader.data.local.db.DownloadEntity
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(dao: DownloadDao) : ViewModel() {
    val downloads: Flow<List<DownloadEntity>> = dao.flowAll()
}
