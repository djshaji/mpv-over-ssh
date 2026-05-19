@file:Suppress("DEPRECATION")

package org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.acoustixaudio.opiqo.mpvoverssh.MpvOverSshApplication
import org.acoustixaudio.opiqo.mpvoverssh.settings.ThemeMode
import org.acoustixaudio.opiqo.mpvoverssh.streaming.StreamState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    application: MpvOverSshApplication,
    profileId: Long,
    initialSharedUri: String? = null,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onSwitchProfile: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
     val context = LocalContext.current
     val viewModel: DashboardViewModel = viewModel(
         factory = DashboardViewModel.Factory(
             application.repository,
             application.localMediaStreamController,
             profileId,
             context.applicationContext
         )
     )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var mediaUrl by rememberSaveable { mutableStateOf("") }
    var customCommand by rememberSaveable { mutableStateOf("") }
    var seekPercent by rememberSaveable { mutableFloatStateOf(50f) }
    var selectedLocalUri by rememberSaveable { mutableStateOf("") }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showTerminal by rememberSaveable { mutableStateOf(true) }
    var showProfilePicker by remember { mutableStateOf(false) }
    // Do not persist this across process recreation; shared routes should auto-start again.
    var hasConsumedInitialSharedUri by remember(initialSharedUri) { mutableStateOf(false) }

    val localMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedLocalUri = uri.toString()
            viewModel.startLocalMediaStream(uri)
        }
    }

    val isBusy = uiState.connectionStatus == ConnectionStatus.Connecting
    val socketControlsEnabled = !isBusy &&
        uiState.connectionStatus == ConnectionStatus.Connected &&
        uiState.isSocketReady

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(initialSharedUri, hasConsumedInitialSharedUri) {
        if (hasConsumedInitialSharedUri) return@LaunchedEffect
        val sharedUri = initialSharedUri?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val parsedUri = runCatching { Uri.parse(sharedUri) }.getOrNull() ?: return@LaunchedEffect
        selectedLocalUri = sharedUri
        viewModel.startLocalMediaStream(parsedUri)
        hasConsumedInitialSharedUri = true
    }

    if (uiState.remoteBrowser.isVisible) {
        RemoteFileBrowserSheet(
            browser = uiState.remoteBrowser,
            onDismiss = viewModel::closeRemoteBrowser,
            onNavigateUp = viewModel::navigateUpDirectory,
            onOpenDirectory = { entry -> viewModel.navigateToDirectory(entry.path) },
            onSelectFile = { entry -> viewModel.selectRemoteFile(entry.path) },
            onClearError = viewModel::clearRemoteBrowserError
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.profile?.name ?: "Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showThemeMenu = true }) {
                            Icon(Icons.Rounded.Palette, contentDescription = "Theme")
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("System") },
                                onClick = {
                                    onThemeModeChange(ThemeMode.System)
                                    showThemeMenu = false
                                },
                                trailingIcon = {
                                    if (themeMode == ThemeMode.System) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Light") },
                                onClick = {
                                    onThemeModeChange(ThemeMode.Light)
                                    showThemeMenu = false
                                },
                                trailingIcon = {
                                    if (themeMode == ThemeMode.Light) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Dark") },
                                onClick = {
                                    onThemeModeChange(ThemeMode.Dark)
                                    showThemeMenu = false
                                },
                                trailingIcon = {
                                    if (themeMode == ThemeMode.Dark) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.clearCommandHistory() }) {
                        Icon(Icons.Rounded.History, contentDescription = "Clear History")
                    }
                    IconButton(onClick = { viewModel.clearTerminal() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Terminal")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
         Column(
             modifier = Modifier
                 .fillMaxSize()
                 .padding(padding)
                 .padding(16.dp)
                 .verticalScroll(rememberScrollState()),
             verticalArrangement = Arrangement.spacedBy(16.dp)
         ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStatusChip(status = uiState.connectionStatus)

                val showProfileSwitcher = false
                if (showProfileSwitcher && uiState.connectionStatus == ConnectionStatus.Disconnected && profiles.size > 1) {
                    Box {
                        OutlinedButton(onClick = { showProfilePicker = true }) {
                            Icon(Icons.Rounded.SwitchAccount, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Profile")
                        }
                        DropdownMenu(
                            expanded = showProfilePicker,
                            onDismissRequest = { showProfilePicker = false }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.name) },
                                    onClick = {
                                        showProfilePicker = false
                                        if (profile.id != profileId) {
                                            onSwitchProfile(profile.id)
                                        }
                                    },
                                    trailingIcon = {
                                        if (profile.id == profileId) {
                                            Icon(Icons.Rounded.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.checkConnection() },
                    enabled = !isBusy
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check Connection")
                }
            }

            OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("Media URL") },
                placeholder = { Text("https://example.com/media.mp4") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.playUrl(mediaUrl) },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = "Play URL")
                    }
                }
            )

            OutlinedButton(
                onClick = { viewModel.openRemoteBrowser(uiState.remoteBrowser.currentPath) },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Remote Files")
            }

            LocalStreamingCard(
                streamState = uiState.streamState,
                selectedLocalUri = selectedLocalUri,
                onPickMedia = { localMediaPicker.launch(arrayOf("video/*", "audio/*", "image/*")) },
                onStopStream = viewModel::stopLocalMediaStream,
                enabled = !isBusy
            )

             ControlGrid(
                 viewModel = viewModel,
                 socketControlsEnabled = socketControlsEnabled,
                 launchEnabled = !isBusy
             )

             Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                 OutlinedButton(
                     onClick = { viewModel.closeMpv() },
                     enabled = !isBusy,
                     modifier = Modifier.weight(1f)
                 ) {
                     Icon(Icons.Rounded.Close, contentDescription = null)
                     Spacer(modifier = Modifier.width(6.dp))
                     Text("Close mpv")
                 }
                 OutlinedButton(
                     onClick = { viewModel.disconnect() },
                     enabled = !isBusy,
                     modifier = Modifier.weight(1f)
                 ) {
                     Icon(Icons.Rounded.Logout, contentDescription = null)
                     Spacer(modifier = Modifier.width(6.dp))
                     Text("Disconnect")
                 }
             }

            if (!uiState.isSocketReady) {
                Text(
                    text = "Launch mpv or play media to enable playback controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Seek: ${seekPercent.toInt()}%",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = seekPercent,
                onValueChange = { seekPercent = it },
                onValueChangeFinished = {
                    viewModel.seekToPercent(seekPercent.toInt())
                },
                valueRange = 0f..100f,
                enabled = socketControlsEnabled
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customCommand,
                    onValueChange = { customCommand = it },
                    label = { Text("Custom command") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { viewModel.sendCustomCommand(customCommand) },
                    enabled = !isBusy,
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send")
                }
            }

            CommandHistoryView(
                history = uiState.commandHistory,
                onRerun = viewModel::rerunCommand,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terminal Output",
                    style = MaterialTheme.typography.titleSmall
                )
                FilledTonalButton(
                    onClick = { showTerminal = !showTerminal },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (showTerminal) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (showTerminal) "Hide Terminal" else "Show Terminal"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showTerminal) "Hide" else "Show")
                }
            }

            if (showTerminal) {
                TerminalView(
                    output = uiState.terminalOutput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalStreamingCard(
    streamState: StreamState,
    selectedLocalUri: String,
    onPickMedia: () -> Unit,
    onStopStream: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Local Media Stream", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Pick local video, audio, or image files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val activeStreamUrl = streamUrl(streamState)
            if (activeStreamUrl != null) {
                Text(
                    text = "Serving at: $activeStreamUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (selectedLocalUri.isNotBlank()) {
                Text(
                    text = "Selected: $selectedLocalUri",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Text(
                text = "Status: ${streamStateLabel(streamState)}",
                style = MaterialTheme.typography.labelLarge
            )

            if (streamState is StreamState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = streamState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickMedia,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Local Media")
                }
                OutlinedButton(
                    onClick = onStopStream,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.StopCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Stream")
                }
            }
        }
    }
}

private fun streamUrl(streamState: StreamState): String? {
    return when (streamState) {
        is StreamState.Preparing -> streamState.publishUrl
        is StreamState.Streaming -> streamState.session.publishUrl
        else -> null
    }
}

private fun streamStateLabel(streamState: StreamState): String {
    return when (streamState) {
        StreamState.Idle -> "Idle"
        is StreamState.Preparing -> "Preparing"
        is StreamState.Streaming -> "Streaming"
        is StreamState.Retrying -> "Retrying (attempt ${streamState.attempt})"
        is StreamState.Error -> "Error - ${streamState.message}"
        StreamState.Stopped -> "Stopped"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteFileBrowserSheet(
    browser: RemoteBrowserState,
    onDismiss: () -> Unit,
    onNavigateUp: () -> Unit,
    onOpenDirectory: (RemoteFsEntry) -> Unit,
    onSelectFile: (RemoteFsEntry) -> Unit,
    onClearError: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Remote Files", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateUp, enabled = !browser.isLoading) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Up")
                }
            }

            Text(
                text = browser.currentPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            browser.errorMessage?.let { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            }

            if (browser.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (browser.entries.isEmpty()) {
                Text(
                    text = "No files found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(browser.entries, key = { it.path }) { entry ->
                        val icon = if (entry.isDirectory) Icons.Rounded.Folder else Icons.Rounded.InsertDriveFile
                        val clickAction = if (entry.isDirectory) {
                            { onOpenDirectory(entry) }
                        } else {
                            { onSelectFile(entry) }
                        }

                        ListItem(
                            modifier = Modifier.clickable(onClick = clickAction),
                            headlineContent = { Text(entry.name, maxLines = 1) },
                            supportingContent = {
                                Text(if (entry.isDirectory) "Directory" else "File")
                            },
                            leadingContent = { Icon(icon, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandHistoryView(
    history: List<CommandHistoryItem>,
    onRerun: (CommandHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Command History", style = MaterialTheme.typography.titleSmall)
            if (history.isEmpty()) {
                Text(
                    text = "No commands yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history, key = { it.id }) { entry ->
                        ElevatedCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\$ ${entry.command}",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = entry.output,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (entry.isError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 2
                                    )
                                }
                                FilledTonalIconButton(onClick = { onRerun(entry) }) {
                                    Icon(Icons.Rounded.Replay, contentDescription = "Run Again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlGrid(
    viewModel: DashboardViewModel,
    socketControlsEnabled: Boolean,
    launchEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon = Icons.Rounded.PlayArrow,
                label = "Play/Pause",
                onClick = { viewModel.playPause() },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
            ControlButton(
                icon = Icons.Rounded.Stop,
                label = "Stop",
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon = Icons.Rounded.FastRewind,
                label = "-5s",
                onClick = { viewModel.seekBySeconds(-5) },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
            ControlButton(
                icon = Icons.Rounded.FastForward,
                label = "+5s",
                onClick = { viewModel.seekBySeconds(5) },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon = Icons.Rounded.VolumeDown,
                label = "Vol -",
                onClick = { viewModel.adjustVolume(-5) },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
            ControlButton(
                icon = Icons.Rounded.VolumeUp,
                label = "Vol +",
                onClick = { viewModel.adjustVolume(5) },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ControlButton(
                icon = Icons.Rounded.SkipPrevious,
                label = "Previous",
                onClick = { viewModel.previousTrack() },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
            ControlButton(
                icon = Icons.Rounded.SkipNext,
                label = "Next",
                onClick = { viewModel.nextTrack() },
                modifier = Modifier.weight(1f),
                enabled = socketControlsEnabled
            )
        }
        FilledTonalButton(
            onClick = { viewModel.launchMpv() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = launchEnabled
        ) {
            Icon(Icons.Rounded.RocketLaunch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch mpv")
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ConnectionStatusChip(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.Connected -> "Connected" to MaterialTheme.colorScheme.primary
        ConnectionStatus.Connecting -> "Connecting" to MaterialTheme.colorScheme.tertiary
        ConnectionStatus.Disconnected -> "Disconnected" to MaterialTheme.colorScheme.error
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Circle,
                contentDescription = null,
                tint = color
            )
        }
    )
}

@Composable
fun TerminalView(
    output: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E), // Dark terminal background
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = if (output.isEmpty()) "Terminal ready..." else output,
                color = Color(0xFF00FF00), // Classic terminal green
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
