package com.chromalauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.chromalauncher.app.bridge.LauncherBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AccountUiState(
    val username: String,
    val isMicrosoft: Boolean = false
)

data class AccountsScreenUiState(
    val accounts: List<AccountUiState> = emptyList(),
    val selectedUsername: String = ""
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AccountsScreenUiState())
    val uiState: StateFlow<AccountsScreenUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun loadAccounts() {
        try {
            val names = LauncherBridge.getAccountNames()
            val accounts = names.map { AccountUiState(username = it) }
            val selected = LauncherBridge.getSelectedAccountName(getApplication())
            _uiState.update {
                it.copy(
                    accounts = accounts,
                    selectedUsername = selected
                )
            }
        } catch (e: Exception) {
            // Ignore errors loading accounts
        }
    }

    fun createOfflineAccount(username: String) {
        LauncherBridge.createOfflineAccount(username)
        loadAccounts()
    }

    fun selectAccount(username: String) {
        LauncherBridge.selectAccount(getApplication(), username)
        _uiState.update { it.copy(selectedUsername = username) }
    }

    fun deleteAccount(username: String) {
        LauncherBridge.deleteAccount(username)
        if (_uiState.value.selectedUsername == username) {
            LauncherBridge.selectAccount(getApplication(), "")
        }
        loadAccounts()
    }
}
