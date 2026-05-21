package org.mediadownloader.ui.main.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.mediadownloader.data.local.db.DownloadEntity
import org.mediadownloader.data.local.db.DownloadStatus

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())

    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads yet.\nShare a video from X to get started.",
                style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(downloads) { item -> DownloadCard(item) }
    }
}

@Composable
private fun DownloadCard(item: DownloadEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.fileName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            val statusText = when (item.status) {
                DownloadStatus.DOWNLOADING -> "Downloading…"
                DownloadStatus.COMPLETED   -> "Done · ${item.fileSizeBytes / 1024} KB"
                DownloadStatus.FAILED      -> "Failed"
            }
            Text(statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status == DownloadStatus.FAILED)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
