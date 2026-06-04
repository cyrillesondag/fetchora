package org.mediadownloader.ui.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.mediadownloader.ui.theme.XDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        val tweetUrl = intent
            ?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { extractUrl(it) }

        if (tweetUrl == null) { finish(); return }

        viewModel.loadVariants(tweetUrl)

        setContent {
            XDownloaderTheme {
                LaunchedEffect(Unit) {
                    viewModel.finishEvents.collect { finish() }
                }

                val state by viewModel.uiState.collectAsState()
                QualityBottomSheet(
                    state = state,
                    onDismiss = { finish() },
                    onDownload = { variant -> viewModel.download(tweetUrl, variant) }
                )
            }
        }
    }

    private fun extractUrl(text: String): String? =
        Regex("""https?://[^\s]+""").find(text)?.value
}
