@file:Suppress("unused")

package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.net.Uri

/**
 * Stream runtime state consumed by UI/ViewModel.
 */
sealed interface StreamState {
    data object Idle : StreamState

    data class Preparing(
        val inputUri: Uri,
        val publishUrl: String
    ) : StreamState

    data class Streaming(
        val session: StreamSession,
        val lastBytesSent: Long? = null,
        val lastBitrateKbps: Int? = null
    ) : StreamState

    data class Retrying(
        val attempt: Int,
        val nextRetryInMs: Long,
        val lastError: String
    ) : StreamState

    data class Error(
        val message: String
    ) : StreamState

    data object Stopped : StreamState
}

data class StreamSession(
    val inputUri: Uri,
    val publishUrl: String,
    val startedAtEpochMs: Long
)

data class StreamOptions(
    val videoBitrateKbps: Int = 2500,
    val audioBitrateKbps: Int = 128,
    val keyframeIntervalSeconds: Int = 2,
    val maxRetries: Int = 3,
    val retryBackoffMs: Long = 3000
)

/**
 * Low-level streaming events from the encoder/uploader implementation.
 */
sealed interface StreamEvent {
    data class Preparing(
        val inputUri: Uri,
        val publishUrl: String
    ) : StreamEvent

    data class Running(
        val session: StreamSession
    ) : StreamEvent

    data class Stats(
        val bytesSent: Long,
        val bitrateKbps: Int? = null
    ) : StreamEvent

    data class Retrying(
        val attempt: Int,
        val nextRetryInMs: Long,
        val reason: String
    ) : StreamEvent

    data class Failed(
        val message: String
    ) : StreamEvent

    data object Stopped : StreamEvent
}


