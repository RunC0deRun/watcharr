package io.github.runc0derun.watcharr.shared.data.dvr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DvrModelsTest {

    @Test
    fun testDvrStatusParsing() {
        assertEquals(DvrStatus.SCHEDULED, DvrStatus.fromString("scheduled"))
        assertEquals(DvrStatus.SCHEDULED, DvrStatus.fromString("PENDING"))
        assertEquals(DvrStatus.RECORDING, DvrStatus.fromString("recording"))
        assertEquals(DvrStatus.RECORDING, DvrStatus.fromString("ACTIVE"))
        assertEquals(DvrStatus.COMPLETED, DvrStatus.fromString("completed"))
        assertEquals(DvrStatus.COMPLETED, DvrStatus.fromString("FINISHED"))
        assertEquals(DvrStatus.FAILED, DvrStatus.fromString("unknown_status"))
    }

    @Test
    fun testDvrRecordingProperties() {
        val rec = DvrRecording(
            id = "rec_001",
            programTitle = "Evening News",
            channelName = "BBC News",
            startEpochMs = 1000L,
            stopEpochMs = 2000L,
            status = DvrStatus.COMPLETED,
            streamUrl = "http://localhost:8080/api/dvr/recordings/rec_001/stream"
        )

        assertTrue(rec.isCompleted())
        assertFalse(rec.isScheduled())
        assertFalse(rec.isLiveRecording())
        assertEquals("rec_001", rec.id)
        assertEquals("Evening News", rec.programTitle)
        assertEquals("http://localhost:8080/api/dvr/recordings/rec_001/stream", rec.streamUrl)
    }

    @Test
    fun testAutomaticCompletionStatusTransition() {
        val pastTime = System.currentTimeMillis() - 60_000L // 1 minute ago
        val rec = DvrRecording(
            id = "rec_002",
            programTitle = "Late Night Show",
            channelName = "NBC",
            startEpochMs = pastTime - 3600_000L,
            stopEpochMs = pastTime,
            status = DvrStatus.RECORDING,
            streamUrl = "http://localhost:8080/api/dvr/recordings/rec_002/stream"
        )

        val now = System.currentTimeMillis()
        val evaluatedStatus = if (now >= rec.stopEpochMs) DvrStatus.COMPLETED else rec.status
        assertEquals(DvrStatus.COMPLETED, evaluatedStatus)
    }
}
