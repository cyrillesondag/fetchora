package org.mediadownloader.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mediadownloader.data.remote.model.VideoVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    state: ShareUiState,
    onDismiss: () -> Unit,
    onDownload: (VideoVariant) -> Unit,
    onRetry: () -> Unit
) {
    val isDownloading by rememberUpdatedState(state is ShareUiState.Downloading)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isDownloading }
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Download Video", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when (state) {
                is ShareUiState.Idle, is ShareUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ShareUiState.Loaded -> {
                    var selected by remember { mutableStateOf(state.variants.first()) }

                    state.variants.forEach { variant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = variant == selected, onClick = { selected = variant })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = variant == selected, onClick = { selected = variant })
                            Spacer(Modifier.width(8.dp))
                            Text(variant.qualityLabel, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { onDownload(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Download")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel")
                    }
                }
                is ShareUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
                is ShareUiState.Downloading -> {
                    Text(
                        text = if (state.progress == 0) "Downloading…" else "Downloading… ${state.progress} %",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.progress == 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
