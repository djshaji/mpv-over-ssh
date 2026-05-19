package org.acoustixaudio.opiqo.mpvoverssh.streaming

import android.content.ContentResolver
import android.net.Uri
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class HttpMediaStreamServer(
    private val port: Int,
    private val contentResolver: ContentResolver
) {
    val listeningPort: Int
        get() = serverSocket?.localPort ?: port

    @Volatile
    private var source: MediaSource? = null

    @Volatile
    private var serverSocket: ServerSocket? = null

    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool()

    fun setSource(uri: Uri, token: String) {
        source = MediaSource(uri = uri, token = token)
    }

    fun clearSource() {
        source = null
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return

        val socket = ServerSocket(port)
        serverSocket = socket
        executor.execute {
            while (running.get()) {
                try {
                    val client = socket.accept()
                    executor.execute { handleClient(client) }
                } catch (_: Exception) {
                    if (running.get()) continue else break
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        executor.shutdownNow()
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            runCatching {
                val request = parseRequest(client.getInputStream())
                val response = buildResponse(request)
                writeResponse(client.getOutputStream(), response)
            }.onFailure {
                // Best effort: the socket is closed by use { }.
            }
        }
    }

    private fun buildResponse(request: HttpRequest?): HttpResponse {
        val activeSource = source ?: return HttpResponse(
            status = 503,
            reason = "Service Unavailable",
            headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
            body = "Stream is not active".byteInputStream(),
            contentLength = "Stream is not active".toByteArray().size.toLong()
        )

        if (request == null) {
            return HttpResponse(
                status = 400,
                reason = "Bad Request",
                headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
                body = "Malformed request".byteInputStream(),
                contentLength = "Malformed request".toByteArray().size.toLong()
            )
        }

        if (request.method != "GET" && request.method != "HEAD") {
            return textResponse(405, "Method Not Allowed", "Only GET and HEAD are supported")
        }

        if (request.path != buildStreamPath(activeSource.token)) {
            return textResponse(404, "Not Found", "Unknown stream path")
        }

        val mimeType = resolveMimeType(activeSource.uri)
        val totalLength = resolveLength(activeSource.uri)
        val requestedRange = parseRangeHeader(request.headers["range"] ?: request.headers["Range"], totalLength ?: -1)

        if (requestedRange != null && totalLength != null) {
            val stream = openStream(activeSource.uri) ?: return textResponse(404, "Not Found", "Unable to open media file")
            if (!stream.skipFully(requestedRange.start)) {
                stream.close()
                return textResponse(416, "Range Not Satisfiable", "Requested range is not available")
            }

            val responseBody = if (request.method == "HEAD") null else stream
            return HttpResponse(
                status = 206,
                reason = "Partial Content",
                headers = mapOf(
                    "Content-Type" to mimeType,
                    "Accept-Ranges" to "bytes",
                    "Content-Range" to "bytes ${requestedRange.start}-${requestedRange.endInclusive}/$totalLength",
                    "Content-Length" to requestedRange.length.toString()
                ),
                body = responseBody,
                contentLength = requestedRange.length
            )
        }

        val stream = if (request.method == "HEAD") null else openStream(activeSource.uri)
        if (request.method != "HEAD" && stream == null) {
            return textResponse(404, "Not Found", "Unable to open media file")
        }

        val headers = mutableMapOf(
            "Content-Type" to mimeType,
            "Accept-Ranges" to "bytes"
        )
        if (totalLength != null) {
            headers["Content-Length"] = totalLength.toString()
        }

        return HttpResponse(
            status = 200,
            reason = "OK",
            headers = Collections.unmodifiableMap(headers),
            body = stream,
            contentLength = totalLength
        )
    }

    private fun parseRequest(inputStream: InputStream): HttpRequest? {
        val reader = inputStream.bufferedReader(StandardCharsets.ISO_8859_1)
        val requestLine = reader.readLine()?.takeIf { it.isNotBlank() } ?: return null
        val parts = requestLine.split(' ', limit = 3)
        if (parts.size < 2) return null

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) break
            val separatorIndex = line.indexOf(':')
            if (separatorIndex <= 0) continue
            val name = line.substring(0, separatorIndex).trim()
            val value = line.substring(separatorIndex + 1).trim()
            headers[name] = value
            headers[name.lowercase()] = value
        }

        return HttpRequest(
            method = parts[0].uppercase(),
            path = parts[1],
            headers = headers
        )
    }

    private fun writeResponse(outputStream: java.io.OutputStream, response: HttpResponse) {
        BufferedOutputStream(outputStream).use { out ->
            val statusLine = "HTTP/1.1 ${response.status} ${response.reason}\r\n"
            out.write(statusLine.toByteArray(StandardCharsets.ISO_8859_1))
            response.headers.forEach { (name, value) ->
                out.write("$name: $value\r\n".toByteArray(StandardCharsets.ISO_8859_1))
            }
            out.write("Connection: close\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1))
            response.body?.use { body ->
                body.copyTo(out)
            }
            out.flush()
        }
    }

    private fun resolveMimeType(uri: Uri): String {
        return contentResolver.getType(uri) ?: "application/octet-stream"
    }

    private fun resolveLength(uri: Uri): Long? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> runCatching { File(uri.path.orEmpty()).length() }.getOrNull()
                ?.takeIf { it >= 0 }

            else -> {
                contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                    val length = descriptor.length
                    if (length >= 0) length else null
                }
            }
        }
    }

    private fun openStream(uri: Uri): InputStream? {
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: return null
                runCatching { FileInputStream(File(path)) }.getOrNull()
            }

            else -> contentResolver.openInputStream(uri)
        }
    }

    private fun textResponse(status: Int, reason: String, text: String): HttpResponse {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        return HttpResponse(
            status = status,
            reason = reason,
            headers = mapOf("Content-Type" to "text/plain; charset=utf-8", "Content-Length" to bytes.size.toString()),
            body = bytes.inputStream(),
            contentLength = bytes.size.toLong()
        )
    }

    private fun InputStream.skipFully(bytes: Long): Boolean {
        var remaining = bytes
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) {
                val read = read()
                if (read == -1) return false
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
        return true
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val headers: Map<String, String>
    )

    private data class HttpResponse(
        val status: Int,
        val reason: String,
        val headers: Map<String, String>,
        val body: InputStream?,
        val contentLength: Long?
    )

    private data class MediaSource(
        val uri: Uri,
        val token: String
    )
}




