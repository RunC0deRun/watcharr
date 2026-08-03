package io.github.runc0derun.watcharr.shared.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelEntityDrmTest {

    @Test
    fun testIsDrmChannelWithLiveProxyUrl() {
        val channel = ChannelEntity(
            url = "https://delta-bridge.jstienstra.nl/live/4",
            name = "NPO 1",
            tvgId = "4",
            tvgName = "NPO 1",
            logoUrl = null,
            groupTitle = null
        )
        assertTrue(channel.isDrmChannel())
    }

    @Test
    fun testIsDrmChannelWithLicenseKeyword() {
        val channel = ChannelEntity(
            url = "http://example.com/stream?license=widevine",
            name = "DRM Stream",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = null
        )
        assertTrue(channel.isDrmChannel())
    }

    @Test
    fun testIsDrmChannelWithIsDrmPropertyTrue() {
        val channel = ChannelEntity(
            url = "http://example.com/stream/4",
            name = "Channel 4",
            tvgId = "4",
            tvgName = "Channel 4",
            logoUrl = null,
            groupTitle = null,
            isDrm = true
        )
        assertTrue(channel.isDrmChannel())
    }

    @Test
    fun testIsDrmChannelWithNonDrmHlsUrl() {
        val channel = ChannelEntity(
            url = "https://example.com/live/channel.m3u8",
            name = "Normal HLS Stream",
            tvgId = null,
            tvgName = null,
            logoUrl = null,
            groupTitle = null
        )
        assertFalse(channel.isDrmChannel())
    }
}
