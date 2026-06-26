package com.iptv.shared.data.epg

import java.util.Locale

object EpgMatcher {

    private val filterSuffixes = setOf(
        "hd", "sd", "fhd", "uhd", "4k", "1080p", "720p", "hevc", "h264", "h.264", "h265", "h.265",
        "us", "uk", "ca", "fr", "de", "es", "it"
    )

    /**
     * Normalizes a channel name or EPG program channel ID to allow fallback matching.
     * E.g., "HBO HD (US)" -> "hbo"
     */
    fun normalize(name: String): String {
        var result = name.lowercase(Locale.US)
        
        result = result.replace(Regex("\\s*\\([^)]*\\)"), "")
        result = result.replace(Regex("\\s*\\[[^]]*\\]"), "")
        result = result.replace(Regex("[|\\-+_.:]"), " ")
        
        val tokens = result.split(Regex("\\s+"))
        val filteredTokens = tokens.filter { token ->
            token.isNotEmpty() && !filterSuffixes.contains(token)
        }
        
        return filteredTokens.joinToString("")
    }

    /**
     * Checks if a channel matches an EPG program channel ID.
     * First matches by tvg-id, then falls back to normalized name matching.
     */
    fun isMatch(channelTvgId: String?, channelName: String, programChannelId: String): Boolean {
        if (!channelTvgId.isNullOrEmpty() && channelTvgId.equals(programChannelId, ignoreCase = true)) {
            return true
        }
        
        val normalizedChannelName = normalize(channelName)
        val normalizedProgramChannelId = normalize(programChannelId)
        
        return normalizedChannelName.isNotEmpty() && normalizedChannelName == normalizedProgramChannelId
    }
}
