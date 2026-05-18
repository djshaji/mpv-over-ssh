package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract consumed by ViewModel to control foreground streaming lifecycle.
 */
interface LocalMediaStreamServiceController {
    val streamState: StateFlow<StreamState>

    suspend fun startStreaming(
        inputUri: Uri,
        publishUrl: String,
        options: StreamOptions = StreamOptions()
    )

    suspend fun stopStreaming()
}

/**
 * Action constants reserved for a future foreground service implementation.
 */
object LocalMediaStreamServiceActions {
    const val ACTION_START_STREAM = "org.acoustixaudio.opiqo.mpvoverssh.action.START_STREAM"
    const val ACTION_STOP_STREAM = "org.acoustixaudio.opiqo.mpvoverssh.action.STOP_STREAM"
    const val EXTRA_INPUT_URI = "org.acoustixaudio.opiqo.mpvoverssh.extra.INPUT_URI"
    const val EXTRA_PUBLISH_URL = "org.acoustixaudio.opiqo.mpvoverssh.extra.PUBLISH_URL"
}

class DefaultLocalMediaStreamServiceController(
    private val context: Context
) : LocalMediaStreamServiceController {
    override val streamState: StateFlow<StreamState> = LocalMediaStreamService.streamState

    override suspend fun startStreaming(
        inputUri: Uri,
        publishUrl: String,
        options: StreamOptions
    ) {
        val intent = Intent(context, LocalMediaStreamService::class.java).apply {
            action = LocalMediaStreamServiceActions.ACTION_START_STREAM
            putExtra(LocalMediaStreamServiceActions.EXTRA_INPUT_URI, inputUri.toString())
            putExtra(LocalMediaStreamServiceActions.EXTRA_PUBLISH_URL, publishUrl)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override suspend fun stopStreaming() {
        val intent = Intent(context, LocalMediaStreamService::class.java).apply {
            action = LocalMediaStreamServiceActions.ACTION_STOP_STREAM
        }
        context.startService(intent)
    }
}

