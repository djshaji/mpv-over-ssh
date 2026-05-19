package org.acoustixaudio.opiqo.mpvoverssh.ui.profiles

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.MpvOverSshApplication
import org.acoustixaudio.opiqo.mpvoverssh.data.SshProfile

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    application: MpvOverSshApplication,
    onNavigateToDashboard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfilesViewModel = viewModel(
        factory = ProfilesViewModel.Factory(application.repository)
    )
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long?>()
    val scope = rememberCoroutineScope()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                ProfileListPane(
                    profiles = profiles,
                    onProfileClick = { profileId ->
                        // Directly navigate to dashboard instead of opening detail pane
                        onNavigateToDashboard(profileId)
                    },
                    onEditProfile = { profileId ->
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, profileId)
                        }
                    },
                    onAddProfile = {
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, null)
                        }
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val selectedProfileId = navigator.currentDestination?.contentKey
                val profile = profiles.find { it.id == selectedProfileId }
                
                ProfileDetailPane(
                    profile = profile,
                    onSave = { updatedProfile ->
                        viewModel.saveProfile(updatedProfile)
                        scope.launch {
                            navigator.navigateBack()
                        }
                    },
                    onDelete = { profileToDelete ->
                        viewModel.deleteProfile(profileToDelete)
                        scope.launch {
                            navigator.navigateBack()
                        }
                    },
                    onBack = {
                        scope.launch {
                            navigator.navigateBack()
                        }
                    },
                    onConnect = { profileToConnect ->
                        onNavigateToDashboard(profileToConnect.id)
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListPane(
    profiles: List<SshProfile>,
    onProfileClick: (Long) -> Unit,
    onEditProfile: (Long) -> Unit,
    onAddProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAboutDialog by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar("No app available to open link") }
        }
    }

    fun reportIssue() {
        val subject = "mpv over ssh - Bug report"
        val body = buildString {
            appendLine("Please describe the issue:")
            appendLine()
            appendLine("App version: $versionName")
            appendLine("Android version: ${android.os.Build.VERSION.RELEASE}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        }
        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(mailIntent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar("No email app installed") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Profiles") },
                actions = {
                    TextButton(onClick = { showAboutDialog = true }) { Text("About") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Profile")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(profiles) { profile ->
                ListItem(
                    headlineContent = { Text(profile.name) },
                    supportingContent = { Text("${profile.username}@${profile.host}:${profile.port}") },
                    trailingContent = {
                        IconButton(onClick = { onEditProfile(profile.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile")
                        }
                    },
                    modifier = Modifier.clickable { onProfileClick(profile.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("mpv over ssh")
                    Text("Version: $versionName")
                    Text("Author: Opiqo")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { openUrl("https://github.com/") }) {
                        Text("Source")
                    }
                    TextButton(onClick = { openUrl("https://github.com/issues") }) {
                        Text("Issues")
                    }
                    TextButton(onClick = {
                        reportIssue()
                        showAboutDialog = false
                    }) {
                        Text("Report")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailPane(
    profile: SshProfile?,
    onSave: (SshProfile) -> Unit,
    onDelete: (SshProfile) -> Unit,
    onBack: () -> Unit,
    onConnect: (SshProfile) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var host by remember(profile) { mutableStateOf(profile?.host ?: "") }
    var port by remember(profile) { mutableStateOf(profile?.port?.toString() ?: "22") }
    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var privateKey by remember(profile) { mutableStateOf(profile?.privateKey ?: "") }
    var strictHostKeyChecking by remember(profile) { mutableStateOf(profile?.strictHostKeyChecking ?: false) }
    var password by remember(profile) { mutableStateOf(profile?.password ?: "") }
    var showDeleteConfirmation by remember(profile) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profile == null) "New Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (profile != null) {
                        IconButton(onClick = { onConnect(profile) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Connect")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = {
                        onSave(
                            SshProfile(
                                id = profile?.id ?: 0L,
                                name = name,
                                host = host,
                                port = port.toIntOrNull() ?: 22,
                                username = username,
                                privateKey = privateKey.ifBlank { null },
                                strictHostKeyChecking = strictHostKeyChecking,
                                password = password.ifBlank { null }
                            )
                        )
                    }) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = privateKey,
                onValueChange = { privateKey = it },
                label = { Text("Private Key (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = strictHostKeyChecking,
                    onCheckedChange = { strictHostKeyChecking = it }
                )
                Text("Strict Host Key Checking")
            }
        }
    }

    if (showDeleteConfirmation && profile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete profile") },
            text = { Text("Are you sure you want to delete '${profile.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete(profile)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
