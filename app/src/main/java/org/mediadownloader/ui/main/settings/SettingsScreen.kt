package org.mediadownloader.ui.main.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val cobaltUrl by viewModel.cobaltUrl.collectAsState()
    val folderUri by viewModel.folderUri.collectAsState()
    var cobaltUrlDraft by remember(cobaltUrl) { mutableStateOf(cobaltUrl) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { viewModel.onFolderSelected(context, it) } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Download folder", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Text(folderUri ?: "No folder selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Change Folder") }

        Spacer(Modifier.height(24.dp))
        Text("Cobalt instance URL", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = cobaltUrlDraft,
            onValueChange = { cobaltUrlDraft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.saveCobaltUrl(cobaltUrlDraft) },
            modifier = Modifier.fillMaxWidth()) { Text("Save") }
    }
}
