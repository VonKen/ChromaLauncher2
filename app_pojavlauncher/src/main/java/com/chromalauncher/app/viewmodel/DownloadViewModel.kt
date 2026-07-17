package com.chromalauncher.app.viewmodel

import android.app.Application
import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import com.chromalauncher.app.bridge.LauncherBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.kdt.pojavlaunch.JMinecraftVersionList

data class DownloadUiState(
    val isLoading: Boolean = true,
    val versions: List<JMinecraftVersionList.Version> = emptyList(),
    val error: String = "",
    val downloadingVersion: String = "",
    val downloadProgress: Float = 0f,
    val downloadStatus: String = "",
    val downloadComplete: String = ""
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    init {
        refreshVersionList()
    }

    fun refreshVersionList() {
        _uiState.update { it.copy(isLoading = true, error = "") }
        LauncherBridge.fetchVersionList { versionList ->
            if (versionList != null && versionList.versions != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        versions = versionList.versions.toList()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load version list. Check your internet connection.")
                }
            }
        }
    }

    fun downloadVersion(activity: Activity, version: JMinecraftVersionList.Version) {
        _uiState.update { it.copy(downloadingVersion = version.id, error = "") }
        LauncherBridge.downloadVersion(
            activity = activity,
            version = version,
            versionId = version.id,
            onDone = {
                _uiState.update {
                    it.copy(
                        downloadingVersion = "",
                        downloadComplete = version.id
                    )
                }
            },
            onError = { throwable ->
                _uiState.update {
                    it.copy(
                        downloadingVersion = "",
                        error = "Download failed: ${throwable.message}"
                    )
                }
            }
        )
    }
}
