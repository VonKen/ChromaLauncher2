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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalauncher.app.ui.components.ChromaCard
import com.chromalauncher.app.ui.components.ChromaDivider
import com.chromalauncher.app.ui.components.ChromaTopBar
import com.chromalauncher.app.ui.components.VersionDropdown

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var memoryAllocation by remember { mutableFloatStateOf(1024f) }
    var useAlternateSurface by remember { mutableStateOf(false) }
    var forceEnglish by remember { mutableStateOf(false) }
    var selectedRenderer by remember { mutableStateOf("Holy GL4ES") }
    var selectedJava by remember { mutableStateOf("Java 8") }

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
                        selectedVersion = selectedRenderer,
                        versions = listOf(
                            "Holy GL4ES",
                            "Zink (Vulkan)",
                            "LTW (OpenGL ES 3)"
                        ),
                        onVersionSelected = { selectedRenderer = it }
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
                        selectedVersion = selectedJava,
                        versions = listOf("Java 8", "Java 17", "Java 21"),
                        onVersionSelected = { selectedJava = it }
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
                        text = "${memoryAllocation.toInt()} MB",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Slider(
                        value = memoryAllocation,
                        onValueChange = { memoryAllocation = it },
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
                        checked = useAlternateSurface,
                        onCheckedChange = { useAlternateSurface = it }
                    )
                    SettingToggle(
                        title = "Force English",
                        subtitle = "Show original strings",
                        checked = forceEnglish,
                        onCheckedChange = { forceEnglish = it }
                    )
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
