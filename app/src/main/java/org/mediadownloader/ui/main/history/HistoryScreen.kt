package org.mediadownloader.ui.main.history

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import org.mediadownloader.R
import org.mediadownloader.data.local.db.DownloadEntity
import org.mediadownloader.data.local.db.DownloadStatus

import androidx.compose.ui.tooling.preview.Preview
import org.mediadownloader.ui.theme.FetchoraTheme

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    HistoryScreenContent(downloads)
}

@Composable
fun HistoryScreenContent(downloads: List<DownloadEntity>) {
    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.history_empty_state_message),
                style = MaterialTheme.typography.bodyLarge
            )
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
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.status == DownloadStatus.COMPLETED && item.filePath.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.filePath)
                            .videoFrameMillis(1000)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val statusText = when (item.status) {
                    DownloadStatus.DOWNLOADING -> "Downloading…"
                    DownloadStatus.COMPLETED   -> "Done · ${Formatter.formatShortFileSize(context, item.fileSizeBytes)}"
                    DownloadStatus.FAILED      -> "Failed"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.status == DownloadStatus.FAILED)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    FetchoraTheme {
        HistoryScreenContent(
            downloads = listOf(
                DownloadEntity(
                    id = "1",
                    tweetUrl = "https://x.com/user/status/1",
                    videoUrl = "https://video.twimg.com/1.mp4",
                    fileName = "video_1.mp4",
                    filePath = "/storage/emulated/0/Download/video_1.mp4",
                    fileSizeBytes = 1024 * 1024 * 5,
                    status = DownloadStatus.COMPLETED
                ),
                DownloadEntity(
                    id = "2",
                    tweetUrl = "https://x.com/user/status/2",
                    videoUrl = "https://video.twimg.com/2.mp4",
                    fileName = "video_2.mp4",
                    filePath = "",
                    fileSizeBytes = 0,
                    status = DownloadStatus.DOWNLOADING
                ),
                DownloadEntity(
                    id = "3",
                    tweetUrl = "https://x.com/user/status/3",
                    videoUrl = "https://video.twimg.com/3.mp4",
                    fileName = "video_3.mp4",
                    filePath = "",
                    fileSizeBytes = 0,
                    status = DownloadStatus.FAILED
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenEmptyPreview() {
    FetchoraTheme {
        HistoryScreenContent(downloads = emptyList())
    }
}
