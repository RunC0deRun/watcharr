package com.iptv.mobile.ui

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.mvi.*
import com.iptv.shared.playback.BaseIptvViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MobileViewModel(application: Application) : BaseIptvViewModel(application) {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    init {
        initPreferences()

        viewModelScope.launch {
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
                    useDispatcharr = useDispatcharr,
                    dispatcharrUrl = dispatcharrUrl
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

    override fun _uiStateFavUrls(): Set<String> {
        return _uiState.value.favoriteUrls
    }

    fun sendConfigToTv(tvSetupUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val m3u = prefs.getString("playlist_url", "") ?: ""
                val epg = prefs.getString("epg_url", "") ?: ""

                if (m3u.isEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onError("Mobile app is not configured yet!")
                    }
                    return@launch
                }

                val json = org.json.JSONObject()
                json.put("playlistUrl", m3u)
                json.put("epgUrl", epg)
                val jsonBytes = json.toString().toByteArray()

                val url = java.net.URL(tvSetupUrl)
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
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onError("TV server returned error code: $code")
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Failed to connect to TV: ${e.localizedMessage}")
                }
            }
        }
    }

    private data class SettingsInfo(
        val selectedGroup: String?,
        val isOnboardingCompleted: Boolean,
        val useDispatcharr: Boolean,
        val dispatcharrUrl: String
    )
}
