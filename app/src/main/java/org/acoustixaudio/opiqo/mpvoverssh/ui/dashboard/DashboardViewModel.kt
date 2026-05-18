package org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.data.AppRepository
import org.acoustixaudio.opiqo.mpvoverssh.data.SshProfile
import org.acoustixaudio.opiqo.mpvoverssh.streaming.LocalMediaStreamServiceController
import org.acoustixaudio.opiqo.mpvoverssh.streaming.StreamOptions
import org.acoustixaudio.opiqo.mpvoverssh.streaming.StreamState

data class DashboardUiState(
    val profile: SshProfile? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val terminalOutput: String = "",
    val commandHistory: List<CommandHistoryItem> = emptyList(),
    val remoteBrowser: RemoteBrowserState = RemoteBrowserState(),
    val streamState: StreamState = StreamState.Idle,
    val isSocketReady: Boolean = false,
    val errorMessage: String? = null
)

data class CommandHistoryItem(
    val id: Long,
    val command: String,
    val output: String,
    val isError: Boolean
)

data class RemoteBrowserState(
    val isVisible: Boolean = false,
    val currentPath: String = "/",
    val entries: List<RemoteFsEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class RemoteFsEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean
)

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected
}

class DashboardViewModel(
    private val repository: AppRepository,
    private val streamServiceController: LocalMediaStreamServiceController,
    private val profileId: Long
) : ViewModel() {
    private enum class CommandCategory {
        Generic,
        SocketControl,
        LaunchMpv,
        StartPlayback,
        ConnectionProbe
    }

    private var historyIdCounter: Long = 0L

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observeStreamState()
    }

    private fun observeStreamState() {
        viewModelScope.launch {
            streamServiceController.streamState.collectLatest { streamState ->
                _uiState.update { state ->
                    val stateWithStream = state.copy(streamState = streamState)
                    if (streamState is StreamState.Error) {
                        stateWithStream.copy(
                            errorMessage = streamState.message,
                            terminalOutput = appendTerminal(
                                state.terminalOutput,
                                "local-stream",
                                "Error: ${streamState.message}"
                            )
                        )
                    } else {
                        stateWithStream
                    }
                }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.getProfileById(profileId)
            _uiState.update { it.copy(profile = profile) }
        }
    }

    fun sendCommand(command: String) {
        executeCommand(command, CommandCategory.Generic)
    }

    private fun executeCommand(command: String, category: CommandCategory) {
        val profile = uiState.value.profile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting, errorMessage = null) }
            val result = repository.executeCommand(profile, command)

            result.onSuccess { output ->
                _uiState.update { state ->
                    val trimmedOutput = output.trimEnd()
                    state.copy(
                        terminalOutput = appendTerminal(state.terminalOutput, command, trimmedOutput),
                        commandHistory = appendHistory(state.commandHistory, command, trimmedOutput, isError = false),
                        connectionStatus = ConnectionStatus.Connected,
                        isSocketReady = resolveSocketReady(state.isSocketReady, category, trimmedOutput)
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    val rawMessage = error.message ?: "Unknown error"
                    val userMessage = buildUserErrorMessage(rawMessage, category)
                    val isConnectionFailure = isConnectionFailure(rawMessage)
                    val errorText = "Error: $userMessage"
                    state.copy(
                        terminalOutput = appendTerminal(
                            state.terminalOutput,
                            command,
                            errorText
                        ),
                        commandHistory = appendHistory(state.commandHistory, command, errorText, isError = true),
                        connectionStatus = if (isConnectionFailure) {
                            ConnectionStatus.Disconnected
                        } else {
                            ConnectionStatus.Connected
                        },
                        isSocketReady = when {
                            isConnectionFailure -> false
                            category == CommandCategory.SocketControl && isSocketUnavailable(rawMessage) -> false
                            else -> state.isSocketReady
                        },
                        errorMessage = userMessage
                    )
                }
            }
        }
    }

    fun playPause() = sendSocketCommand("cycle pause")

    fun checkConnection() {
        executeCommand(
            "if [ -S /tmp/mpvsocket ]; then echo '__SOCKET_READY__'; else echo '__SOCKET_MISSING__'; fi",
            CommandCategory.ConnectionProbe
        )
    }

    fun stopPlayback() = sendSocketCommand("stop")

    fun seekBySeconds(seconds: Int) = sendSocketCommand("seek $seconds")

    fun seekToPercent(percent: Int) = sendSocketCommand("seek $percent absolute-percent")

    fun adjustVolume(delta: Int) = sendSocketCommand("add volume $delta")

    fun nextTrack() = sendSocketCommand("playlist-next")

    fun previousTrack() = sendSocketCommand("playlist-prev")

    fun launchMpv() {
        executeCommand(
            "pgrep -x mpv >/dev/null || nohup mpv --idle --force-window --input-ipc-server=/tmp/mpvsocket >/tmp/mpv.log 2>&1 &",
            CommandCategory.LaunchMpv
        )
    }

    fun playUrl(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "URL cannot be empty") }
            return
        }
        executeCommand(buildMpvPlayCommand(trimmedUrl), CommandCategory.StartPlayback)
    }

    fun openRemoteBrowser(startPath: String = "/") {
        _uiState.update { state ->
            state.copy(remoteBrowser = state.remoteBrowser.copy(isVisible = true))
        }
        listRemoteDirectory(startPath)
    }

    fun closeRemoteBrowser() {
        _uiState.update { state ->
            state.copy(remoteBrowser = state.remoteBrowser.copy(isVisible = false, errorMessage = null))
        }
    }

    fun navigateToDirectory(path: String) {
        listRemoteDirectory(path)
    }

    fun navigateUpDirectory() {
        val current = uiState.value.remoteBrowser.currentPath
        val parent = parentPath(current)
        listRemoteDirectory(parent)
    }

    fun selectRemoteFile(path: String) {
        executeCommand(buildMpvPlayCommand(path), CommandCategory.StartPlayback)
        closeRemoteBrowser()
    }

    fun sendCustomCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Command cannot be empty") }
            return
        }
        sendCommand(trimmedCommand)
    }

    fun startLocalMediaStream(inputUri: Uri, publishUrl: String) {
        viewModelScope.launch {
            runCatching {
                streamServiceController.startStreaming(
                    inputUri = inputUri,
                    publishUrl = publishUrl,
                    options = StreamOptions()
                )
            }.onSuccess {
                val remotePlaybackUrl = toRemotePlaybackUrl(publishUrl)
                executeCommand(buildMpvPlayCommand(remotePlaybackUrl), CommandCategory.StartPlayback)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to start local media stream")
                }
            }
        }
    }

    fun stopLocalMediaStream() {
        viewModelScope.launch {
            runCatching { streamServiceController.stopStreaming() }
                .onSuccess {
                    sendCommand("echo \"stop\" | socat - /tmp/mpvsocket")
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to stop local media stream")
                    }
                }
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalOutput = "") }
    }

    fun rerunCommand(item: CommandHistoryItem) {
        sendCommand(item.command)
    }

    fun clearCommandHistory() {
        _uiState.update { it.copy(commandHistory = emptyList()) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reportUserError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearRemoteBrowserError() {
        _uiState.update { state ->
            state.copy(remoteBrowser = state.remoteBrowser.copy(errorMessage = null))
        }
    }

    private fun sendSocketCommand(command: String) {
        if (!uiState.value.isSocketReady) {
            _uiState.update {
                it.copy(errorMessage = "mpv socket is not ready. Launch mpv or play a URL first.")
            }
            return
        }
        executeCommand("echo \"$command\" | socat - /tmp/mpvsocket", CommandCategory.SocketControl)
    }

    private fun buildMpvPlayCommand(target: String): String {
        return "nohup mpv --force-window --input-ipc-server=/tmp/mpvsocket ${shellQuote(target)} >/tmp/mpv.log 2>&1 &"
    }

    private fun listRemoteDirectory(path: String) {
        val profile = uiState.value.profile ?: return
        val quotedPath = shellQuote(path)
        val command = "if [ -d $quotedPath ]; then cd $quotedPath && pwd && find -L . -mindepth 1 -maxdepth 1 -printf '%y\\t%f\\n' | LC_ALL=C sort; else echo '__ERROR__ Not a directory'; exit 1; fi"

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    connectionStatus = ConnectionStatus.Connecting,
                    remoteBrowser = state.remoteBrowser.copy(
                        isLoading = true,
                        errorMessage = null
                    )
                )
            }

            val result = repository.executeCommand(profile, command)
            result.onSuccess { output ->
                val parsedPath = output.lineSequence().firstOrNull()?.trim().orEmpty()
                if (parsedPath.isBlank()) {
                    _uiState.update { state ->
                        state.copy(
                            connectionStatus = ConnectionStatus.Disconnected,
                            remoteBrowser = state.remoteBrowser.copy(
                                isLoading = false,
                                errorMessage = "Unable to read directory path"
                            )
                        )
                    }
                    return@onSuccess
                }

                val entries = output
                    .lineSequence()
                    .drop(1)
                    .mapNotNull { parseRemoteEntry(parsedPath, it) }
                    .toList()

                _uiState.update { state ->
                    state.copy(
                        connectionStatus = ConnectionStatus.Connected,
                        remoteBrowser = state.remoteBrowser.copy(
                            currentPath = parsedPath,
                            entries = entries,
                            isLoading = false,
                            errorMessage = null,
                            isVisible = true
                        )
                    )
                }
            }.onFailure { error ->
                val rawMessage = error.message ?: "Failed to load remote directory"
                val message = if (isConnectionFailure(rawMessage)) {
                    "Connection lost while loading remote files"
                } else {
                    rawMessage
                }
                _uiState.update { state ->
                    state.copy(
                        connectionStatus = if (isConnectionFailure(rawMessage)) {
                            ConnectionStatus.Disconnected
                        } else {
                            ConnectionStatus.Connected
                        },
                        errorMessage = message,
                        remoteBrowser = state.remoteBrowser.copy(
                            isLoading = false,
                            errorMessage = message,
                            isVisible = true
                        )
                    )
                }
            }
        }
    }

    private fun parseRemoteEntry(basePath: String, line: String): RemoteFsEntry? {
        val parts = line.split('\t', limit = 2)
        if (parts.size < 2) return null

        val type = parts[0].trim()
        val name = parts[1].trim()
        if (name.isBlank() || name == "." || name == "..") return null

        val fullPath = joinPath(basePath, name)
        return RemoteFsEntry(
            name = name,
            path = fullPath,
            isDirectory = type == "d"
        )
    }

    private fun joinPath(basePath: String, name: String): String {
        return if (basePath == "/") "/$name" else "$basePath/$name"
    }

    private fun parentPath(path: String): String {
        if (path == "/") return "/"
        val normalized = path.trimEnd('/')
        val idx = normalized.lastIndexOf('/')
        return if (idx <= 0) "/" else normalized.substring(0, idx)
    }

    private fun shellQuote(value: String): String {
        return "'${value.replace("'", "'\\''")}'"
    }

    private fun toRemotePlaybackUrl(publishUrl: String): String {
        val parsed = Uri.parse(publishUrl)
        if (parsed.host == null) return publishUrl
        val port = parsed.port.takeIf { it > 0 } ?: 1935
        return parsed.buildUpon()
            .encodedAuthority("127.0.0.1:$port")
            .build()
            .toString()
    }

    private fun buildUserErrorMessage(rawMessage: String, category: CommandCategory): String {
        return when {
            isConnectionFailure(rawMessage) -> "SSH connection failed. Check host, credentials, or network."
            category == CommandCategory.ConnectionProbe -> {
                "Unable to verify connection state. ${rawMessage.trim()}"
            }
            category == CommandCategory.SocketControl && isSocketUnavailable(rawMessage) -> {
                "mpv socket is unavailable. Launch mpv first."
            }
            else -> rawMessage
        }
    }

    private fun resolveSocketReady(current: Boolean, category: CommandCategory, output: String): Boolean {
        return when (category) {
            CommandCategory.SocketControl,
            CommandCategory.LaunchMpv,
            CommandCategory.StartPlayback -> true
            CommandCategory.ConnectionProbe -> output.lineSequence().any { it.trim() == "__SOCKET_READY__" }
            CommandCategory.Generic -> current
        }
    }

    private fun isConnectionFailure(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("session.connect") ||
            normalized.contains("auth fail") ||
            normalized.contains("unknownhost") ||
            normalized.contains("connection timed out") ||
            normalized.contains("connection refused") ||
            normalized.contains("connection reset") ||
            normalized.contains("network is unreachable")
    }

    private fun isSocketUnavailable(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("mpvsocket") ||
            normalized.contains("socat") ||
            normalized.contains("no such file")
    }

    private fun appendTerminal(current: String, command: String, output: String): String {
        val prefix = if (current.isBlank()) "" else "\n"
        return "$current$prefix\$ $command\n${output.trimEnd()}"
    }

    private fun appendHistory(
        current: List<CommandHistoryItem>,
        command: String,
        output: String,
        isError: Boolean
    ): List<CommandHistoryItem> {
        historyIdCounter += 1
        val next = CommandHistoryItem(
            id = historyIdCounter,
            command = command,
            output = output,
            isError = isError
        )
        return (listOf(next) + current).take(50)
    }

    class Factory(
        private val repository: AppRepository,
        private val streamServiceController: LocalMediaStreamServiceController,
        private val profileId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, streamServiceController, profileId) as T
        }
    }
}
