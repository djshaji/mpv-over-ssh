package org.acoustixaudio.opiqo.mpvoverssh.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.acoustixaudio.opiqo.mpvoverssh.data.AppRepository
import org.acoustixaudio.opiqo.mpvoverssh.data.SshProfile

class ProfilesViewModel(private val repository: AppRepository) : ViewModel() {

    val profiles: StateFlow<List<SshProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveProfile(profile: SshProfile) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                repository.insertProfile(profile)
            } else {
                repository.updateProfile(profile)
            }
        }
    }

    fun deleteProfile(profile: SshProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }

    class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfilesViewModel(repository) as T
        }
    }
}
