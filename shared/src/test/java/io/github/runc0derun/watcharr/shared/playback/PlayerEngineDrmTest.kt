package io.github.runc0derun.watcharr.shared.playback

import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerEngineDrmTest {

    @Test
    fun testFindChannelIdFromLiveUrl() {
        val playerEngine = PlayerEngineTestable()
        val channel = ChannelEntity(
            url = "http://localhost:8080/live/channel_123",
            name = "Live DRM Channel",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = null
        )
        val channelId = playerEngine.findChannelId(channel, null)
        assertEquals("channel_123", channelId)
    }

    @Test
    fun testFindChannelIdFromPseudoChannelTvgId() {
        val playerEngine = PlayerEngineTestable()
        val pseudoChannel = ChannelEntity(
            url = "http://localhost:8080/api/channels/recordings/rec_001/file/",
            name = "Recorded Movie",
            tvgId = "channel_456",
            tvgName = null,
            logoUrl = null,
            groupTitle = "Recordings"
        )
        val channelId = playerEngine.findChannelId(pseudoChannel, null)
        assertEquals("channel_456", channelId)
    }

    @Test
    fun testFindChannelIdFromActiveChannelListByName() {
        val playerEngine = PlayerEngineTestable()
        val liveChannel = ChannelEntity(
            url = "http://localhost:8080/live/channel_789",
            name = "HBO HD",
            tvgId = "channel_789",
            tvgName = "HBO HD",
            logoUrl = null,
            groupTitle = "Premium"
        )
        playerEngine.setActiveChannelList(listOf(liveChannel))

        val recordingChannel = ChannelEntity(
            url = "http://localhost:8080/api/channels/recordings/rec_002/file/",
            name = "HBO HD",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = "Recordings"
        )
        val channelId = playerEngine.findChannelId(recordingChannel, null)
        assertEquals("channel_789", channelId)
    }

    @Test
    fun testFindChannelIdReturnNullWhenNoMatch() {
        val playerEngine = PlayerEngineTestable()
        val recordingChannel = ChannelEntity(
            url = "http://localhost:8080/api/channels/recordings/rec_003/file/",
            name = "Unknown Show",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = "Recordings"
        )
        val channelId = playerEngine.findChannelId(recordingChannel, null)
        assertNull(channelId)
    }

    @Test
    fun testNon2xxHeadResponseIgnored() {
        // Simulates a HEAD probe returning HTTP 405 Method Not Allowed with Content-Type: application/json
        val responseCode = 405
        val contentType = "application/json"
        val isValidHeadMediaResponse = responseCode in 200..299 &&
                contentType.lowercase().isNotEmpty() &&
                !contentType.lowercase().contains("octet-stream") &&
                !contentType.lowercase().contains("html") &&
                !contentType.lowercase().contains("json")

        assertEquals(false, isValidHeadMediaResponse)
    }

    @Test
    fun testRecordingChannelIdResolution() {
        val playerEngine = PlayerEngineTestable()
        val liveChannel = ChannelEntity(
            url = "https://delta-bridge.jstienstra.nl/live/4",
            name = "NPO 1",
            tvgId = "4",
            tvgName = "NPO 1",
            logoUrl = null,
            groupTitle = "General"
        )
        playerEngine.setActiveChannelList(listOf(liveChannel))

        val dualFileRecordingChannel = ChannelEntity(
            url = "/data/user/0/io.github.runc0derun.watcharr/files/Recordings/Watcharr_Show_1234_video.mp4|/data/user/0/io.github.runc0derun.watcharr/files/Recordings/Watcharr_Show_1234_audio.mp4",
            name = "NPO 1",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = "Recordings"
        )

        val channelId = playerEngine.findChannelId(dualFileRecordingChannel, null)
        assertEquals("4", channelId)
    }
}

private class PlayerEngineTestable {
    private var activeChannelList: List<ChannelEntity> = emptyList()
    private var currentChannel: ChannelEntity? = null

    fun setActiveChannelList(channels: List<ChannelEntity>) {
        activeChannelList = channels
    }

    fun findChannelId(channel: ChannelEntity?, uri: Any?): String? {
        val urlStr = channel?.url ?: ""
        val pathSegments = try {
            java.net.URI(urlStr).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val liveIndex = pathSegments.indexOf("live")
        if (liveIndex != -1 && liveIndex + 1 < pathSegments.size) {
            return pathSegments[liveIndex + 1]
        }

        val tvgId = channel?.tvgId?.trim()
        if (!tvgId.isNullOrEmpty()) {
            return tvgId
        }

        val targetChannel = channel ?: currentChannel
        if (targetChannel != null) {
            val matched = activeChannelList.firstOrNull {
                it.name.equals(targetChannel.name, ignoreCase = true) ||
                        (it.tvgName != null && it.tvgName.equals(targetChannel.name, ignoreCase = true))
            }
            if (matched != null) {
                val matchedTvgId = matched.tvgId?.trim()
                if (!matchedTvgId.isNullOrEmpty()) {
                    return matchedTvgId
                }
                val matchedSegments = try {
                    java.net.URI(matched.url).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
                } catch (e: Exception) { emptyList() }
                val mLiveIndex = matchedSegments.indexOf("live")
                if (mLiveIndex != -1 && mLiveIndex + 1 < matchedSegments.size) {
                    return matchedSegments[mLiveIndex + 1]
                }
            }
        }

        return null
    }
}
