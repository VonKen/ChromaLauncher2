package com.chromalauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chromalauncher.app.ui.components.AccountChip
import com.chromalauncher.app.ui.components.ChromaButton
import com.chromalauncher.app.ui.components.ChromaCard
import com.chromalauncher.app.ui.components.ChromaDivider
import com.chromalauncher.app.ui.components.VersionDropdown
import com.chromalauncher.app.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onModsClick: () -> Unit,
    onAccountsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chroma",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )

            AccountChip(
                username = uiState.username,
                onClick = onAccountsClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        ChromaDivider()
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.noVersionWarning) {
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No versions installed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Go to Download to install a version first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else if (uiState.noAccountWarning) {
            ChromaCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No account selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Create an offline account first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        ChromaCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                VersionDropdown(
                    selectedVersion = uiState.selectedVersion,
                    versions = uiState.availableVersions,
                    onVersionSelected = { viewModel.selectVersion(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChromaButton(
            text = if (uiState.isLaunching) "Launching..." else "Play",
            onClick = {
                if (viewModel.onPlayClick()) {
                    onPlayClick()
                }
            },
            enabled = !uiState.isLaunching
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Download,
                label = "Download",
                onClick = onDownloadClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Extension,
                label = "Mods",
                onClick = onModsClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChromaCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
