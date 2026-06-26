package com.iptv.shared.data.parser

import com.iptv.shared.data.db.ChannelEntity

data class M3uTrack(
    val name: String,
    val url: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null
) {
    fun toEntity(): ChannelEntity {
        return ChannelEntity(
            url = url,
            name = name,
            tvgId = tvgId,
            tvgName = tvgName,
            logoUrl = logoUrl,
            groupTitle = groupTitle
        )
    }
}
