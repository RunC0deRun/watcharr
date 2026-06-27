package com.iptv.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.tv.TvApp
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class TvViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TvApp
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

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

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

    init {
        val now = System.currentTimeMillis()
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

            val epgInfoFlow = combine(
                programDao.getActiveProgramsFlow(now),
                programDao.getAllUpcomingProgramsFlow(now)
            ) { active, upcoming ->
                EpgInfo(active, upcoming)
            }

            combine(
                channelInfoFlow,
                loadingInfoFlow,
                epgInfoFlow,
                _selectedGroup,
                playerEngine.playbackState
            ) { channelInfo, loadingInfo, epgInfo, selectedGroup, playbackState ->
                val searchQuery = channelInfo.searchQuery
                val favoriteUrls = channelInfo.favoriteUrls
                val channels = channelInfo.channels

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

                TvUiState(
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

data class TvUiState(
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
    val epgData: Map<String, List<ProgramEntity>> = emptyMap()
)
