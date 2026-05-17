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
    val isConnecting: Boolean = false,
    val terminalOutput: String = "",
    val errorMessage: String? = null
)

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
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            val result = repository.executeCommand(profile, command)
            
            result.onSuccess { output ->
                _uiState.update { state ->
                    state.copy(
                        terminalOutput = state.terminalOutput + "\n$ " + command + "\n" + output,
                        isConnecting = false
                    )
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        terminalOutput = state.terminalOutput + "\n$ " + command + "\nError: " + error.message,
                        isConnecting = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalOutput = "") }
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
