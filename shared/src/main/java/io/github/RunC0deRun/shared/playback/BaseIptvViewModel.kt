package io.github.RunC0deRun.shared.playback

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.RunC0deRun.shared.data.db.AppDatabase
import io.github.RunC0deRun.shared.data.db.ChannelEntity
import io.github.RunC0deRun.shared.data.db.FavoriteEntity
import io.github.RunC0deRun.shared.data.db.ProgramEntity
import io.github.RunC0deRun.shared.data.epg.EpgFetcher
import io.github.RunC0deRun.shared.data.epg.EpgMatcher
import io.github.RunC0deRun.shared.data.epg.EpgSyncWorker
import io.github.RunC0deRun.shared.data.parser.M3uParser
import io.github.RunC0deRun.shared.mvi.PlaybackIntent
import io.github.RunC0deRun.shared.mvi.PlaybackSideEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("PropertyName")
open class BaseIptvViewModel(application: Application) : AndroidViewModel(application) {

    protected val database = AppDatabase.getDatabase(application)
    protected val channelDao = database.channelDao()
    protected val programDao = database.programDao()
    protected val favoriteDao = database.favoriteDao()

    val playerEngine = PlayerEngineProvider.get(application)

    protected val _isLoadingPlaylist = MutableStateFlow(false)
    protected val _playlistUrlInput = MutableStateFlow("")
    protected val _selectedGroup = MutableStateFlow<String?>(null)

    protected val _isLoadingEpg = MutableStateFlow(false)
    protected val _epgUrlInput = MutableStateFlow("")

    protected val _searchQuery = MutableStateFlow("")

    protected val _sideEffects = MutableSharedFlow<PlaybackSideEffect>()
    val sideEffects: SharedFlow<PlaybackSideEffect> = _sideEffects.asSharedFlow()

    protected val _isOnboardingCompleted = MutableStateFlow(false)
    protected val _useDispatcharr = MutableStateFlow(false)
    protected val _dispatcharrUrl = MutableStateFlow("")

    protected val prefs: SharedPreferences = application.getSharedPreferences("watcharr_prefs", Context.MODE_PRIVATE)

    protected data class ChannelInfo(
        val channels: List<ChannelEntity>,
        val groups: List<String>,
        val searchQuery: String,
        val favoriteUrls: Set<String>
    )

    protected data class LoadingInfo(
        val isLoadingPlaylist: Boolean,
        val playlistUrlInput: String,
        val isLoadingEpg: Boolean,
        val epgUrlInput: String
    )

    protected data class EpgInfo(
        val activePrograms: List<ProgramEntity>,
        val upcomingPrograms: List<ProgramEntity>
    )

    protected val channelInfoFlow = combine(
        channelDao.getAllChannelsFlow(),
        channelDao.getUniqueGroupsFlow(),
        _searchQuery,
        favoriteDao.getFavoriteUrlsFlow()
    ) { channels, groups, search, favorites ->
        ChannelInfo(channels, groups, search, favorites.toSet())
    }

    protected val loadingInfoFlow = combine(
        _isLoadingPlaylist,
        _playlistUrlInput,
        _isLoadingEpg,
        _epgUrlInput
    ) { isLoadPlaylist, playlistUrl, isLoadEpg, epgUrl ->
        LoadingInfo(isLoadPlaylist, playlistUrl, isLoadEpg, epgUrl)
    }

    private val nowFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30.seconds)
        }
    }

    protected val epgInfoFlow = nowFlow.flatMapLatest { now ->
        combine(
            programDao.getActiveProgramsFlow(now),
            programDao.getAllUpcomingProgramsFlow(now)
        ) { active, upcoming ->
            EpgInfo(active, upcoming)
        }
    }

    protected fun initPreferences() {
        val onboardingDone = prefs.getBoolean("onboarding_completed", false)
        _isOnboardingCompleted.value = onboardingDone
        _playlistUrlInput.value = prefs.getString("playlist_url", "") ?: ""
        _epgUrlInput.value = prefs.getString("epg_url", "") ?: ""
        _useDispatcharr.value = prefs.getBoolean("use_dispatcharr", false)
        _dispatcharrUrl.value = prefs.getString("dispatcharr_url", "") ?: ""
    }

    fun completeOnboarding(playlistUrl: String, epgUrl: String, dispatcharrUrl: String?, useDispatcharr: Boolean) {
        viewModelScope.launch {
            val trimmedPlaylist = playlistUrl.trim().removeSuffix("/")
            val trimmedEpg = epgUrl.trim().removeSuffix("/")
            val trimmedDispatcharr = dispatcharrUrl?.trim()?.removeSuffix("/") ?: ""

            if (trimmedPlaylist.isNotEmpty() && !isValidUrl(trimmedPlaylist)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid Playlist URL format!"))
                return@launch
            }
            if (trimmedEpg.isNotEmpty() && !isValidUrl(trimmedEpg)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid EPG URL format!"))
                return@launch
            }
            if (useDispatcharr && trimmedDispatcharr.isNotEmpty() && !isValidUrl(trimmedDispatcharr)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid Dispatcharr URL format!"))
                return@launch
            }

            prefs.edit().apply {
                putString("playlist_url", trimmedPlaylist)
                putString("epg_url", trimmedEpg)
                putString("dispatcharr_url", trimmedDispatcharr)
                putBoolean("use_dispatcharr", useDispatcharr)
                putBoolean("onboarding_completed", true)
                apply()
            }

            _playlistUrlInput.value = trimmedPlaylist
            _epgUrlInput.value = trimmedEpg
            _useDispatcharr.value = useDispatcharr
            _dispatcharrUrl.value = trimmedDispatcharr
            _isOnboardingCompleted.value = true

            if (trimmedPlaylist.isNotEmpty()) {
                handleIntent(PlaybackIntent.LoadPlaylist(trimmedPlaylist))
            }
            if (trimmedEpg.isNotEmpty()) {
                loadEpg(trimmedEpg)
            }
        }
    }

    fun handleIntent(intent: PlaybackIntent) {
        when (intent) {
            is PlaybackIntent.LoadPlaylist -> {
                loadPlaylist(intent.m3uUrl)
            }
            is PlaybackIntent.SelectChannel -> {
                onSelectChannel(intent.channel)
            }
            is PlaybackIntent.TogglePlay -> {
                playerEngine.togglePlay()
            }
        }
    }

    open fun onSelectChannel(channel: ChannelEntity) {
        // Implemented by subclasses
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
                if (uiStateFavUrls().contains(channelUrl)) {
                    favoriteDao.delete(channelUrl)
                } else {
                    favoriteDao.insert(FavoriteEntity(channelUrl))
                }
            }
        }
    }

    open fun uiStateFavUrls(): Set<String> {
        return emptySet()
    }

    protected fun loadPlaylist(m3uUrl: String) {
        viewModelScope.launch {
            _isLoadingPlaylist.value = true
            val context = getApplication<Application>()
            val tempFile = java.io.File.createTempFile("playlist", ".m3u", context.cacheDir)
            try {
                withContext(Dispatchers.IO) {
                    val url = URL(m3uUrl)
                    val conn = url.openConnection()
                    conn.connectTimeout = 30000
                    conn.readTimeout = 30000
                    conn.getInputStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    (conn as? java.net.HttpURLConnection)?.disconnect()

                    java.io.FileInputStream(tempFile).use { fileInputStream ->
                        channelDao.deleteAll()
                        val batch = mutableListOf<ChannelEntity>()
                        val batchSize = 1000
                        M3uParser.parse(fileInputStream).collect { channel ->
                            batch.add(channel)
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
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                _isLoadingPlaylist.value = false
            }
        }
    }

    fun loadEpg(epgUrl: String) {
        viewModelScope.launch {
            _isLoadingEpg.value = true
            try {
                withContext(Dispatchers.IO) {
                    val fetcher = EpgFetcher(getApplication())
                    val result = fetcher.fetchAndSyncEpg(epgUrl)
                    if (result.isFailure) throw result.exceptionOrNull()!!
                }
                EpgSyncWorker.schedule(getApplication(), epgUrl)
                _sideEffects.emit(PlaybackSideEffect.ShowToast("EPG guide loaded & daily sync scheduled successfully"))
            } catch (e: Exception) {
                e.printStackTrace()
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Failed to load EPG: ${e.localizedMessage}"))
            } finally {
                _isLoadingEpg.value = false
            }
        }
    }

    protected fun buildEpgData(
        filteredChannels: List<ChannelEntity>,
        epgInfo: EpgInfo
    ): Map<String, List<ProgramEntity>> {
        val activeMapByTvg = epgInfo.activePrograms.associateBy { it.channelId.lowercase(java.util.Locale.US) }
        val activeMapByNorm = epgInfo.activePrograms.associateBy { EpgMatcher.normalize(it.channelId) }

        val upcomingGroupByTvg = epgInfo.upcomingPrograms.groupBy { it.channelId.lowercase(java.util.Locale.US) }
        val upcomingGroupByNorm = epgInfo.upcomingPrograms.groupBy { EpgMatcher.normalize(it.channelId) }

        return filteredChannels.associate { channel ->
            val normName = EpgMatcher.normalize(channel.name)
            val tvgIdLower = channel.tvgId?.lowercase(java.util.Locale.US)

            val current = (if (!tvgIdLower.isNullOrEmpty()) activeMapByTvg[tvgIdLower] else null)
                ?: activeMapByNorm[normName]

            val upcoming = (if (!tvgIdLower.isNullOrEmpty()) upcomingGroupByTvg[tvgIdLower] else null)
                ?: upcomingGroupByNorm[normName]
                ?: emptyList()

            val programs = mutableListOf<ProgramEntity>()
            if (current != null) {
                programs.add(current)
            }
            programs.addAll(upcoming)
            channel.url to programs
        }
    }

    private fun isValidUrl(url: String): Boolean {
        if (url.isEmpty()) return true
        return try {
            val parsed = URL(url)
            parsed.protocol == "http" || parsed.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        playerEngine.release()
    }
}
