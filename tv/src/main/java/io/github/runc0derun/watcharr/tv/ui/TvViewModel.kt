package io.github.runc0derun.watcharr.tv.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.mvi.*
import io.github.runc0derun.watcharr.shared.playback.BaseIptvViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TvViewModel(application: Application) : BaseIptvViewModel(application) {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    private val _setupQrUrl = MutableStateFlow("")
    private val _setupStatus = MutableStateFlow("Initializing setup server...")
    private var setupServer: TvSetupServer? = null

    init {
        initPreferences()

        val onboardingDone = prefs.getBoolean("onboarding_completed", false)
        if (!onboardingDone) {
            startSetupServer()
        }

        viewModelScope.launch {
            val setupFlow = combine(
                _setupQrUrl,
                _setupStatus
            ) { qrUrl, status ->
                SetupInfo(qrUrl, status)
            }

            val tailnetFlow = combine(_isTailnetEnabled, _tailscaleAuthKey, _tsnetStatus) { enabled, key, status ->
                TailnetInfo(enabled, key, status)
            }

            val settingsFlow = combine(
                _selectedGroup,
                _isOnboardingCompleted,
                _useDispatcharr,
                _dispatcharrUrl,
                _dispatcharrUsername,
                _dispatcharrPassword,
                setupFlow,
                tailnetFlow,
                dvrInfoFlow,
                combine(_dvrRecordingMode, _dvrStorageType, _nfsSmbProtocol, _nfsSmbHost, _nfsSmbSharePath) { mode, storage, proto, host, path ->
                    DvrConfigInfo(mode, storage, proto, host, path)
                },
                combine(_nfsSmbUser, _nfsSmbPass) { user, pass ->
                    DvrCredsInfo(user, pass)
                }
            ) { args ->
                val selectedGroup = args[0] as String?
                val completed = args[1] as Boolean
                val useDispatcharr = args[2] as Boolean
                val dispatcharrUrl = args[3] as String
                val dispatcharrUsername = args[4] as String
                val dispatcharrPassword = args[5] as String
                val setupInfo = args[6] as SetupInfo
                val tailnet = args[7] as TailnetInfo
                val dvr = args[8] as DvrInfo
                val dvrConfig = args[9] as DvrConfigInfo
                val dvrCreds = args[10] as DvrCredsInfo
                SettingsInfo(
                    selectedGroup = selectedGroup,
                    isOnboardingCompleted = completed,
                    useDispatcharr = useDispatcharr,
                    dispatcharrUrl = dispatcharrUrl,
                    dispatcharrUsername = dispatcharrUsername,
                    dispatcharrPassword = dispatcharrPassword,
                    setupQrUrl = setupInfo.setupQrUrl,
                    setupStatus = setupInfo.setupStatus,
                    isTailnetEnabled = tailnet.enabled,
                    tailscaleAuthKey = tailnet.key,
                    tsnetStatus = tailnet.status,
                    recordings = dvr.recordings,
                    isDvrLoading = dvr.isDvrLoading,
                    scheduledProgramKeys = dvr.scheduledProgramKeys,
                    dvrRecordingMode = dvrConfig.mode,
                    dvrStorageType = dvrConfig.storage,
                    nfsSmbProtocol = dvrConfig.proto,
                    nfsSmbHost = dvrConfig.host,
                    nfsSmbSharePath = dvrConfig.path,
                    nfsSmbUser = dvrCreds.user,
                    nfsSmbPass = dvrCreds.pass
                )
            }

            combine(
                channelInfoFlow,
                loadingInfoFlow,
                epgInfoFlow,
                playerEngine.playbackState,
                settingsFlow
            ) { channelInfo, loadingInfo, epgInfo, playbackState, settingsInfo ->
                val searchQuery = channelInfo.searchQuery
                val favoriteUrls = channelInfo.favoriteUrls
                val channels = channelInfo.channels

                val selectedGroup = settingsInfo.selectedGroup
                val isOnboardingCompleted = settingsInfo.isOnboardingCompleted
                val useDispatcharr = settingsInfo.useDispatcharr
                val dispatcharrUrl = settingsInfo.dispatcharrUrl
                val setupQrUrl = settingsInfo.setupQrUrl
                val setupStatus = settingsInfo.setupStatus

                val filteredChannels = channels.filter { channel ->
                    val matchesGroup = when (selectedGroup) {
                        null -> true
                        "Favorites" -> favoriteUrls.contains(channel.url)
                        else -> channel.groupTitle == selectedGroup
                    }
                    val matchesSearch = if (searchQuery.isEmpty()) {
                        true
                    } else {
                        channel.name.contains(searchQuery, ignoreCase = true) ||
                                (channel.groupTitle?.contains(searchQuery, ignoreCase = true) ?: false)
                    }
                    matchesGroup && matchesSearch
                }

                val epgData = buildEpgData(filteredChannels, epgInfo)

                IptvUiState(
                    channels = filteredChannels,
                    groups = channelInfo.groups,
                    selectedGroup = selectedGroup,
                    playbackState = playbackState,
                    isLoadingPlaylist = loadingInfo.isLoadingPlaylist,
                    playlistUrlInput = loadingInfo.playlistUrlInput,
                    isLoadingEpg = loadingInfo.isLoadingEpg,
                    epgUrlInput = loadingInfo.epgUrlInput,
                    searchQuery = searchQuery,
                    favoriteUrls = favoriteUrls,
                    epgData = epgData,
                    isOnboardingCompleted = isOnboardingCompleted,
                    setupQrUrl = setupQrUrl,
                    setupStatus = setupStatus,
                    useDispatcharr = useDispatcharr,
                    dispatcharrUrl = dispatcharrUrl,
                    dispatcharrUsername = settingsInfo.dispatcharrUsername,
                    dispatcharrPassword = settingsInfo.dispatcharrPassword,
                    isTailnetEnabled = settingsInfo.isTailnetEnabled,
                    tailscaleAuthKey = settingsInfo.tailscaleAuthKey,
                    tsnetStatus = settingsInfo.tsnetStatus,
                    recordings = settingsInfo.recordings,
                    isDvrLoading = settingsInfo.isDvrLoading,
                    scheduledProgramKeys = settingsInfo.scheduledProgramKeys,
                    dvrRecordingMode = settingsInfo.dvrRecordingMode,
                    dvrStorageType = settingsInfo.dvrStorageType,
                    nfsSmbProtocol = settingsInfo.nfsSmbProtocol,
                    nfsSmbHost = settingsInfo.nfsSmbHost,
                    nfsSmbSharePath = settingsInfo.nfsSmbSharePath,
                    nfsSmbUser = settingsInfo.nfsSmbUser,
                    nfsSmbPass = settingsInfo.nfsSmbPass
                )
            }.collect { state ->
                _uiState.value = state.copy(isInitialized = true)
            }
        }
    }

    override fun onSelectChannel(channel: ChannelEntity) {
        playerEngine.setActiveChannelList(_uiState.value.channels)
        playerEngine.play(channel)
    }

    override fun uiStateFavUrls(): Set<String> {
        return _uiState.value.favoriteUrls
    }

    fun startSetupServer() {
        if (setupServer != null) return
        viewModelScope.launch {
            try {
                val ip = io.github.runc0derun.watcharr.shared.utils.NetworkUtils.getLocalIpAddress()
                if (ip == null) {
                    _setupStatus.value = "Connect to WiFi or local network to enable QR setup."
                    return@launch
                }
                
                setupServer = TvSetupServer { m3u, epg ->
                    saveConfigAndCompleteOnboarding(m3u, epg, null, false)
                }
                val port = setupServer!!.start(viewModelScope)
                val setupUrl = "http://$ip:$port/setup"
                _setupQrUrl.value = setupUrl
                _setupStatus.value = "Scan the QR code to pair device.\n(Server active at http://$ip:$port)"
            } catch (e: Exception) {
                _setupStatus.value = "Failed to start local setup server: ${e.localizedMessage}"
            }
        }
    }

    fun stopSetupServer() {
        setupServer?.stop()
        setupServer = null
    }

    fun saveConfigAndCompleteOnboarding(
        playlistUrl: String,
        epgUrl: String,
        dispatcharrUrl: String?,
        useDispatcharr: Boolean,
        dispatcharrUsername: String = "",
        dispatcharrPassword: String = "",
        dvrRecordingMode: String = "WATCHARR",
        dvrStorageType: String = "ON_DEVICE",
        nfsSmbProtocol: String = "SMB",
        nfsSmbHost: String = "",
        nfsSmbSharePath: String = "",
        nfsSmbUser: String = "",
        nfsSmbPass: String = ""
    ) {
        completeOnboarding(
            playlistUrl, epgUrl, dispatcharrUrl, useDispatcharr, dispatcharrUsername, dispatcharrPassword,
            dvrRecordingMode, dvrStorageType, nfsSmbProtocol, nfsSmbHost, nfsSmbSharePath, nfsSmbUser, nfsSmbPass
        )
        stopSetupServer()
    }

    fun playNextChannel() {
        val currentChannel = (uiState.value.playbackState as? PlaybackState.Playing)?.channel ?: return
        val list = uiState.value.channels
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.url == currentChannel.url }
        if (index != -1) {
            val nextIndex = (index + 1) % list.size
            handleIntent(PlaybackIntent.SelectChannel(list[nextIndex]))
        }
    }

    fun playPreviousChannel() {
        val currentChannel = (uiState.value.playbackState as? PlaybackState.Playing)?.channel ?: return
        val list = uiState.value.channels
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.url == currentChannel.url }
        if (index != -1) {
            val prevIndex = (index - 1 + list.size) % list.size
            handleIntent(PlaybackIntent.SelectChannel(list[prevIndex]))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSetupServer()
    }

    private data class SetupInfo(
        val setupQrUrl: String,
        val setupStatus: String
    )

    private data class TailnetInfo(
        val enabled: Boolean,
        val key: String,
        val status: String
    )

    private data class DvrConfigInfo(
        val mode: String,
        val storage: String,
        val proto: String,
        val host: String,
        val path: String
    )

    private data class DvrCredsInfo(
        val user: String,
        val pass: String
    )

    private data class SettingsInfo(
        val selectedGroup: String?,
        val isOnboardingCompleted: Boolean,
        val useDispatcharr: Boolean,
        val dispatcharrUrl: String,
        val dispatcharrUsername: String,
        val dispatcharrPassword: String,
        val setupQrUrl: String,
        val setupStatus: String,
        val isTailnetEnabled: Boolean,
        val tailscaleAuthKey: String,
        val tsnetStatus: String,
        val recordings: List<io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording>,
        val isDvrLoading: Boolean,
        val scheduledProgramKeys: Set<String>,
        val dvrRecordingMode: String,
        val dvrStorageType: String,
        val nfsSmbProtocol: String,
        val nfsSmbHost: String,
        val nfsSmbSharePath: String,
        val nfsSmbUser: String,
        val nfsSmbPass: String
    )
}
