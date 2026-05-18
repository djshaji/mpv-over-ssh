package org.acoustixaudio.opiqo.mpvoverssh.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.data.AppRepository
import org.acoustixaudio.opiqo.mpvoverssh.data.SshProfile

data class DashboardUiState(
    val profile: SshProfile? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val terminalOutput: String = "",
    val errorMessage: String? = null
)

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected
}

class DashboardViewModel(
    private val repository: AppRepository,
    private val profileId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.getProfileById(profileId)
            _uiState.update { it.copy(profile = profile) }
        }
    }

    fun sendCommand(command: String) {
        val profile = uiState.value.profile ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.Connecting, errorMessage = null) }
            val result = repository.executeCommand(profile, command)

            result.onSuccess { output ->
                _uiState.update { state ->
                    state.copy(
                        terminalOutput = appendTerminal(state.terminalOutput, command, output),
                        connectionStatus = ConnectionStatus.Connected
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        terminalOutput = appendTerminal(
                            state.terminalOutput,
                            command,
                            "Error: ${error.message ?: "Unknown error"}"
                        ),
                        connectionStatus = ConnectionStatus.Disconnected,
                        errorMessage = error.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun playPause() = sendSocketCommand("cycle pause")

    fun stopPlayback() = sendSocketCommand("stop")

    fun seekBySeconds(seconds: Int) = sendSocketCommand("seek $seconds")

    fun seekToPercent(percent: Int) = sendSocketCommand("seek $percent absolute-percent")

    fun adjustVolume(delta: Int) = sendSocketCommand("add volume $delta")

    fun nextTrack() = sendSocketCommand("playlist-next")

    fun previousTrack() = sendSocketCommand("playlist-prev")

    fun launchMpv() {
        sendCommand("pgrep -x mpv >/dev/null || nohup mpv --idle --force-window --input-ipc-server=/tmp/mpvsocket >/tmp/mpv.log 2>&1 &")
    }

    fun playUrl(url: String) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "URL cannot be empty") }
            return
        }
        val escapedUrl = trimmedUrl.replace("\"", "\\\"")
        sendCommand("nohup mpv --force-window --input-ipc-server=/tmp/mpvsocket \"$escapedUrl\" >/tmp/mpv.log 2>&1 &")
    }

    fun sendCustomCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Command cannot be empty") }
            return
        }
        sendCommand(trimmedCommand)
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalOutput = "") }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun sendSocketCommand(command: String) {
        sendCommand("echo \"$command\" | socat - /tmp/mpvsocket")
    }

    private fun appendTerminal(current: String, command: String, output: String): String {
        val prefix = if (current.isBlank()) "" else "\n"
        return "$current$prefix\$ $command\n${output.trimEnd()}"
    }

    class Factory(
        private val repository: AppRepository,
        private val profileId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, profileId) as T
        }
    }
}
