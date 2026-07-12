package io.github.runc0derun.shared.data.parser

import io.github.runc0derun.shared.data.db.ProgramEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

object EpgParser {

    private val xmltvFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.US)
    private val fallbackFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)

    /**
     * Incrementally parses XMLTV events sequentially from raw or gzip stream.
     * Emits ProgramEntity as they are parsed to avoid loading full guide into memory.
     */
    fun parse(inputStream: InputStream, isGzip: Boolean): Flow<ProgramEntity> = flow {
        inputStream.use { rawStream ->
            val stream = if (isGzip) GZIPInputStream(rawStream) else rawStream
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(stream, null)

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentStart = 0L
        var currentStop = 0L
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentIconUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentStart = if (startStr != null) parseXmltvDate(startStr) else 0L
                            currentStop = if (stopStr != null) parseXmltvDate(stopStr) else 0L
                        }
                        "title" -> if (currentChannelId != null) currentTitle = parser.nextText()
                        "desc" -> if (currentChannelId != null) currentDesc = parser.nextText()
                        "icon" -> if (currentChannelId != null) currentIconUrl = parser.getAttributeValue(null, "src")
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name == "programme") {
                        if (currentChannelId != null && currentTitle != null) {
                            emit(
                                ProgramEntity(
                                    channelId = currentChannelId,
                                    start = currentStart,
                                    stop = currentStop,
                                    title = currentTitle,
                                    desc = currentDesc,
                                    iconUrl = currentIconUrl
                                )
                            )
                        }
                        currentChannelId = null
                        currentStart = 0L
                        currentStop = 0L
                        currentTitle = null
                        currentDesc = null
                        currentIconUrl = null
                    }
                }
            }
            eventType = parser.next()
        }
        }
    }

    private fun parseXmltvDate(dateStr: String): Long {
        val formatted = if (!dateStr.contains(" ") && (dateStr.contains("+") || dateStr.contains("-"))) {
            val signIndex = dateStr.indexOfFirst { it == '+' || it == '-' }
            dateStr.substring(0, signIndex) + " " + dateStr.substring(signIndex)
        } else {
            dateStr
        }
        return try {
            java.time.ZonedDateTime.parse(formatted, xmltvFormatter).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                java.time.LocalDateTime.parse(formatted, fallbackFormatter)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (ex: Exception) {
                0L
            }
        }
    }
}
