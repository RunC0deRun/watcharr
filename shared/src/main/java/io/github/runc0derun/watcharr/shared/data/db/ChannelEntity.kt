package io.github.runc0derun.watcharr.shared.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.core.net.toUri

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
    val groupTitle: String?,
    val isDrm: Boolean = false
) {
    fun isRadioOrAudioOnly(): Boolean {
        val group = groupTitle?.lowercase() ?: ""
        val chName = name.lowercase()
        return group.contains("radio") || group.contains("audio") || group.contains("music") || group.contains("podcast") ||
                chName.contains("radio") || chName.contains(" fm ") || chName.endsWith(" fm") || chName.startsWith("fm ") || chName.contains(" am ") || chName.contains("audio") || chName.contains("music")
    }

    fun isDrmChannel(): Boolean {
        if (isDrm) return true
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains("license") || lowerUrl.contains("widevine")) return true
        val pathSegments = try {
            java.net.URI(url).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val liveIndex = pathSegments.indexOf("live")
        val isHls = lowerUrl.contains(".m3u8")
        if (liveIndex != -1 && pathSegments.size == liveIndex + 2 && !isHls) {
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
                    .setArtworkUri(logoUrl?.toUri())
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build()
            )
            .build()
    }
}
