package io.github.runc0derun.watcharr.shared.data.dvr

enum class DvrStatus {
    SCHEDULED,
    RECORDING,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String): DvrStatus {
            return when (value.uppercase()) {
                "SCHEDULED", "PENDING" -> SCHEDULED
                "RECORDING", "IN_PROGRESS", "ACTIVE" -> RECORDING
                "COMPLETED", "FINISHED", "DONE" -> COMPLETED
                else -> FAILED
            }
        }
    }
}

data class DvrRecording(
    val id: String,
    val programTitle: String,
    val channelName: String,
    val channelId: String? = null,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val status: DvrStatus,
    val streamUrl: String,
    val posterUrl: String? = null,
    val description: String? = null
) {
    fun isLiveRecording(): Boolean = status == DvrStatus.RECORDING
    fun isScheduled(): Boolean = status == DvrStatus.SCHEDULED
    fun isCompleted(): Boolean = status == DvrStatus.COMPLETED
}

data class ScheduleRecordingRequest(
    val programTitle: String,
    val channelName: String,
    val channelId: String,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val description: String? = null,
    val posterUrl: String? = null
)
