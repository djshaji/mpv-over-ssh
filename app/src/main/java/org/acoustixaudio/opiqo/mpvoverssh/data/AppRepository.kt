package org.acoustixaudio.opiqo.mpvoverssh.data

import kotlinx.coroutines.flow.Flow
import org.acoustixaudio.opiqo.mpvoverssh.ssh.SshManager

class AppRepository(
    private val sshProfileDao: SshProfileDao,
    private val sshManager: SshManager
) {
    val allProfiles: Flow<List<SshProfile>> = sshProfileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): SshProfile? = sshProfileDao.getProfileById(id)

    suspend fun insertProfile(profile: SshProfile) = sshProfileDao.insertProfile(profile)

    suspend fun updateProfile(profile: SshProfile) = sshProfileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: SshProfile) = sshProfileDao.deleteProfile(profile)

    suspend fun executeCommand(profile: SshProfile, command: String): Result<String> {
        return sshManager.executeCommand(profile, command)
    }
}
