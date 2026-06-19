package org.mediadownloader.ui.main.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel<SettingsViewModel>()) {
    val context = LocalContext.current
    val cobaltUrl by viewModel.cobaltUrl.collectAsState()
    val cobaltApiKey by viewModel.cobaltApiKey.collectAsState()
    val folderUri by viewModel.folderUri.collectAsState()
    val serverInfoState by viewModel.serverInfoState.collectAsState()
    SettingsContent(
        modifier = Modifier.fillMaxSize(),
        cobaltUrl = cobaltUrl,
        cobaltApiKey = cobaltApiKey,
        folderUri = folderUri,
        serverInfoState = serverInfoState,
        onFolderSelected = { uri -> viewModel.onFolderSelected(context, uri) },
        onTestAndSave = { url -> viewModel.testAndSave(url) },
        onSaveApiKey = { key -> viewModel.saveApiKey(key) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    cobaltUrl: String,
    cobaltApiKey: String?,
    folderUri: String?,
    serverInfoState: ServerInfoState,
    onFolderSelected: (Uri) -> Unit,
    onTestAndSave: (String) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var cobaltUrlDraft by remember(cobaltUrl) { mutableStateOf(cobaltUrl) }
    var apiKeyDraft by remember(cobaltApiKey) { mutableStateOf(cobaltApiKey ?: "") }
    val scrollState = rememberScrollState()

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { onFolderSelected(it) } }

    // Helper to extract a readable name from the URI
    val folderName = remember(folderUri) {
        folderUri?.let { uriString ->
            runCatching {
                val uri = uriString.toUri()
                val path = uri.path ?: ""
                path.substringAfterLast(':').substringAfterLast('/')
            }.getOrDefault("Selected Folder")
        } ?: "Not selected"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section: Storage
        SettingsSection(title = "Storage", icon = Icons.Default.Folder) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { /* Read-only */ },
                    modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
                    label = { Text("Download location") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    readOnly = true,
                    )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { folderPicker.launch(folderUri?.toUri()) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select")
                    }
                }
            }
        }


        // Section: API Configuration
        SettingsSection(title = "Service Configuration", icon = Icons.Default.Link) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cobaltUrlDraft,
                    onValueChange = { cobaltUrlDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cobalt Instance URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLoading = serverInfoState is ServerInfoState.Loading

                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onTestAndSave(cobaltUrlDraft) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test & Save")
                        }
                    }
                }

                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key (optionnel)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = PasswordVisualTransformation(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onSaveApiKey(apiKeyDraft) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }

        // Section: Server Info
        SettingsSection(title = "Information", icon = Icons.Default.Settings) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    when (serverInfoState) {
                        is ServerInfoState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "Version",
                                    value = serverInfoState.info.version
                                )
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "Status",
                                    value = "Connected"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val servicesBuffer = StringBuilder()
                                serverInfoState.info.services.joinTo(buffer = servicesBuffer, separator = ", ")
                                InfoColumn(
                                    modifier = Modifier.weight(1f),
                                    label = "services",
                                    value = servicesBuffer.toString()
                                )
                            }
                        }

                        is ServerInfoState.Error -> {
                            Text(
                                text = serverInfoState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        else -> {
                            // Empty view for Loading and Idle
                            Text(
                                text = "No information available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    SettingsContent(
        cobaltUrl = "https://cobalt.example.com",
        cobaltApiKey = null,
        folderUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload",
        serverInfoState = ServerInfoState.Idle,
        onFolderSelected = {},
        onTestAndSave = {},
        onSaveApiKey = {}
    )
}
