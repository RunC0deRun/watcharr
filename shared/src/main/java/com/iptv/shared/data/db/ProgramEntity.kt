package com.iptv.shared.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["channelId"]),
        Index(value = ["start"]),
        Index(value = ["stop"])
    ]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: String,
    val start: Long,
    val stop: Long,
    val title: String,
    val desc: String?
)
