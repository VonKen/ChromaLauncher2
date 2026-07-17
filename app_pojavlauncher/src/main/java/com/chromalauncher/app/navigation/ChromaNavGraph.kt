package com.chromalauncher.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chromalauncher.app.ui.screens.AccountsScreen
import com.chromalauncher.app.ui.screens.DownloadScreen
import com.chromalauncher.app.ui.screens.MainScreen
import com.chromalauncher.app.ui.screens.ModsScreen
import com.chromalauncher.app.ui.screens.SettingsScreen
import com.chromalauncher.app.viewmodel.AccountViewModel
import com.chromalauncher.app.viewmodel.DownloadViewModel
import com.chromalauncher.app.viewmodel.MainViewModel

@Composable
fun ChromaNavHost() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()
    val downloadViewModel: DownloadViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onPlayClick = {
                    mainViewModel.loadVersions()
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
                viewModel = downloadViewModel,
                onBack = {
                    mainViewModel.loadVersions()
                    navController.popBackStack()
                }
            )
        }
        composable("mods") {
            ModsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("accounts") {
            val accountsState by accountViewModel.uiState.collectAsState()

            AccountsScreen(
                accounts = accountsState.accounts,
                selectedUsername = accountsState.selectedUsername,
                onCreateAccount = { accountViewModel.createOfflineAccount(it) },
                onSelectAccount = { accountViewModel.selectAccount(it) },
                onDeleteAccount = { accountViewModel.deleteAccount(it) },
                onBack = {
                    mainViewModel.loadAccount()
                    navController.popBackStack()
                }
            )
        }
    }
}
