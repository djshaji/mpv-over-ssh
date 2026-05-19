package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Controller consumed by the ViewModel to start and stop the in-app HTTP stream.
 */
interface LocalMediaStreamController {
    val streamState: StateFlow<StreamState>

    suspend fun startStreaming(
        inputUri: Uri,
        options: StreamOptions = StreamOptions()
    ): String

    suspend fun stopStreaming()
}

/**
 * In-process HTTP streaming controller backed by NanoHTTPD.
 */
class DefaultLocalMediaStreamController(
    private val context: Context
) : LocalMediaStreamController {
    private val appContext = context.applicationContext
    private var server: HttpMediaStreamServer? = null

    private val streamStateMutable = MutableStateFlow<StreamState>(StreamState.Idle)
    override val streamState: StateFlow<StreamState> = streamStateMutable.asStateFlow()

    override suspend fun startStreaming(
        inputUri: Uri,
        options: StreamOptions
    ): String = withContext(Dispatchers.IO) {
        stopStreaming()

        val host = resolveLocalHttpHost()
            ?: throw IllegalStateException("Unable to determine a reachable local IP address")

        val token = UUID.randomUUID().toString().replace("-", "")
        val streamPath = buildStreamPath(token)
        var lastError: Throwable? = null

        for (port in DEFAULT_PORT_RANGE) {
            val candidate = HttpMediaStreamServer(port, appContext.contentResolver)
            candidate.setSource(inputUri, token)
            try {
                candidate.start()
                server = candidate
                val servedUrl = "http://$host:${candidate.listeningPort}$streamPath"
                streamStateMutable.value = StreamState.Preparing(inputUri, servedUrl)
                streamStateMutable.value = StreamState.Streaming(
                    StreamSession(
                        inputUri = inputUri,
                        publishUrl = servedUrl,
                        startedAtEpochMs = System.currentTimeMillis()
                    )
                )
                return@withContext servedUrl
            } catch (error: Throwable) {
                lastError = error
                candidate.stop()
            }
        }

        streamStateMutable.value = StreamState.Error(
            lastError?.message ?: "Unable to start the local HTTP stream"
        )
        throw IllegalStateException("Unable to start the local HTTP stream", lastError)
    }

    override suspend fun stopStreaming() = withContext(Dispatchers.IO) {
        server?.stop()
        server?.clearSource()
        server = null
        streamStateMutable.value = StreamState.Stopped
    }

    private fun resolveLocalHttpHost(): String? {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces().asList()
        val addresses = interfaces.flatMap { interfaceAddresses ->
            interfaceAddresses.inetAddresses.toList()
        }
        return pickBestHttpHostAddress(addresses)
    }

    private companion object {
        val DEFAULT_PORT_RANGE = 8080..8095
    }
}

private fun <T> java.util.Enumeration<T>.asList(): List<T> {
    val items = mutableListOf<T>()
    while (hasMoreElements()) {
        items += nextElement()
    }
    return items
}
