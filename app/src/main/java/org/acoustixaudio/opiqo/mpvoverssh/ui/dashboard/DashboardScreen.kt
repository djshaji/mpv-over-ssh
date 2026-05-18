@file:Suppress("DEPRECATION")

package org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.acoustixaudio.opiqo.mpvoverssh.MpvOverSshApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    application: MpvOverSshApplication,
    profileId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(application.repository, profileId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var mediaUrl by rememberSaveable { mutableStateOf("") }
    var customCommand by rememberSaveable { mutableStateOf("") }
    var seekPercent by rememberSaveable { mutableFloatStateOf(50f) }

    val isBusy = uiState.connectionStatus == ConnectionStatus.Connecting

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionStatusChip(status = uiState.connectionStatus)

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

            ControlGrid(
                viewModel = viewModel,
                isLoading = isBusy
            )

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
                enabled = !isBusy
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

            TerminalView(
                output = uiState.terminalOutput,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ControlGrid(
    viewModel: DashboardViewModel,
    isLoading: Boolean
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
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.Stop,
                label = "Stop",
                onClick = { viewModel.stopPlayback() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
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
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.FastForward,
                label = "+5s",
                onClick = { viewModel.seekBySeconds(5) },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
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
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.VolumeUp,
                label = "Vol +",
                onClick = { viewModel.adjustVolume(5) },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
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
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.SkipNext,
                label = "Next",
                onClick = { viewModel.nextTrack() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
        FilledTonalButton(
            onClick = { viewModel.launchMpv() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
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
