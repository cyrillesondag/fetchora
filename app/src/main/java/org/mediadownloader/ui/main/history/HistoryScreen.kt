package org.mediadownloader.ui.main.history

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = {
            if (item.status == DownloadStatus.COMPLETED) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType("content://${item.filePath.toUri()}".toUri(), "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Ignore for now
                }
            }
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.fileName, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            val statusText = when (item.status) {
                DownloadStatus.DOWNLOADING -> "Downloading…"
                DownloadStatus.COMPLETED   -> "Done · ${Formatter.formatShortFileSize(context, item.fileSizeBytes)}"
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
