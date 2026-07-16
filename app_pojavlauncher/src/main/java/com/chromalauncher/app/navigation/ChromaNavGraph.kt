package com.chromalauncher.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chromalauncher.app.ui.screens.AccountsScreen
import com.chromalauncher.app.ui.screens.DownloadScreen
import com.chromalauncher.app.ui.screens.MainScreen
import com.chromalauncher.app.ui.screens.ModsScreen
import com.chromalauncher.app.ui.screens.SettingsScreen
import com.chromalauncher.app.viewmodel.MainViewModel

@Composable
fun ChromaNavHost() {
    val navController = rememberNavController()
    val mainViewModel = MainViewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onPlayClick = {
                    // Launch game via bridge
                    mainViewModel.launchGame()
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                onDownloadClick = {
                    navController.navigate("download")
                },
                onModsClick = {
                    navController.navigate("mods")
                },
                onAccountsClick = {
                    navController.navigate("accounts")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("download") {
            DownloadScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("mods") {
            ModsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("accounts") {
            AccountsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
