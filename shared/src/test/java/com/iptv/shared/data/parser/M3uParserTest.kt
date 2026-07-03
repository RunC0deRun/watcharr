package com.iptv.shared.data.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun testParseSimpleM3u() = runBlocking {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="1" tvg-name="Test Channel 1" tvg-logo="http://logo1.png" group-title="News",Test Channel 1
            http://stream1.m3u8
            #EXTINF:-1 tvg-id="2" tvg-logo="http://logo2.png" group-title="Movies",Test Channel 2
            http://stream2.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray())
        val tracks = M3uParser.parse(inputStream).toList()

        assertEquals(2, tracks.size)

        assertEquals("Test Channel 1", tracks[0].name)
        assertEquals("http://stream1.m3u8", tracks[0].url)
        assertEquals("1", tracks[0].tvgId)
        assertEquals("Test Channel 1", tracks[0].tvgName)
        assertEquals("http://logo1.png", tracks[0].logoUrl)
        assertEquals("News", tracks[0].groupTitle)

        assertEquals("Test Channel 2", tracks[1].name)
        assertEquals("http://stream2.m3u8", tracks[1].url)
        assertEquals("2", tracks[1].tvgId)
        assertEquals(null, tracks[1].tvgName)
        assertEquals("http://logo2.png", tracks[1].logoUrl)
        assertEquals("Movies", tracks[1].groupTitle)
    }

    @Test
    fun testParseInvalidLineIsIgnored() = runBlocking {
        val m3uContent = """
            #EXTM3U
            Some garbage line
            #EXTINF:-1 tvg-id="3",Channel 3
            http://stream3.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray())
        val tracks = M3uParser.parse(inputStream).toList()

        assertEquals(1, tracks.size)
        assertEquals("Channel 3", tracks[0].name)
        assertEquals("http://stream3.m3u8", tracks[0].url)
        assertEquals("3", tracks[0].tvgId)
    }

    @Test
    fun testParseWindowsLineEndings() = runBlocking {
        val m3uContent = "#EXTM3U\r\n#EXTINF:-1 tvg-id=\"4\",Channel 4\r\nhttp://stream4.m3u8\r\n"
        val inputStream = ByteArrayInputStream(m3uContent.toByteArray())
        val tracks = M3uParser.parse(inputStream).toList()

        assertEquals(1, tracks.size)
        assertEquals("Channel 4", tracks[0].name)
        assertEquals("http://stream4.m3u8", tracks[0].url)
    }

    @Test
    fun testParseEmptyAndMalformedContent() = runBlocking {
        val emptyContent = ""
        val emptyStream = ByteArrayInputStream(emptyContent.toByteArray())
        val emptyTracks = M3uParser.parse(emptyStream).toList()
        assertTrue(emptyTracks.isEmpty())

        val onlyHeader = "#EXTM3U\n"
        val headerStream = ByteArrayInputStream(onlyHeader.toByteArray())
        val headerTracks = M3uParser.parse(headerStream).toList()
        assertTrue(headerTracks.isEmpty())

        val missingUrl = """
            #EXTM3U
            #EXTINF:-1 tvg-id="5",Channel 5
            #EXTINF:-1 tvg-id="6",Channel 6
            http://stream6.m3u8
        """.trimIndent()
        val missingUrlStream = ByteArrayInputStream(missingUrl.toByteArray())
        val missingUrlTracks = M3uParser.parse(missingUrlStream).toList()
        // Channel 5 is ignored because it had no URL immediately following it
        assertEquals(1, missingUrlTracks.size)
        assertEquals("Channel 6", missingUrlTracks[0].name)
        assertEquals("http://stream6.m3u8", missingUrlTracks[0].url)
    }
}
