package com.chromalauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.chromalauncher.app.bridge.LauncherBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MainUiState(
    val username: String = "Not logged in",
    val selectedVersion: String = "",
    val availableVersions: List<String> = emptyList(),
    val isLaunching: Boolean = false,
    val noAccountWarning: Boolean = false,
    val noVersionWarning: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadVersions()
        loadAccount()
    }

    fun loadAccount() {
        try {
            val account = LauncherBridge.getSelectedAccountName(getApplication())
            _uiState.update {
                it.copy(
                    username = account.ifEmpty { "Not logged in" },
                    noAccountWarning = account.isEmpty()
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(username = "Error loading account", noAccountWarning = true) }
        }
    }

    fun loadVersions() {
        try {
            val versions = LauncherBridge.getInstalledVersions()
            _uiState.update {
                it.copy(
                    availableVersions = versions,
                    selectedVersion = versions.firstOrNull() ?: "",
                    noVersionWarning = versions.isEmpty()
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    availableVersions = emptyList(),
                    selectedVersion = "",
                    noVersionWarning = true
                )
            }
        }
    }

    fun selectVersion(version: String) {
        _uiState.update { it.copy(selectedVersion = version, noVersionWarning = false) }
    }

    fun onPlayClick(): Boolean {
        val state = _uiState.value
        if (state.selectedVersion.isEmpty()) {
            _uiState.update { it.copy(noVersionWarning = true) }
            return false
        }
        if (state.username == "Not logged in" || state.username.isEmpty()) {
            _uiState.update { it.copy(noAccountWarning = true) }
            return false
        }
        return true
    }
}
