package io.github.RunC0deRun.shared.mvi

import io.github.RunC0deRun.shared.data.db.ChannelEntity
import io.github.RunC0deRun.shared.data.db.ProgramEntity

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data object Loading : PlaybackState
    data class Playing(val channel: ChannelEntity, val isVideoRestricted: Boolean = false) : PlaybackState
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

data class IptvUiState(
    val channels: List<ChannelEntity> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isLoadingPlaylist: Boolean = false,
    val playlistUrlInput: String = "",
    val isLoadingEpg: Boolean = false,
    val epgUrlInput: String = "",
    val searchQuery: String = "",
    val favoriteUrls: Set<String> = emptySet(),
    val epgData: Map<String, List<ProgramEntity>> = emptyMap(),
    val isOnboardingCompleted: Boolean = false,
    val useDispatcharr: Boolean = false,
    val dispatcharrUrl: String = "",
    val setupQrUrl: String = "",
    val setupStatus: String = "",
    val isInitialized: Boolean = false
)
