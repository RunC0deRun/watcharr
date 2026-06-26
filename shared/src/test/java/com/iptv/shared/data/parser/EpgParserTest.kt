package com.iptv.shared.data.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class EpgParserTest {

    @Test
    fun testParseXmltvEPG() = runBlocking {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <tv>
              <channel id="hbo.us">
                <display-name>HBO HD</display-name>
              </channel>
              <programme start="20260626200000 +0200" stop="20260626213000 +0200" channel="hbo.us">
                <title lang="en">House of the Dragon</title>
                <desc lang="en">S02E02. Rhaenyra reaches out to allies.</desc>
              </programme>
              <programme start="20260626213000 +0200" stop="20260626230000 +0200" channel="hbo.us">
                <title lang="en">The Wire</title>
                <desc lang="en">S01E01. Baltimore drug investigation begins.</desc>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray())
        val programs = EpgParser.parse(inputStream, isGzip = false).toList()

        assertEquals(2, programs.size)

        assertEquals("hbo.us", programs[0].channelId)
        assertEquals("House of the Dragon", programs[0].title)
        assertEquals("S02E02. Rhaenyra reaches out to allies.", programs[0].desc)
        
        assertEquals(true, programs[0].stop > programs[0].start)
        
        assertEquals("hbo.us", programs[1].channelId)
        assertEquals("The Wire", programs[1].title)
        assertEquals(programs[0].stop, programs[1].start)
    }
}
