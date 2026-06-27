package com.iptv.shared.data.parser

import com.iptv.shared.data.db.ProgramEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

object EpgParser {

    private val xmltvDateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    /**
     * Incrementally parses XMLTV events sequentially from raw or gzip stream.
     * Emits ProgramEntity as they are parsed to avoid loading full guide into memory.
     */
    fun parse(inputStream: InputStream, isGzip: Boolean): Flow<ProgramEntity> = flow {
        val stream = if (isGzip) GZIPInputStream(inputStream) else inputStream
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(stream, null)

        var eventType = parser.eventType
        var currentChannelId: String? = null
        var currentStart: Long = 0L
        var currentStop: Long = 0L
        var currentTitle: String? = null
        var currentDesc: String? = null
        var currentIconUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name == "programme") {
                        currentChannelId = parser.getAttributeValue(null, "channel")
                        val startStr = parser.getAttributeValue(null, "start")
                        val stopStr = parser.getAttributeValue(null, "stop")
                        currentStart = if (startStr != null) parseXmltvDate(startStr) else 0L
                        currentStop = if (stopStr != null) parseXmltvDate(stopStr) else 0L
                    } else if (name == "title" && currentChannelId != null) {
                        currentTitle = parser.nextText()
                    } else if (name == "desc" && currentChannelId != null) {
                        currentDesc = parser.nextText()
                    } else if (name == "icon" && currentChannelId != null) {
                        currentIconUrl = parser.getAttributeValue(null, "src")
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

    private fun parseXmltvDate(dateStr: String): Long {
        val formatted = if (!dateStr.contains(" ") && (dateStr.contains("+") || dateStr.contains("-"))) {
            val signIndex = dateStr.indexOfFirst { it == '+' || it == '-' }
            dateStr.substring(0, signIndex) + " " + dateStr.substring(signIndex)
        } else {
            dateStr
        }
        return try {
            xmltvDateFormat.parse(formatted)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val fallbackFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                fallbackFormat.parse(formatted)?.time ?: 0L
            } catch (ex: Exception) {
                0L
            }
        }
    }
}
