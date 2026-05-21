package org.mediadownloader.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.mediadownloader.ui.main.history.HistoryScreen
import org.mediadownloader.ui.main.settings.SettingsScreen
import org.mediadownloader.ui.theme.XDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { XDownloaderTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    val navController = rememberNavController()
    val tabs = listOf("history" to Icons.Default.History, "settings" to Icons.Default.Settings)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(label) { launchSingleTop = true }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label.replaceFirstChar { it.uppercaseChar() }) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "history", modifier = Modifier.padding(padding)) {
            composable("history")  { HistoryScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
