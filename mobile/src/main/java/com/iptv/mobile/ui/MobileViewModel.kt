package com.iptv.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.mobile.MobileApp
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.db.ProgramEntity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MobileViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MobileApp
    private val channelDao = app.database.channelDao()
    private val programDao = app.database.programDao()
    
    val playerEngine = PlayerEngine(application)

    private val _isLoadingPlaylist = MutableStateFlow(false)
    private val _playlistUrlInput = MutableStateFlow("")
    private val _selectedGroup = MutableStateFlow<String?>(null)

    private val _isLoadingEpg = MutableStateFlow(false)
    private val _epgUrlInput = MutableStateFlow("")

    private val _sideEffects = MutableSharedFlow<PlaybackSideEffect>()
    val sideEffects: SharedFlow<PlaybackSideEffect> = _sideEffects.asSharedFlow()

    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            combine(
                combine(channelDao.getAllChannelsFlow(), channelDao.getUniqueGroupsFlow(), ::Pair),
                combine(_selectedGroup, playerEngine.playbackState, ::Pair),
                combine(
                    combine(_isLoadingPlaylist, _playlistUrlInput, ::Pair),
                    combine(_isLoadingEpg, _epgUrlInput, ::Pair),
                    ::Pair
                ),
                combine(
                    programDao.getActiveProgramsFlow(now),
                    programDao.getAllUpcomingProgramsFlow(now),
                    ::Pair
                )
            ) { p1, p2, p3, p4 ->
                val (channels, groups) = p1
                val (selectedGroup, playbackState) = p2
                val (playlistInfo, epgInfo) = p3
                val (isLoadingPlaylist, playlistUrl) = playlistInfo
                val (isLoadingEpg, epgUrl) = epgInfo
                val (activeProgs, upcomingProgs) = p4

                val filteredChannels = if (selectedGroup == null) {
                    channels
                } else {
                    channels.filter { it.groupTitle == selectedGroup }
                }

                val epgData = filteredChannels.associate { channel ->
                    val current = activeProgs.firstOrNull {
                        EpgMatcher.isMatch(channel.tvgId, channel.name, it.channelId)
                    }
                    val next = upcomingProgs.firstOrNull {
                        EpgMatcher.isMatch(channel.tvgId, channel.name, it.channelId) && (current == null || it.start >= current.stop)
                    }
                    channel.url to Pair(current, next)
                }

                MobileUiState(
                    channels = filteredChannels,
                    groups = groups,
                    selectedGroup = selectedGroup,
                    playbackState = playbackState,
                    isLoadingPlaylist = isLoadingPlaylist,
                    playlistUrlInput = playlistUrl,
                    isLoadingEpg = isLoadingEpg,
                    epgUrlInput = epgUrl,
                    epgData = epgData
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun handleIntent(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.LoadPlaylist -> {
                loadPlaylist(intent.m3uUrl)
            }
            is PlaybackIntent.SelectChannel -> {
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

    private fun loadPlaylist(m3uUrl: String) {
        viewModelScope.launch {
            _isLoadingPlaylist.value = true
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = URL(m3uUrl).openStream()
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
    val epgData: Map<String, Pair<ProgramEntity?, ProgramEntity?>> = emptyMap()
)
