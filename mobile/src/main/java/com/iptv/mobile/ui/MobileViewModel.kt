package com.iptv.mobile.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.mobile.MobileApp
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
import com.iptv.shared.data.db.FavoriteEntity
import com.iptv.shared.data.db.FavoriteDao
import com.iptv.shared.data.epg.EpgFetcher
import com.iptv.shared.data.epg.EpgMatcher
import com.iptv.shared.data.epg.EpgSyncWorker
import com.iptv.shared.data.parser.M3uParser
import com.iptv.shared.mvi.PlaybackIntent
import com.iptv.shared.mvi.PlaybackSideEffect
import com.iptv.shared.mvi.PlaybackState
import com.iptv.shared.playback.PlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MobileApp
    private val channelDao = app.database.channelDao()
    private val programDao = app.database.programDao()
    private val favoriteDao = app.database.favoriteDao()
    
    val playerEngine = com.iptv.shared.playback.PlayerEngineProvider.get(application)

    private val _isLoadingPlaylist = MutableStateFlow(false)
    private val _playlistUrlInput = MutableStateFlow("")
    private val _selectedGroup = MutableStateFlow<String?>(null)

    private val _isLoadingEpg = MutableStateFlow(false)
    private val _epgUrlInput = MutableStateFlow("")

    private val _searchQuery = MutableStateFlow("")

    private val _sideEffects = MutableSharedFlow<PlaybackSideEffect>()
    val sideEffects: SharedFlow<PlaybackSideEffect> = _sideEffects.asSharedFlow()

    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(false)
    private val _useDispatcharr = MutableStateFlow(false)
    private val _dispatcharrUrl = MutableStateFlow("")

    private val prefs = application.getSharedPreferences("watcharr_prefs", Context.MODE_PRIVATE)

    private data class ChannelInfo(
        val channels: List<ChannelEntity>,
        val groups: List<String>,
        val searchQuery: String,
        val favoriteUrls: Set<String>
    )

    private data class LoadingInfo(
        val isLoadingPlaylist: Boolean,
        val playlistUrlInput: String,
        val isLoadingEpg: Boolean,
        val epgUrlInput: String
    )

    private data class EpgInfo(
        val activePrograms: List<ProgramEntity>,
        val upcomingPrograms: List<ProgramEntity>
    )

    private data class SettingsInfo(
        val selectedGroup: String?,
        val isOnboardingCompleted: Boolean,
        val useDispatcharr: Boolean,
        val dispatcharrUrl: String
    )

    init {
        val onboardingDone = prefs.getBoolean("onboarding_completed", false)
        _isOnboardingCompleted.value = onboardingDone
        _playlistUrlInput.value = prefs.getString("playlist_url", "") ?: ""
        _epgUrlInput.value = prefs.getString("epg_url", "") ?: ""
        _useDispatcharr.value = prefs.getBoolean("use_dispatcharr", false)
        _dispatcharrUrl.value = prefs.getString("dispatcharr_url", "") ?: ""

        viewModelScope.launch {
            val channelInfoFlow = combine(
                channelDao.getAllChannelsFlow(),
                channelDao.getUniqueGroupsFlow(),
                _searchQuery,
                favoriteDao.getFavoriteUrlsFlow()
            ) { channels, groups, search, favorites ->
                ChannelInfo(channels, groups, search, favorites.toSet())
            }

            val loadingInfoFlow = combine(
                _isLoadingPlaylist,
                _playlistUrlInput,
                _isLoadingEpg,
                _epgUrlInput
            ) { isLoadPlaylist, playlistUrl, isLoadEpg, epgUrl ->
                LoadingInfo(isLoadPlaylist, playlistUrl, isLoadEpg, epgUrl)
            }

            val nowFlow = kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    kotlinx.coroutines.delay(30000)
                }
            }

            val epgInfoFlow = nowFlow.flatMapLatest { now ->
                combine(
                    programDao.getActiveProgramsFlow(now),
                    programDao.getAllUpcomingProgramsFlow(now)
                ) { active, upcoming ->
                    EpgInfo(active, upcoming)
                }
            }

            val settingsFlow = combine(
                _selectedGroup,
                _isOnboardingCompleted,
                _useDispatcharr,
                _dispatcharrUrl
            ) { selectedGroup, completed, useDispatcharr, dispatcharrUrl ->
                SettingsInfo(selectedGroup, completed, useDispatcharr, dispatcharrUrl)
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

                val epgData = filteredChannels.associate { channel ->
                    val current = epgInfo.activePrograms.firstOrNull {
                        EpgMatcher.isMatch(channel.tvgId, channel.name, it.channelId)
                    }
                    val upcoming = epgInfo.upcomingPrograms.filter {
                        EpgMatcher.isMatch(channel.tvgId, channel.name, it.channelId)
                    }
                    
                    val programs = mutableListOf<ProgramEntity>()
                    if (current != null) {
                        programs.add(current)
                    }
                    programs.addAll(upcoming)
                    channel.url to programs
                }

                MobileUiState(
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
                    useDispatcharr = useDispatcharr,
                    dispatcharrUrl = dispatcharrUrl
                )
            }.collect { state ->
                _uiState.value = state.copy(isInitialized = true)
            }
        }
    }

    fun completeOnboarding(playlistUrl: String, epgUrl: String, dispatcharrUrl: String?, useDispatcharr: Boolean) {
        viewModelScope.launch {
            prefs.edit().apply {
                putString("playlist_url", playlistUrl)
                putString("epg_url", epgUrl)
                putString("dispatcharr_url", dispatcharrUrl)
                putBoolean("use_dispatcharr", useDispatcharr)
                putBoolean("onboarding_completed", true)
                apply()
            }

            _playlistUrlInput.value = playlistUrl
            _epgUrlInput.value = epgUrl
            _useDispatcharr.value = useDispatcharr
            _dispatcharrUrl.value = dispatcharrUrl ?: ""
            _isOnboardingCompleted.value = true

            if (playlistUrl.isNotEmpty()) {
                handleIntent(PlaybackIntent.LoadPlaylist(playlistUrl))
            }
            if (epgUrl.isNotEmpty()) {
                loadEpg(epgUrl)
            }
        }
    }

    fun sendConfigToTv(tvSetupUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val m3u = prefs.getString("playlist_url", "") ?: ""
                val epg = prefs.getString("epg_url", "") ?: ""

                if (m3u.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onError("Mobile app is not configured yet!")
                    }
                    return@launch
                }

                val json = org.json.JSONObject()
                json.put("playlistUrl", m3u)
                json.put("epgUrl", epg)
                val jsonBytes = json.toString().toByteArray()

                val url = URL(tvSetupUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                try {
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { os ->
                        os.write(jsonBytes)
                    }

                    val code = conn.responseCode
                    if (code == 200) {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError("TV server returned error code: $code")
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Failed to connect to TV: ${e.localizedMessage}")
                }
            }
        }
    }

    fun handleIntent(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.LoadPlaylist -> {
                loadPlaylist(intent.m3uUrl)
            }
            is PlaybackIntent.SelectChannel -> {
                playerEngine.setActiveChannelList(_uiState.value.channels)
                playerEngine.play(intent.channel)
            }
            is PlaybackIntent.TogglePlay -> {
                playerEngine.togglePlay()
            }
        }
    }

    fun updateUrlInput(url: String) {
        _playlistUrlInput.value = url
    }

    fun updateEpgUrlInput(url: String) {
        _epgUrlInput.value = url
    }

    fun selectGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(channelUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (_uiState.value.favoriteUrls.contains(channelUrl)) {
                    favoriteDao.delete(channelUrl)
                } else {
                    favoriteDao.insert(FavoriteEntity(channelUrl))
                }
            }
        }
    }

    private fun loadPlaylist(m3uUrl: String) {
        viewModelScope.launch {
            _isLoadingPlaylist.value = true
            try {
                withContext(Dispatchers.IO) {
                    URL(m3uUrl).openStream().use { inputStream ->
                        channelDao.deleteAll()
                        
                        val batch = mutableListOf<ChannelEntity>()
                        val batchSize = 1000
                        
                        M3uParser.parse(inputStream).collect { track ->
                            batch.add(track.toEntity())
                            if (batch.size >= batchSize) {
                                channelDao.insertAll(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) {
                            channelDao.insertAll(batch)
                        }
                    }
                }
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Playlist loaded successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Failed to load playlist: ${e.localizedMessage}"))
            } finally {
                _isLoadingPlaylist.value = false
            }
        }
    }

    fun loadEpg(epgUrl: String) {
        viewModelScope.launch {
            _isLoadingEpg.value = true
            try {
                withContext(Dispatchers.IO) {
                    val fetcher = EpgFetcher(app)
                    val result = fetcher.fetchAndSyncEpg(epgUrl)
                    if (result.isFailure) throw result.exceptionOrNull()!!
                }
                EpgSyncWorker.schedule(app, epgUrl)
                _sideEffects.emit(PlaybackSideEffect.ShowToast("EPG guide loaded & daily sync scheduled successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Failed to load EPG: ${e.localizedMessage}"))
            } finally {
                _isLoadingEpg.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}

data class MobileUiState(
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
    val isInitialized: Boolean = false
)
