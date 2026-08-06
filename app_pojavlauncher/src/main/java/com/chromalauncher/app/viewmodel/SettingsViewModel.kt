package com.chromalauncher.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.chromalauncher.app.ChromaApplication
import com.chromalauncher.app.manager.RendererManager
import com.chromalauncher.app.plugins.DriverPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.kdt.pojavlaunch.prefs.LauncherPreferences

data class SettingsUiState(
    val rendererId: String = "opengles2",
    val rendererOptions: List<Pair<String, String>> = emptyList(),
    val memoryMb: Int = 1024,
    val useAlternateSurface: Boolean = false,
    val forceEnglish: Boolean = false,
    val vkDriverSystem: Boolean = false,
    val vkDriver: String = "Turnip",
    val vkDriverOptions: List<String> = emptyList()
)

class SettingsViewModel : AndroidViewModel(Application()) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val renderers = RendererManager.rendererList.map { it.id to it.des }
        val selectedRenderer = if (renderers.any { it.first == LauncherPreferences.PREF_RENDERER }) {
            LauncherPreferences.PREF_RENDERER
        } else {
            renderers.firstOrNull()?.first ?: "opengles2"
        }
        _uiState.value = SettingsUiState(
            rendererId = selectedRenderer,
            rendererOptions = renderers,
            memoryMb = LauncherPreferences.PREF_RAM_ALLOCATION,
            useAlternateSurface = LauncherPreferences.PREF_USE_ALTERNATE_SURFACE,
            forceEnglish = LauncherPreferences.PREF_FORCE_ENGLISH,
            vkDriverSystem = LauncherPreferences.PREF_VK_DRIVER_SYSTEM,
            vkDriver = LauncherPreferences.PREF_VK_DRIVER,
            vkDriverOptions = DriverPlugin.driverList.map { it.driver }
        )
    }

    fun refreshPlugins() {
        RendererManager.refresh(ChromaApplication.instance)
        load()
    }

    fun updateRenderer(id: String) {
        if (_uiState.value.rendererOptions.none { it.first == id }) return
        _uiState.value = _uiState.value.copy(rendererId = id)
        LauncherPreferences.PREF_RENDERER = id
        LauncherPreferences.DEFAULT_PREF.edit().putString("renderer", id).apply()
    }

    fun updateMemory(mb: Int) {
        _uiState.value = _uiState.value.copy(memoryMb = mb)
        LauncherPreferences.PREF_RAM_ALLOCATION = mb
        LauncherPreferences.DEFAULT_PREF.edit().putInt("allocation", mb).apply()
    }

    fun toggleAlternateSurface() {
        val value = !_uiState.value.useAlternateSurface
        _uiState.value = _uiState.value.copy(useAlternateSurface = value)
        LauncherPreferences.PREF_USE_ALTERNATE_SURFACE = value
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean("alternate_surface", value).apply()
    }

    fun toggleForceEnglish() {
        val value = !_uiState.value.forceEnglish
        _uiState.value = _uiState.value.copy(forceEnglish = value)
        LauncherPreferences.PREF_FORCE_ENGLISH = value
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean("force_english", value).apply()
    }

    fun toggleVkDriverSystem() {
        val value = !_uiState.value.vkDriverSystem
        _uiState.value = _uiState.value.copy(vkDriverSystem = value)
        LauncherPreferences.PREF_VK_DRIVER_SYSTEM = value
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean("vk_driver_system", value).apply()
    }

    fun updateVkDriver(name: String) {
        if (_uiState.value.vkDriverOptions.none { it == name }) return
        _uiState.value = _uiState.value.copy(vkDriver = name)
        LauncherPreferences.PREF_VK_DRIVER = name
        LauncherPreferences.DEFAULT_PREF.edit().putString("vk_driver", name).apply()
        DriverPlugin.selectDriver(name)
    }
}
