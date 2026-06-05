package org.mediadownloader.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.mediadownloader.ui.main.history.HistoryScreen
import org.mediadownloader.ui.main.settings.SettingsScreen
import org.mediadownloader.ui.theme.FetchoraTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FetchoraTheme { MainScreen() } }
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
