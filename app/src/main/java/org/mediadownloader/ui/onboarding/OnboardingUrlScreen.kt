package org.mediadownloader.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.mediadownloader.data.local.datastore.SettingsDataStore
import org.mediadownloader.ui.main.settings.ServerInfoState
import org.mediadownloader.ui.main.settings.SettingsViewModel

@Composable
fun OnboardingUrlScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onNavigateToApp: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serverInfoState by settingsViewModel.serverInfoState.collectAsState()
    var urlDraft by rememberSaveable { mutableStateOf(SettingsDataStore.DEFAULT_COBALT_URL) }
    var pendingContinue by remember { mutableStateOf(false) }
    val isLoading = serverInfoState is ServerInfoState.Loading

    // Après testAndSave, si succès et qu'on attendait une confirmation, on avance
    LaunchedEffect(serverInfoState) {
        if (pendingContinue && serverInfoState is ServerInfoState.Success) {
            pendingContinue = false
            onComplete()
            onNavigateToApp()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicators(currentStep = 1, totalSteps = 2)
            TextButton(onClick = {
                onComplete()
                onNavigateToApp()
            }) {
                Text("Passer", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "🔗 Configurez votre instance",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Entrez l'URL d'une instance Cobalt. L'instance officielle est pré-remplie.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = urlDraft,
            onValueChange = {
                urlDraft = it
                settingsViewModel.clearServerInfo()
            },
            label = { Text("URL de l'instance") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            )
        )
        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instances.cobalt.best"))
                context.startActivity(intent)
            }
        ) {
            Text(
                "↗ Trouver d'autres instances sur instances.cobalt.best",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        // Zone d'état
        when (val state = serverInfoState) {
            is ServerInfoState.Idle -> {}
            is ServerInfoState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
            is ServerInfoState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "✓ Instance vérifiée",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Cobalt ${state.info.version} · ${state.info.url}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            is ServerInfoState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "✗ Connexion échouée",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Boutons
        Button(
            onClick = {
                pendingContinue = true
                settingsViewModel.testAndSave(urlDraft)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tester et continuer")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Retour")
        }
        Spacer(Modifier.height(16.dp))
    }
}
