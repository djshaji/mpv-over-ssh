package org.acoustixaudio.opiqo.mpvoverssh.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SshProfileDao {
    @Query("SELECT * FROM ssh_profiles")
    fun getAllProfiles(): Flow<List<SshProfile>>

    @Query("SELECT * FROM ssh_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): SshProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SshProfile): Long

    @Update
    suspend fun updateProfile(profile: SshProfile)

    @Delete
    suspend fun deleteProfile(profile: SshProfile)
}
