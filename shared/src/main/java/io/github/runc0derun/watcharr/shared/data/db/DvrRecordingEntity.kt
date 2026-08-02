package io.github.runc0derun.watcharr.shared.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dvr_recordings",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["startEpochMs"]),
        Index(value = ["status"])
    ]
)
data class DvrRecordingEntity(
    @PrimaryKey
    val id: String,
    val programTitle: String,
    val channelName: String,
    val channelId: String,
    val startEpochMs: Long,
    val stopEpochMs: Long,
    val status: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val description: String? = null,
    val localFilePath: String? = null,
    val recordingEngine: String = "WATCHARR", // "WATCHARR" or "DISPATCHARR"
    val storageType: String = "ON_DEVICE",    // "ON_DEVICE" or "NETWORK_SHARE"
    val isDrmDecrypted: Boolean = false,
    val drmKeySetId: String? = null,
    val errorReason: String? = null
)
