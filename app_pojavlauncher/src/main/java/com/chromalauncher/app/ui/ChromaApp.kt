package com.chromalauncher.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromalauncher.app.backend.LauncherBackend
import com.chromalauncher.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaApp(onLaunchGame: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ChromaViolet,
            secondary = ChromaCyan,
            tertiary = ChromaMagenta,
            background = ChromaDarkBg,
            surface = ChromaDarkSurface,
            surfaceVariant = ChromaDarkCard,
            onPrimary = ChromaTextPrimary,
            onSecondary = ChromaTextPrimary,
            onTertiary = ChromaTextPrimary,
            onBackground = ChromaTextPrimary,
            onSurface = ChromaTextPrimary,
            onSurfaceVariant = ChromaTextSecondary,
            error = ChromaError,
        )
    ) {
        Scaffold(
            containerColor = ChromaDarkBg,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Chroma",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ChromaDarkSurface,
                        titleContentColor = ChromaViolet,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = ChromaDarkSurface) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChromaViolet,
                            selectedTextColor = ChromaViolet,
                            indicatorColor = ChromaDarkCard,
                        ),
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Versions") },
                        label = { Text("Versions") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChromaCyan,
                            selectedTextColor = ChromaCyan,
                            indicatorColor = ChromaDarkCard,
                        ),
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Accounts") },
                        label = { Text("Accounts") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChromaMagenta,
                            selectedTextColor = ChromaMagenta,
                            indicatorColor = ChromaDarkCard,
                        ),
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeScreen(onLaunchGame = onLaunchGame)
                    1 -> VersionsScreen()
                    2 -> AccountsScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onLaunchGame: (String) -> Unit) {
    var instanceInfo by remember { mutableStateOf<LauncherBackend.InstanceInfo?>(null) }
    var accountInfo by remember { mutableStateOf<LauncherBackend.AccountInfo?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        instanceInfo = withContext(Dispatchers.IO) { LauncherBackend.getSelectedInstance() }
        accountInfo = withContext(Dispatchers.IO) { LauncherBackend.getCurrentAccount() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChromaDarkCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Current Instance",
                    fontSize = 14.sp,
                    color = ChromaTextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    instanceInfo?.name ?: "No instance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChromaTextPrimary,
                )
                Text(
                    instanceInfo?.versionId ?: "Unknown version",
                    fontSize = 14.sp,
                    color = ChromaCyan,
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChromaDarkCard),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Account",
                    fontSize = 14.sp,
                    color = ChromaTextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            accountInfo?.isMicrosoft == true -> Icons.Filled.Cloud
                            accountInfo?.isLocal == true -> Icons.Filled.Person
                            else -> Icons.Filled.HelpOutline
                        },
                        contentDescription = null,
                        tint = ChromaMagenta,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        accountInfo?.username ?: "No account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChromaTextPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val versionId = instanceInfo?.versionId ?: LauncherBackend.getLatestReleaseVersionId()
                onLaunchGame(versionId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = accountInfo != null && instanceInfo?.versionId != null,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChromaViolet,
                contentColor = ChromaTextPrimary,
                disabledContainerColor = ChromaDarkCard,
                disabledContentColor = ChromaTextSecondary,
            ),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Play",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun VersionsScreen() {
    var versions by remember { mutableStateOf<List<LauncherBackend.VersionInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            LauncherBackend.fetchVersionList { list ->
                versions = list.sortedByDescending { it.releaseTime }
                isLoading = false
            }
        }
    }

    val filteredVersions = versions.filter { ver ->
        when (filter) {
            "release" -> ver.type == "release"
            "snapshot" -> ver.type == "snapshot"
            else -> true
        }
    }.filter { ver ->
        searchQuery.isEmpty() || ver.id.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search versions...", color = ChromaTextSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ChromaTextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChromaCyan,
                unfocusedBorderColor = ChromaDarkCard,
                focusedContainerColor = ChromaDarkCard,
                unfocusedContainerColor = ChromaDarkCard,
                cursorColor = ChromaCyan,
                focusedTextColor = ChromaTextPrimary,
                unfocusedTextColor = ChromaTextPrimary,
            ),
            singleLine = true,
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("all" to "All", "release" to "Release", "snapshot" to "Snapshot").forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChromaCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ChromaCyan,
                    ),
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChromaCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredVersions) { version ->
                    VersionItem(version)
                }
            }
        }
    }
}

@Composable
fun VersionItem(version: LauncherBackend.VersionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChromaDarkCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (version.type) {
                            "release" -> ChromaSuccess
                            "snapshot" -> ChromaMagenta
                            else -> ChromaTextSecondary
                        }
                    ),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    version.id,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChromaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${version.type.replaceFirstChar { it.uppercase() }} \u2022 ${version.releaseTime.take(10)}",
                    fontSize = 12.sp,
                    color = ChromaTextSecondary,
                )
            }
        }
    }
}

@Composable
fun AccountsScreen() {
    var accounts by remember { mutableStateOf<List<LauncherBackend.AccountInfo>>(emptyList()) }
    var currentAccount by remember { mutableStateOf<LauncherBackend.AccountInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            accounts = LauncherBackend.loadAccounts()
            currentAccount = LauncherBackend.getCurrentAccount()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ChromaMagenta)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            "Accounts",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ChromaTextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Microsoft and offline accounts are supported",
            fontSize = 14.sp,
            color = ChromaTextSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = ChromaTextSecondary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No accounts",
                        fontSize = 18.sp,
                        color = ChromaTextSecondary,
                    )
                    Text(
                        "Add an account to start playing",
                        fontSize = 14.sp,
                        color = ChromaTextSecondary,
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(accounts.size) { index ->
                    val account = accounts[index]
                    val isCurrent = account.profileId == currentAccount?.profileId

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) ChromaViolet.copy(alpha = 0.15f) else ChromaDarkCard,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = when {
                                    account.isMicrosoft -> Icons.Filled.Cloud
                                    account.isLocal -> Icons.Filled.Person
                                    else -> Icons.Filled.HelpOutline
                                },
                                contentDescription = null,
                                tint = if (isCurrent) ChromaViolet else ChromaTextSecondary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    account.username,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ChromaTextPrimary,
                                )
                                Text(
                                    when {
                                        account.isMicrosoft -> "Microsoft"
                                        account.isLocal -> "Offline"
                                        else -> "Unknown"
                                    },
                                    fontSize = 12.sp,
                                    color = ChromaTextSecondary,
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Current",
                                    tint = ChromaViolet,
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        LauncherBackend.deleteAccount(index)
                                        accounts = LauncherBackend.loadAccounts()
                                        currentAccount = LauncherBackend.getCurrentAccount()
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = ChromaError,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
