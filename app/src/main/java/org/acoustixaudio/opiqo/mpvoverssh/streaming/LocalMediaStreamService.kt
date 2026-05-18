@file:Suppress("unused")

package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.acoustixaudio.opiqo.mpvoverssh.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalMediaStreamService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val streamer: RtmpStreamer = FfmpegRtmpStreamer()
    private var streamJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            LocalMediaStreamServiceActions.ACTION_START_STREAM -> {
                val stateText = "Preparing local stream"
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(stateText)
                )
                handleStart(intent)
            }

            LocalMediaStreamServiceActions.ACTION_STOP_STREAM -> {
                serviceScope.launch {
                    streamer.stop()
                }
                streamJob?.cancel()
                streamStateMutable.value = StreamState.Stopped
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        streamJob?.cancel()
        serviceScope.cancel()
        if (streamStateMutable.value !is StreamState.Stopped) {
            streamStateMutable.value = StreamState.Idle
        }
        super.onDestroy()
    }

    private fun handleStart(intent: Intent) {
        val inputUri = intent.getStringExtra(LocalMediaStreamServiceActions.EXTRA_INPUT_URI)
            ?.let(Uri::parse)
        val publishUrl = intent.getStringExtra(LocalMediaStreamServiceActions.EXTRA_PUBLISH_URL)

        if (inputUri == null || publishUrl.isNullOrBlank()) {
            streamStateMutable.value = StreamState.Error("Missing stream input or publish URL.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        streamJob?.cancel()
        streamJob = serviceScope.launch {
            streamer.start(inputUri, publishUrl).collectLatest { event ->
                when (event) {
                    is StreamEvent.Preparing -> {
                        streamStateMutable.value = StreamState.Preparing(event.inputUri, event.publishUrl)
                        updateNotification("Preparing stream")
                    }

                    is StreamEvent.Running -> {
                        streamStateMutable.value = StreamState.Streaming(event.session)
                        updateNotification("Streaming local media")
                    }

                    is StreamEvent.Stats -> {
                        val current = streamStateMutable.value
                        if (current is StreamState.Streaming) {
                            streamStateMutable.value = current.copy(
                                lastBytesSent = event.bytesSent,
                                lastBitrateKbps = event.bitrateKbps
                            )
                        }
                    }

                    is StreamEvent.Retrying -> {
                        streamStateMutable.value = StreamState.Retrying(
                            attempt = event.attempt,
                            nextRetryInMs = event.nextRetryInMs,
                            lastError = event.reason
                        )
                        updateNotification("Retrying stream (attempt ${event.attempt})")
                    }

                    is StreamEvent.Failed -> {
                        streamStateMutable.value = StreamState.Error(event.message)
                        updateNotification("Stream failed")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                    StreamEvent.Stopped -> {
                        streamStateMutable.value = StreamState.Stopped
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun buildNotification(contentText: String): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Local media streaming",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "local_media_streaming"
        private const val NOTIFICATION_ID = 6001

        private val streamStateMutable = MutableStateFlow<StreamState>(StreamState.Idle)
        val streamState: StateFlow<StreamState> = streamStateMutable.asStateFlow()
    }
}


