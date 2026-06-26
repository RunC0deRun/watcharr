package com.iptv.shared.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["groupTitle"]),
        Index(value = ["tvgId"])
    ]
)
data class ChannelEntity(
    @PrimaryKey
    val url: String,
    val name: String,
    val tvgId: String?,
    val tvgName: String?,
    val logoUrl: String?,
    val groupTitle: String?
)
