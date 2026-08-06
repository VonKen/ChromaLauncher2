package com.chromalauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chromalauncher.app.ui.components.ChromaButton
import com.chromalauncher.app.ui.components.ChromaCard
import com.chromalauncher.app.ui.components.ChromaDivider
import com.chromalauncher.app.ui.components.ChromaTopBar
import com.chromalauncher.app.ui.components.VersionDropdown
import com.chromalauncher.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val rendererNames = state.rendererOptions.map { it.second }
    val selectedRendererName = state.rendererOptions
        .firstOrNull { it.first == state.rendererId }
        ?.second
        ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ChromaTopBar(
            title = "Settings",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onBack
        )

        ChromaDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Renderer
            Text(
                text = "Renderer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    VersionDropdown(
                        selectedVersion = selectedRendererName,
                        versions = rendererNames,
                        onVersionSelected = { name ->
                            state.rendererOptions.firstOrNull { it.second == name }
                                ?.let { viewModel.updateRenderer(it.first) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChromaButton(
                        text = "Refresh renderer plugins",
                        onClick = { viewModel.refreshPlugins() },
                        gradient = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Java Runtime
            Text(
                text = "Java Runtime",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    VersionDropdown(
                        selectedVersion = "Java 8",
                        versions = listOf("Java 8", "Java 17", "Java 21"),
                        onVersionSelected = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Memory
            Text(
                text = "Memory Allocation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${state.memoryMb} MB",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = state.memoryMb.toFloat(),
                        onValueChange = { viewModel.updateMemory(it.toInt()) },
                        valueRange = 256f..4096f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Toggles
            Text(
                text = "Display",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggle(
                        title = "Alternate Surface Rendering",
                        subtitle = "May help performance on GPU-bound scenarios",
                        checked = state.useAlternateSurface,
                        onCheckedChange = { viewModel.toggleAlternateSurface() }
                    )
                    SettingToggle(
                        title = "Force English",
                        subtitle = "Show original strings",
                        checked = state.forceEnglish,
                        onCheckedChange = { viewModel.toggleForceEnglish() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Vulkan driver
            Text(
                text = "Vulkan Driver",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingToggle(
                        title = "Use system VK driver",
                        subtitle = "Disable to use a packaged driver plugin (e.g. Turnip)",
                        checked = state.vkDriverSystem,
                        onCheckedChange = { viewModel.toggleVkDriverSystem() }
                    )
                    if (!state.vkDriverSystem) {
                        VersionDropdown(
                            selectedVersion = state.vkDriver,
                            versions = state.vkDriverOptions,
                            onVersionSelected = { viewModel.updateVkDriver(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Chroma Launcher v1.0.0",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Based on PojavLauncher (LGPL-3.0)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
