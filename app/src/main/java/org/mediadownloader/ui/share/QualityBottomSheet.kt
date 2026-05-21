package org.mediadownloader.ui.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mediadownloader.data.remote.model.VideoVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    state: ShareUiState,
    onDismiss: () -> Unit,
    onDownload: (VideoVariant) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Download Video", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when (state) {
                is ShareUiState.Loading -> {
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
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
                is ShareUiState.Downloading -> {
                    Text("Download started! Check your notifications.")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
                else -> {}
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
