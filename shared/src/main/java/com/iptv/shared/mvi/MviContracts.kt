package com.iptv.shared.mvi

import com.iptv.shared.data.db.ChannelEntity

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Loading : PlaybackState
    data class Playing(val channel: ChannelEntity) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

sealed interface PlaybackIntent {
    data class LoadPlaylist(val m3uUrl: String) : PlaybackIntent
    data class SelectChannel(val channel: ChannelEntity) : PlaybackIntent
    data object TogglePlay : PlaybackIntent
}

sealed interface PlaybackSideEffect {
    data class ShowToast(val message: String) : PlaybackSideEffect
}
