package org.acoustixaudio.opiqo.mpvoverssh.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class HttpStreamUtilsTest {
    @Test
    fun buildStreamPath_trimsAndPrefixesToken() {
        assertEquals("/stream/abc123", buildStreamPath("  abc123  "))
    }

    @Test
    fun parseRangeHeader_supportsExplicitAndSuffixRanges() {
        val explicit = parseRangeHeader("bytes=10-19", 100)
        assertEquals(10L, explicit?.start)
        assertEquals(19L, explicit?.endInclusive)
        assertEquals(10L, explicit?.length)

        val suffix = parseRangeHeader("bytes=-5", 100)
        assertEquals(95L, suffix?.start)
        assertEquals(99L, suffix?.endInclusive)
        assertEquals(5L, suffix?.length)
    }

    @Test
    fun parseRangeHeader_returnsNullForInvalidRequests() {
        assertNull(parseRangeHeader("invalid", 100))
        assertNull(parseRangeHeader("bytes=200-300", 100))
    }

    @Test
    fun pickBestHttpHostAddress_prefersSiteLocalIpv4() {
        val address = pickBestHttpHostAddress(
            listOf(
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("192.168.1.20")
            )
        )

        assertEquals("192.168.1.20", address)
    }
}


