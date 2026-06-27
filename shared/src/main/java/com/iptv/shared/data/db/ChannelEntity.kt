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
) {
    fun isRadioOrAudioOnly(): Boolean {
        val group = groupTitle?.lowercase() ?: ""
        if (group.contains("radio") || group.contains("audio") || group.contains("music") || group.contains("podcast")) {
            return true
        }
        val chName = name.lowercase()
        if (chName.contains("radio") || chName.contains(" fm ") || chName.endsWith(" fm") || chName.startsWith("fm ") || chName.contains(" am ") || chName.contains("audio") || chName.contains("music")) {
            return true
        }
        return false
    }

    fun toMediaItem(currentProgramName: String? = null): androidx.media3.common.MediaItem {
        return androidx.media3.common.MediaItem.Builder()
            .setMediaId(url)
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(name)
                    .setSubtitle(currentProgramName ?: groupTitle ?: "")
                    .setDisplayTitle(name)
                    .setArtworkUri(logoUrl?.let { android.net.Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }
}
