package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.net.Uri
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
}

