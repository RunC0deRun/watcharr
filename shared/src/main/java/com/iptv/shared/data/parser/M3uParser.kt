package com.iptv.shared.data.parser

import com.iptv.shared.data.db.ChannelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object M3uParser {
    /**
     * Parses an M3U stream line-by-line and emits channels as they are found.
     * This avoids memory spikes when loading large playlists.
     */
    fun parse(inputStream: InputStream): Flow<ChannelEntity> = flow {
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            var currentMetadata: Map<String, String>? = null
            var currentName: String? = null

            val extinfPrefix = "#EXTINF:"
            
            while (reader.readLine().also { line = it } != null) {
                val trimmedLine = line!!.trim()
                if (trimmedLine.isEmpty()) continue

                if (trimmedLine.startsWith(extinfPrefix)) {
                    val rest = trimmedLine.substring(extinfPrefix.length)
                    val parsed = parseExtInfLine(rest)
                    currentMetadata = parsed.first
                    currentName = parsed.second
                } else if (!trimmedLine.startsWith("#")) {
                    if (currentName != null) {
                        val url = trimmedLine
                        if (url.isNotEmpty()) {
                            emit(
                                ChannelEntity(
                                    name = currentName,
                                    url = url,
                                    tvgId = currentMetadata?.get("tvg-id"),
                                    tvgName = currentMetadata?.get("tvg-name"),
                                    logoUrl = currentMetadata?.get("tvg-logo"),
                                    groupTitle = currentMetadata?.get("group-title")
                                )
                            )
                        }
                        currentMetadata = null
                        currentName = null
                    }
                }
            }
        }
    }

    private fun parseExtInfLine(line: String): Pair<Map<String, String>, String> {
        val metadata = mutableMapOf<String, String>()
        
        var commaIndex = -1
        var inQuotes = false
        for (i in line.indices) {
            val char = line[i]
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                commaIndex = i
                break
            }
        }
        
        val propertiesStr = if (commaIndex != -1) line.substring(0, commaIndex) else line
        val displayName = if (commaIndex != -1 && commaIndex + 1 < line.length) {
            line.substring(commaIndex + 1).trim()
        } else {
            ""
        }
        
        var index = 0
        while (index < propertiesStr.length) {
            val eqIndex = propertiesStr.indexOf('=', index)
            if (eqIndex == -1) break
            
            val key = propertiesStr.substring(index, eqIndex).trim().substringAfterLast(' ').substringAfterLast('\t')
            
            val startQuote = propertiesStr.indexOf('"', eqIndex)
            if (startQuote == -1) break
            
            val endQuote = propertiesStr.indexOf('"', startQuote + 1)
            if (endQuote == -1) break
            
            val value = propertiesStr.substring(startQuote + 1, endQuote)
            metadata[key.lowercase()] = value
            
            index = endQuote + 1
        }
        
        return Pair(metadata, displayName)
    }
}
