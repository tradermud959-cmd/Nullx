package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AboutScreen
import com.example.ui.HomeScreen
import com.example.ui.SettingsScreen
import com.example.ui.SplashScreen
import com.example.ui.HistoryScreen
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(chatViewModel: ChatViewModel, settingsViewModel: SettingsViewModel) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    viewModel = chatViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToHistory = { navController.navigate("history") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    chatViewModel = chatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("about") {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(
                    viewModel = chatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
