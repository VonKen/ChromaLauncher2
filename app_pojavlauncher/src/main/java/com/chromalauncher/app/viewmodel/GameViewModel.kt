package com.chromalauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val logOutput: String = ""
)

class GameViewModel : AndroidViewModel(Application()) {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun setRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isRunning = running)
    }

    fun setPaused(paused: Boolean) {
        _uiState.value = _uiState.value.copy(isPaused = paused)
    }

    fun appendLog(line: String) {
        _uiState.value = _uiState.value.copy(
            logOutput = _uiState.value.logOutput + line + "\n"
        )
    }
}
