package io.github.RunC0deRun.shared.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

data class StreamStats(
    val resolution: String,
    val frameRate: String,
    val videoCodec: String,
    val videoBitrate: String,
    val audioCodec: String,
    val audioBitrate: String,
    val audioChannels: String,
    val audioSampleRate: String,
    val bufferAhead: String
)

@OptIn(UnstableApi::class)
object StreamStatsHelper {
    fun getStreamStats(player: ExoPlayer): StreamStats {
        var videoWidth = 0
        var videoHeight = 0
        var videoCodec = "N/A"
        var videoBitrate = -1
        var frameRate = -1f

        var audioCodec = "N/A"
        var audioBitrate = -1
        var audioChannels = -1
        var audioSampleRate = -1

        val tracks = player.currentTracks
        for (group in tracks.groups) {
            if (group.isSelected) {
                val type = group.type
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        val format = group.getTrackFormat(i)
                        if (type == C.TRACK_TYPE_VIDEO) {
                            videoWidth = format.width
                            videoHeight = format.height
                            videoCodec = format.codecs ?: format.sampleMimeType?.substringAfter("/") ?: "N/A"
                            videoBitrate = format.bitrate
                            frameRate = format.frameRate
                        } else if (type == C.TRACK_TYPE_AUDIO) {
                            audioCodec = format.codecs ?: format.sampleMimeType?.substringAfter("/") ?: "N/A"
                            audioBitrate = format.bitrate
                            audioChannels = format.channelCount
                            audioSampleRate = format.sampleRate
                        }
                    }
                }
            }
        }

        val videoSize = player.videoSize
        val actualWidth = if (videoSize.width > 0) videoSize.width else videoWidth
        val actualHeight = if (videoSize.height > 0) videoSize.height else videoHeight
        val resStr = if (actualWidth > 0 && actualHeight > 0) "${actualWidth}x${actualHeight}" else "N/A"
        val fpsStr = if (frameRate > 0) "%.2f fps".format(frameRate) else "N/A"
        val vBitrateStr = if (videoBitrate > 0) "%.2f Mbps".format(videoBitrate / 1_000_000.0) else "N/A"
        val aBitrateStr = if (audioBitrate > 0) "${audioBitrate / 1000} kbps" else "N/A"

        val channelsStr = when (audioChannels) {
            1 -> "Mono (1 ch)"
            2 -> "Stereo (2 ch)"
            6 -> "5.1 (6 ch)"
            8 -> "7.1 (8 ch)"
            in 3..Int.MAX_VALUE -> "$audioChannels channels"
            else -> "N/A"
        }

        val sampleRateStr = if (audioSampleRate > 0) "%.1f kHz".format(audioSampleRate / 1000.0) else "N/A"

        val bufferedPosition = player.bufferedPosition
        val currentPosition = player.currentPosition
        val bufferAhead = (bufferedPosition - currentPosition).coerceAtLeast(0L)
        val bufferAheadStr = "%.2f s".format(bufferAhead / 1000.0)

        return StreamStats(
            resolution = resStr,
            frameRate = fpsStr,
            videoCodec = videoCodec,
            videoBitrate = vBitrateStr,
            audioCodec = audioCodec,
            audioBitrate = aBitrateStr,
            audioChannels = channelsStr,
            audioSampleRate = sampleRateStr,
            bufferAhead = bufferAheadStr
        )
    }
}
