package org.acoustixaudio.opiqo.mpvoverssh.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ssh_profiles")
data class SshProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val privateKey: String? = null,
    val passphrase: String? = null,
    val password: String? = null,
    val strictHostKeyChecking: Boolean = false
)
