package com.chromalauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val renderer: String = "Holy GL4ES",
    val javaVersion: String = "Java 8",
    val memoryMb: Int = 1024,
    val useAlternateSurface: Boolean = false,
    val forceEnglish: Boolean = false
)

class SettingsViewModel : AndroidViewModel(Application()) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateRenderer(renderer: String) {
        _uiState.value = _uiState.value.copy(renderer = renderer)
    }

    fun updateJavaVersion(version: String) {
        _uiState.value = _uiState.value.copy(javaVersion = version)
    }

    fun updateMemory(mb: Int) {
        _uiState.value = _uiState.value.copy(memoryMb = mb)
    }

    fun toggleAlternateSurface() {
        _uiState.value = _uiState.value.copy(
            useAlternateSurface = !_uiState.value.useAlternateSurface
        )
    }

    fun toggleForceEnglish() {
        _uiState.value = _uiState.value.copy(
            forceEnglish = !_uiState.value.forceEnglish
        )
    }
}
