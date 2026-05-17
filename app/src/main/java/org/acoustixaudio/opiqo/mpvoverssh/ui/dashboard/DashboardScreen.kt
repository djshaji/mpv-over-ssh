package org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard

import androidx.compose.foundation.background
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
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Buttons Grid
            ControlGrid(
                onCommand = { viewModel.sendCommand(it) },
                isLoading = uiState.isConnecting
            )

            // Terminal Output
            TerminalView(
                output = uiState.terminalOutput,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ControlGrid(
    onCommand: (String) -> Unit,
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
                command = "echo \"cycle pause\" | socat - /tmp/mpvsocket",
                onCommand = onCommand,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.Stop,
                label = "Stop",
                command = "pkill mpv",
                onCommand = onCommand,
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
                command = "echo \"seek -5\" | socat - /tmp/mpvsocket",
                onCommand = onCommand,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.FastForward,
                label = "+5s",
                command = "echo \"seek 5\" | socat - /tmp/mpvsocket",
                onCommand = onCommand,
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
                command = "echo \"add volume -5\" | socat - /tmp/mpvsocket",
                onCommand = onCommand,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            ControlButton(
                icon = Icons.Rounded.VolumeUp,
                label = "Vol +",
                command = "echo \"add volume 5\" | socat - /tmp/mpvsocket",
                onCommand = onCommand,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    command: String,
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = { onCommand(command) },
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
