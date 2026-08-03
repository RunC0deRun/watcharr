package io.github.runc0derun.watcharr.shared.mvi

import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.data.db.ProgramEntity
import io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording

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
    data object FetchRecordings : PlaybackIntent
    data class ScheduleRecording(val program: ProgramEntity, val channel: ChannelEntity? = null) : PlaybackIntent
    data class CancelRecording(val recordingId: String) : PlaybackIntent
    data class PlayRecording(val recording: DvrRecording) : PlaybackIntent
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
    val dispatcharrUsername: String = "",
    val dispatcharrPassword: String = "",
    val setupQrUrl: String = "",
    val setupStatus: String = "",
    val isInitialized: Boolean = false,
    val isTailnetEnabled: Boolean = false,
    val tailscaleAuthKey: String = "",
    val tsnetStatus: String = "",
    val recordings: List<DvrRecording> = emptyList(),
    val isDvrLoading: Boolean = false,
    val scheduledProgramKeys: Set<String> = emptySet(),
    val dvrRecordingMode: String = "WATCHARR",
    val dvrStorageType: String = "ON_DEVICE",
    val nfsSmbProtocol: String = "SMB",
    val nfsSmbHost: String = "",
    val nfsSmbSharePath: String = "",
    val nfsSmbUser: String = "",
    val nfsSmbPass: String = ""
)

