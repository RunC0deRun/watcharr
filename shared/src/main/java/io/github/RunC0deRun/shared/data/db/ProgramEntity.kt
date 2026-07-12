package io.github.runc0derun.shared.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "programs",
    primaryKeys = ["channelId", "start"],
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["start"]),
        Index(value = ["stop"])
    ]
)
data class ProgramEntity(
    val channelId: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val desc: String?,
    val iconUrl: String? = null
)
