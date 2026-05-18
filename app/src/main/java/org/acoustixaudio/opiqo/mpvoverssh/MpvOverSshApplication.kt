package org.acoustixaudio.opiqo.mpvoverssh

import android.app.Application
import org.acoustixaudio.opiqo.mpvoverssh.data.AppDatabase
import org.acoustixaudio.opiqo.mpvoverssh.data.AppRepository
import org.acoustixaudio.opiqo.mpvoverssh.settings.ThemePreferencesRepository
import org.acoustixaudio.opiqo.mpvoverssh.ssh.SshManager
import org.acoustixaudio.opiqo.mpvoverssh.streaming.DefaultLocalMediaStreamServiceController
import org.acoustixaudio.opiqo.mpvoverssh.streaming.FfmpegRtmpStreamer

class MpvOverSshApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.sshProfileDao(), SshManager()) }
    val themePreferencesRepository by lazy { ThemePreferencesRepository(this) }
    @Suppress("unused")
    val rtmpStreamer by lazy { FfmpegRtmpStreamer() }
    @Suppress("unused")
    val localMediaStreamController by lazy { DefaultLocalMediaStreamServiceController(this) }
}
