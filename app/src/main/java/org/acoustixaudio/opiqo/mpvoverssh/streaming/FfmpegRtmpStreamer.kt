@file:Suppress("unused")

package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * FFmpeg process-backed RTMP streamer implementation.
 *
 * Note: requires `ffmpeg` binary availability on the runtime environment.
 */
class FfmpegRtmpStreamer : RtmpStreamer {
    private val activeProcess = AtomicReference<Process?>(null)

    override fun start(
        inputUri: Uri,
        publishUrl: String,
        options: StreamOptions
    ): Flow<StreamEvent> = callbackFlow {
        trySend(StreamEvent.Preparing(inputUri, publishUrl))

        val preflightError = checkFfmpegAvailability()
        if (preflightError != null) {
            trySend(StreamEvent.Failed(preflightError))
            trySend(StreamEvent.Stopped)
            close()
            return@callbackFlow
        }

        val command = buildFfmpegCommand(inputUri, publishUrl, options)
        val processJob: Job = launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
                activeProcess.set(process)

                trySend(
                    StreamEvent.Running(
                        StreamSession(
                            inputUri = inputUri,
                            publishUrl = publishUrl,
                            startedAtEpochMs = System.currentTimeMillis()
                        )
                    )
                )

                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val parsedStats = parseStatsLine(line.orEmpty())
                        if (parsedStats != null) {
                            trySend(parsedStats)
                        }
                    }
                }

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    trySend(StreamEvent.Stopped)
                } else {
                    trySend(StreamEvent.Failed("FFmpeg process failed with exit code $exitCode"))
                    trySend(StreamEvent.Stopped)
                }
            } catch (error: Exception) {
                trySend(StreamEvent.Failed(error.message ?: "Unable to start ffmpeg process"))
                trySend(StreamEvent.Stopped)
            } finally {
                activeProcess.getAndSet(null)?.destroy()
                close()
            }
        }

        awaitClose {
            processJob.cancel()
            activeProcess.getAndSet(null)?.destroy()
        }
    }

    override suspend fun stop() {
        activeProcess.getAndSet(null)?.destroy()
    }

    private fun buildFfmpegCommand(inputUri: Uri, publishUrl: String, options: StreamOptions): List<String> {
        val gop = (options.keyframeIntervalSeconds * 30).coerceAtLeast(30)
        return listOf(
            "ffmpeg",
            "-re",
            "-i", inputUri.toString(),
            "-c:v", "libx264",
            "-preset", "veryfast",
            "-tune", "zerolatency",
            "-pix_fmt", "yuv420p",
            "-b:v", "${options.videoBitrateKbps}k",
            "-c:a", "aac",
            "-b:a", "${options.audioBitrateKbps}k",
            "-ar", "44100",
            "-g", gop.toString(),
            "-keyint_min", gop.toString(),
            "-f", "flv",
            publishUrl
        )
    }

    private fun parseStatsLine(line: String): StreamEvent.Stats? {
        if (!line.contains("bitrate=") && !line.contains("size=")) return null
        val bitrate = Regex("bitrate=\\s*([0-9]+(?:\\.[0-9]+)?)kbits/s")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
            ?.toInt()
        val bytesSent = Regex("size=\\s*([0-9]+)kB")
            .find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.times(1024)
            ?: 0L
        return StreamEvent.Stats(bytesSent = bytesSent, bitrateKbps = bitrate)
    }

    private fun checkFfmpegAvailability(): String? {
        return try {
            val probe = ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start()
            val exit = probe.waitFor()
            if (exit == 0) {
                null
            } else {
                "FFmpeg is not available on this device runtime. Install/package ffmpeg and retry local streaming."
            }
        } catch (_: Exception) {
            "FFmpeg executable was not found. Local streaming requires ffmpeg binary availability."
        }
    }
}

