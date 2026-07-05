package io.github.RunC0deRun.shared.data.parser

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun testParseXmltvEPGWithMalformedDates() = runBlocking {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <tv>
              <programme start="invalid-date" stop="20260626213000" channel="hbo.us">
                <title lang="en">House of the Dragon</title>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray())
        val programs = EpgParser.parse(inputStream, isGzip = false).toList()

        assertEquals(1, programs.size)
        assertEquals("hbo.us", programs[0].channelId)
        assertEquals(0L, programs[0].start) // Fallback for invalid date is 0
        assertTrue(programs[0].stop > 0L) // Valid fallback date yyyyMMddHHmmss parses in UTC
    }

    @Test
    fun testParseXmltvEPGWithMissingAttributes() = runBlocking {
        val xmlContent = """
            <?xml version="1.0" encoding="utf-8"?>
            <tv>
              <programme channel="hbo.us">
                <title lang="en">Untitled Show</title>
              </programme>
              <programme start="20260626200000 +0200">
                <title lang="en">No Channel Show</title>
              </programme>
            </tv>
        """.trimIndent()

        val inputStream = ByteArrayInputStream(xmlContent.toByteArray())
        val programs = EpgParser.parse(inputStream, isGzip = false).toList()

        // The first program has channel and title, start/stop defaults to 0
        // The second program lacks a channel, which is ignored
        assertEquals(1, programs.size)
        assertEquals("hbo.us", programs[0].channelId)
        assertEquals("Untitled Show", programs[0].title)
        assertEquals(0L, programs[0].start)
    }

    @Test(expected = Exception::class)
    fun testParseXmltvEPGWithMalformedXml() {
        runBlocking {
            val xmlContent = """
                <?xml version="1.0" encoding="utf-8"?>
                <tv>
                  <programme start="20260626200000 +0200" channel="hbo.us">
                    <title lang="en">House of the Dragon
                  </programme>
                </tv>
            """.trimIndent()

            val inputStream = ByteArrayInputStream(xmlContent.toByteArray())
            // Should throw XmlPullParserException
            EpgParser.parse(inputStream, isGzip = false).toList()
        }
    }
}
