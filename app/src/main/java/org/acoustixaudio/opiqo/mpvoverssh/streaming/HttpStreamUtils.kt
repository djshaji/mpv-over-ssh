@file:Suppress("unused")

package org.acoustixaudio.opiqo.mpvoverssh.streaming

import java.net.Inet4Address
import java.net.InetAddress
import java.util.Locale

internal data class HttpByteRange(
    val start: Long,
    val endInclusive: Long
) {
    val length: Long get() = endInclusive - start + 1
}

internal fun buildStreamPath(token: String): String {
    return "/stream/${token.trim()}"
}

internal fun parseRangeHeader(rangeHeader: String?, totalLength: Long): HttpByteRange? {
    if (rangeHeader.isNullOrBlank() || totalLength <= 0) return null

    val match = Regex("""bytes=(\d*)-(\d*)""").find(rangeHeader.trim()) ?: return null
    val startText = match.groupValues[1]
    val endText = match.groupValues[2]

    val start: Long
    val end: Long

    when {
        startText.isNotEmpty() -> {
            start = startText.toLongOrNull() ?: return null
            end = if (endText.isNotEmpty()) {
                endText.toLongOrNull() ?: return null
            } else {
                totalLength - 1
            }
        }

        endText.isNotEmpty() -> {
            val suffixLength = endText.toLongOrNull() ?: return null
            if (suffixLength <= 0) return null
            end = totalLength - 1
            start = (totalLength - suffixLength).coerceAtLeast(0)
        }

        else -> return null
    }

    if (start >= totalLength) return null
    return HttpByteRange(start = start, endInclusive = minOf(end, totalLength - 1))
}

internal fun pickBestHttpHostAddress(addresses: Iterable<InetAddress>): String? {
    val candidates = addresses
        .asSequence()
        .filterIsInstance<Inet4Address>()
        .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isMulticastAddress }
        .toList()

    val preferred = candidates.firstOrNull { it.isSiteLocalAddress }
        ?: candidates.firstOrNull()

    return preferred?.hostAddress?.lowercase(Locale.US)
}


