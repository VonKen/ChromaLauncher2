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
    val selectedVersion: String = "1.21.1",
    val availableVersions: List<String> = emptyList(),
    val isLaunching: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadStatus: String = "",
    val installedMods: List<String> = emptyList()
)

class MainViewModel : AndroidViewModel(Application()) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadVersions()
    }

    private fun loadVersions() {
        try {
            val versions = LauncherBridge.getInstalledVersions()
            _uiState.update {
                it.copy(
                    availableVersions = versions,
                    selectedVersion = versions.firstOrNull() ?: "1.21.1"
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    availableVersions = listOf("1.21.1", "1.20.4", "1.20.2"),
                    selectedVersion = "1.21.1"
                )
            }
        }
    }

    fun selectVersion(version: String) {
        _uiState.update { it.copy(selectedVersion = version) }
    }

    fun launchGame() {
        _uiState.update { it.copy(isLaunching = true) }

        try {
            val success = LauncherBridge.launchGame(_uiState.value.selectedVersion)
            if (!success) {
                _uiState.update { it.copy(isLaunching = false) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLaunching = false) }
        }
    }

    fun updateDownloadProgress(progress: Float, status: String) {
        _uiState.update {
            it.copy(
                isDownloading = progress < 1f,
                downloadProgress = progress,
                downloadStatus = status
            )
        }
    }

    fun updateUsername(username: String) {
        _uiState.update { it.copy(username = username) }
    }
}
