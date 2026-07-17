package com.chromalauncher.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalauncher.app.backend.*
import com.chromalauncher.app.ui.theme.ChromaColors

enum class Screen { HOME, ACCOUNTS, DOWNLOAD, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaApp(onLaunchGame: (String) -> Unit) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val launcher = LocalContext.current

    val accounts by produceState(emptyList<String>()) {
        value = LauncherBackend.getAccountNames()
    }
    var selectedAccount by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        selectedAccount = LauncherBackend.getSelectedAccount(launcher)
    }

    val installedVersions by produceState(emptyList<String>()) {
        value = LauncherBackend.getInstalledVersions()
    }
    var selectedVersion by remember(installedVersions) {
        mutableStateOf(installedVersions.firstOrNull() ?: "")
    }

    MaterialTheme(
        colorScheme = ChromaColors
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    selectedAccount = selectedAccount.ifEmpty { "No account" },
                    installedVersions = installedVersions,
                    selectedVersion = selectedVersion,
                    onSelectVersion = { selectedVersion = it },
                    onPlay = {
                        if (selectedVersion.isNotEmpty() && selectedAccount.isNotEmpty()) {
                            onLaunchGame(selectedVersion)
                        }
                    },
                    onAccounts = { screen = Screen.ACCOUNTS },
                    onDownload = { screen = Screen.DOWNLOAD },
                    onSettings = { screen = Screen.SETTINGS }
                )
                Screen.ACCOUNTS -> AccountsScreen(
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onCreate = { name ->
                        LauncherBackend.createOfflineAccount(name)
                        selectedAccount = name
                    },
                    onSelect = { name ->
                        LauncherBackend.selectAccount(launcher, name)
                        selectedAccount = name
                    },
                    onDelete = { name ->
                        LauncherBackend.deleteAccount(name)
                        if (selectedAccount == name) selectedAccount = ""
                    },
                    onBack = { screen = Screen.HOME }
                )
                Screen.DOWNLOAD -> DownloadScreen(
                    onBack = { screen = Screen.HOME }
                )
                Screen.SETTINGS -> SettingsScreen(
                    onBack = { screen = Screen.HOME }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    selectedAccount: String,
    installedVersions: List<String>,
    selectedVersion: String,
    onSelectVersion: (String) -> Unit,
    onPlay: () -> Unit,
    onAccounts: () -> Unit,
    onDownload: () -> Unit,
    onSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Chroma",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            AssistChip(
                onClick = onAccounts,
                label = { Text(selectedAccount, maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(18.dp)) }
            )
        }

        Spacer(Modifier.height(20.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(20.dp))

        if (installedVersions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("No versions installed", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    Text("Tap Download to install a version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (selectedAccount == "No account" || selectedAccount.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("No account selected", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    Text("Tap Accounts to create an offline account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (installedVersions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Version", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expanded = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedVersion.ifEmpty { "Select..." }, style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            installedVersions.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v) },
                                    onClick = { onSelectVersion(v); expanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = selectedVersion.isNotEmpty() && selectedAccount.isNotEmpty()
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Play", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))

        Text("Quick Actions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction(Icons.Default.Download, "Download", Modifier.weight(1f), onDownload)
            QuickAction(Icons.Default.Extension, "Mods", Modifier.weight(1f)) {}
            QuickAction(Icons.Default.Settings, "Settings", Modifier.weight(1f), onSettings)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountsScreen(
    accounts: List<String>,
    selectedAccount: String,
    onCreate: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Accounts") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (accounts.isEmpty()) {
                Spacer(Modifier.height(48.dp))
                Text("No accounts yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Create an offline account to start playing", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { name ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (name == selectedAccount) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else CardDefaults.cardColors()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f).clickable { onSelect(name) }) {
                                    Text(name, fontWeight = FontWeight.Medium)
                                    Text(if (name == selectedAccount) "Selected" else "Offline", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { deleteTarget = name }, Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { showCreate = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Create Offline Account")
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false; newName = "" },
            title = { Text("New Offline Account") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { if (newName.isNotBlank()) { onCreate(newName.trim()); newName = ""; showCreate = false } }, enabled = newName.isNotBlank()) {
                    Text("Create")
                }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; newName = "" }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Account") },
            text = { Text("Delete \"$name\"?") },
            confirmButton = { TextButton(onClick = { onDelete(name); deleteTarget = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadScreen(onBack: () -> Unit) {
    var versions by remember { mutableStateOf<List<net.kdt.pojavlaunch.JMinecraftVersionList.Version>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf("") }
    var done by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        LauncherBackend.fetchVersionList { list ->
            isLoading = false
            if (list != null && list.versions != null) {
                versions = list.versions.toList()
            } else {
                error = "Failed to load versions. Check your internet."
            }
        }
    }

    val activity = LocalContext.current as? android.app.Activity

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Download Versions") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = {
                IconButton(onClick = { isLoading = true; error = ""; done = ""; LauncherBackend.fetchVersionList { list -> isLoading = false; if (list?.versions != null) versions = list.versions.toList() else error = "Failed to load." } }) {
                    Icon(Icons.Default.Refresh, "Refresh")
                }
            }
        )

        Box(Modifier.fillMaxSize().padding(16.dp)) {
            when {
                isLoading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Loading...")
                }
                error.isNotEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp))
                    Button(onClick = { isLoading = true; error = ""; LauncherBackend.fetchVersionList { list -> isLoading = false; if (list?.versions != null) versions = list.versions.toList() else error = "Failed." } }) { Text("Retry") }
                }
                done.isNotEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Installed!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp)); Text(done); Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
                downloading.isNotEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Downloading $downloading...")
                }
                else -> {
                    val releases = versions.filter { it.type == "release" }
                    val snapshots = versions.filter { it.type == "snapshot" }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item { Text("Release", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
                        items(releases.take(20)) { v ->
                            Card(Modifier.fillMaxWidth().clickable(enabled = activity != null) {
                                activity?.let { a ->
                                    downloading = v.id; done = ""; error = ""
                                    LauncherBackend.downloadVersion(a, v, v.id,
                                        onDone = { downloading = ""; done = v.id },
                                        onError = { t -> downloading = ""; error = "Failed: ${t.message}" }
                                    )
                                }
                            }) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(v.id, fontWeight = FontWeight.Medium)
                                        v.releaseTime?.let { Text(it.substring(0, minOf(10, it.length)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)); Text("Snapshot", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
                        items(snapshots.take(10)) { v ->
                            Card(Modifier.fillMaxWidth().clickable(enabled = activity != null) {
                                activity?.let { a ->
                                    downloading = v.id; done = ""; error = ""
                                    LauncherBackend.downloadVersion(a, v, v.id,
                                        onDone = { downloading = ""; done = v.id },
                                        onError = { t -> downloading = ""; error = "Failed: ${t.message}" }
                                    )
                                }
                            }) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(v.id, fontWeight = FontWeight.Medium)
                                        v.releaseTime?.let { Text(it.substring(0, minOf(10, it.length)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Chroma Launcher v1.0", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Based on PojavLauncher (LGPL-3.0)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
