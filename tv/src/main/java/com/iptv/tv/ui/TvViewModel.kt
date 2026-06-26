package com.iptv.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.tv.TvApp
import com.iptv.shared.data.db.ChannelEntity
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
    
    val playerEngine = PlayerEngine(application)

    private val _isLoadingPlaylist = MutableStateFlow(false)
    private val _playlistUrlInput = MutableStateFlow("")
    private val _selectedGroup = MutableStateFlow<String?>(null)

    private val _sideEffects = MutableSharedFlow<PlaybackSideEffect>()
    val sideEffects: SharedFlow<PlaybackSideEffect> = _sideEffects.asSharedFlow()

    private val _uiState = MutableStateFlow(TvUiState())
    val uiState: StateFlow<TvUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                combine(channelDao.getAllChannelsFlow(), channelDao.getUniqueGroupsFlow(), ::Pair),
                combine(_selectedGroup, playerEngine.playbackState, ::Pair),
                combine(_isLoadingPlaylist, _playlistUrlInput, ::Pair)
            ) { (channels, groups), (selectedGroup, playbackState), (isLoadingPlaylist, urlInput) ->
                val filteredChannels = if (selectedGroup == null) {
                    channels
                } else {
                    channels.filter { it.groupTitle == selectedGroup }
                }
                TvUiState(
                    channels = filteredChannels,
                    groups = groups,
                    selectedGroup = selectedGroup,
                    playbackState = playbackState,
                    isLoadingPlaylist = isLoadingPlaylist,
                    playlistUrlInput = urlInput
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
    val playlistUrlInput: String = ""
)
