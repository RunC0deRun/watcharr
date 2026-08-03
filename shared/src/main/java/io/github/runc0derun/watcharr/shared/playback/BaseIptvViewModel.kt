package io.github.runc0derun.watcharr.shared.playback

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.runc0derun.watcharr.shared.data.db.AppDatabase
import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.data.db.FavoriteEntity
import io.github.runc0derun.watcharr.shared.data.db.ProgramEntity
import io.github.runc0derun.watcharr.shared.data.epg.EpgFetcher
import io.github.runc0derun.watcharr.shared.data.epg.EpgMatcher
import io.github.runc0derun.watcharr.shared.data.epg.EpgSyncWorker
import io.github.runc0derun.watcharr.shared.data.parser.M3uParser
import io.github.runc0derun.watcharr.shared.mvi.PlaybackIntent
import io.github.runc0derun.watcharr.shared.mvi.PlaybackSideEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    protected val _dispatcharrUsername = MutableStateFlow("")
    protected val _dispatcharrPassword = MutableStateFlow("")
    protected val _isTailnetEnabled = MutableStateFlow(false)
    protected val _tailscaleAuthKey = MutableStateFlow("")
    protected val _tsnetStatus = MutableStateFlow("")

    protected val dvrRecordingDao = database.dvrRecordingDao()
    protected val networkStorageManager = io.github.runc0derun.watcharr.shared.data.dvr.NetworkStorageManager(application)
    protected val dvrAlarmScheduler = io.github.runc0derun.watcharr.shared.data.dvr.DvrAlarmScheduler(application)

    protected val _dvrRecordingMode = MutableStateFlow("WATCHARR")
    protected val _dvrStorageType = MutableStateFlow("ON_DEVICE")
    protected val _nfsSmbProtocol = MutableStateFlow("SMB")
    protected val _nfsSmbHost = MutableStateFlow("")
    protected val _nfsSmbSharePath = MutableStateFlow("")
    protected val _nfsSmbUser = MutableStateFlow("")
    protected val _nfsSmbPass = MutableStateFlow("")

    protected val dvrClient = io.github.runc0derun.watcharr.shared.data.dvr.DispatcharrDvrClient()
    protected val _recordings = MutableStateFlow<List<io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording>>(emptyList())
    protected val _isDvrLoading = MutableStateFlow(false)
    protected val _scheduledProgramKeys = MutableStateFlow<Set<String>>(emptySet())

    protected data class DvrInfo(
        val recordings: List<io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording>,
        val isDvrLoading: Boolean,
        val scheduledProgramKeys: Set<String>
    )

    protected val dvrInfoFlow = combine(
        _recordings,
        _isDvrLoading,
        _scheduledProgramKeys
    ) { recordings, isLoading, scheduledKeys ->
        DvrInfo(recordings, isLoading, scheduledKeys)
    }

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
        _dispatcharrUsername.value = prefs.getString("dispatcharr_username", "") ?: ""
        _dispatcharrPassword.value = prefs.getString("dispatcharr_password", "") ?: ""

        _dvrRecordingMode.value = prefs.getString("dvr_recording_mode", "WATCHARR") ?: "WATCHARR"
        _dvrStorageType.value = prefs.getString("dvr_storage_type", "ON_DEVICE") ?: "ON_DEVICE"
        _nfsSmbProtocol.value = prefs.getString("nfs_smb_protocol", "SMB") ?: "SMB"
        _nfsSmbHost.value = prefs.getString("nfs_smb_host", "") ?: ""
        _nfsSmbSharePath.value = prefs.getString("nfs_smb_share_path", "") ?: ""
        _nfsSmbUser.value = prefs.getString("nfs_smb_user", "") ?: ""
        _nfsSmbPass.value = prefs.getString("nfs_smb_pass", "") ?: ""

        val tailnetEnabled = prefs.getBoolean("tailnet_enabled", false)
        val tailscaleAuthKey = prefs.getString("tailscale_auth_key", "") ?: ""
        _isTailnetEnabled.value = tailnetEnabled
        _tailscaleAuthKey.value = tailscaleAuthKey

        viewModelScope.launch {
            TsnetManager.status.collect { status ->
                _tsnetStatus.value = status
            }
        }

        viewModelScope.launch {
            dvrRecordingDao.getAllRecordingsFlow().collect { localEntities ->
                fetchRecordings(localEntities)
            }
        }

        if (tailnetEnabled && tailscaleAuthKey.isNotEmpty()) {
            TsnetManager.start(getApplication(), tailscaleAuthKey)
        }

        if (onboardingDone) {
            checkAndSyncEpgOnStartup()
            fetchRecordings()
        }
        verifyDrmChannelsOnStartup()

        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                if (_useDispatcharr.value && getAuthenticatedDispatcharrUrl().isNotEmpty()) {
                    fetchRecordings()
                }
            }
        }
    }

    fun setTailnetEnabled(enabled: Boolean) {
        _isTailnetEnabled.value = enabled
        prefs.edit().putBoolean("tailnet_enabled", enabled).apply()
        if (enabled) {
            val key = _tailscaleAuthKey.value
            if (key.isNotEmpty()) {
                TsnetManager.start(getApplication(), key)
            }
        } else {
            TsnetManager.stop()
        }
    }

    fun setTailscaleAuthKey(authKey: String) {
        _tailscaleAuthKey.value = authKey
        prefs.edit().putString("tailscale_auth_key", authKey).apply()
    }

    fun connectTailscale(authKey: String) {
        _tailscaleAuthKey.value = authKey
        prefs.edit().putString("tailscale_auth_key", authKey).apply()
        setTailnetEnabled(true)
    }

    private fun checkAndSyncEpgOnStartup() {
        val epgUrl = _epgUrlInput.value
        if (epgUrl.isEmpty()) return

        // 1. Ensure the periodic WorkManager job is scheduled.
        // This is safe because EpgSyncWorker.schedule uses UPDATE policy.
        EpgSyncWorker.schedule(getApplication(), epgUrl)

        // 2. Check if the last sync was more than 12 hours ago.
        val lastSync = prefs.getLong("last_epg_sync_time", 0L)
        val now = System.currentTimeMillis()
        if (now - lastSync > 12 * 60 * 60 * 1000L) {
            viewModelScope.launch {
                if (TsnetManager.isEnabled()) {
                    val connected = TsnetManager.awaitConnection()
                    if (!connected) return@launch
                }
                try {
                    withContext(Dispatchers.IO) {
                        val fetcher = EpgFetcher(getApplication())
                        fetcher.fetchAndSyncEpg(epgUrl)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    protected fun getAuthenticatedDispatcharrUrl(): String {
        val raw = _dispatcharrUrl.value.trim().removeSuffix("/")
        if (raw.isEmpty()) return ""
        val user = _dispatcharrUsername.value.trim()
        val pass = _dispatcharrPassword.value.trim()
        if (user.isNotEmpty() && !raw.contains("@")) {
            val scheme = if (raw.startsWith("https://")) "https://" else "http://"
            val cleanHost = raw.removePrefix("http://").removePrefix("https://")
            val encodedUser = try { java.net.URLEncoder.encode(user, "UTF-8") } catch (e: Exception) { user }
            val encodedPass = try { java.net.URLEncoder.encode(pass, "UTF-8") } catch (e: Exception) { pass }
            return "$scheme$encodedUser:$encodedPass@$cleanHost"
        }
        return raw
    }

    protected fun sanitizeUrl(url: String): String {
        return url.replace(Regex("https?://[^@]+@"), if (url.startsWith("https://")) "https://" else "http://")
    }

    fun setDvrRecordingMode(mode: String) {
        val safeMode = if (mode.uppercase() == "DISPATCHARR") "DISPATCHARR" else "WATCHARR"
        _dvrRecordingMode.value = safeMode
        prefs.edit().putString("dvr_recording_mode", safeMode).apply()
    }

    fun setDvrStorageConfig(
        storageType: String,
        protocol: String = "SMB",
        host: String = "",
        sharePath: String = "",
        user: String = "",
        pass: String = ""
    ) {
        val type = if (storageType.uppercase() == "NETWORK_SHARE") "NETWORK_SHARE" else "ON_DEVICE"
        val proto = if (protocol.uppercase() == "NFS") "NFS" else "SMB"
        _dvrStorageType.value = type
        _nfsSmbProtocol.value = proto
        _nfsSmbHost.value = host.trim()
        _nfsSmbSharePath.value = sharePath.trim()
        _nfsSmbUser.value = user.trim()
        _nfsSmbPass.value = pass.trim()

        prefs.edit().apply {
            putString("dvr_storage_type", type)
            putString("nfs_smb_protocol", proto)
            putString("nfs_smb_host", host.trim())
            putString("nfs_smb_share_path", sharePath.trim())
            putString("nfs_smb_user", user.trim())
            putString("nfs_smb_pass", pass.trim())
            apply()
        }
    }

    suspend fun testNetworkStorageReachability(
        protocol: String,
        host: String,
        sharePath: String,
        user: String,
        pass: String
    ): io.github.runc0derun.watcharr.shared.data.dvr.NetworkStorageManager.TestResult {
        val config = io.github.runc0derun.watcharr.shared.data.dvr.NetworkStorageManager.NetworkShareConfig(
            protocol = protocol,
            host = host,
            sharePath = sharePath,
            username = user,
            password = pass
        )
        return networkStorageManager.testConnection(config)
    }

    fun scheduleRecording(program: ProgramEntity, channel: ChannelEntity? = null) {
        val isWatcharrMode = _dvrRecordingMode.value == "WATCHARR" || !_useDispatcharr.value
        if (isWatcharrMode) {
            val targetChannel = channel ?: return
            val recId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val isLiveNow = now >= program.start && now < program.stop
            val initialStatus = if (isLiveNow) "RECORDING" else "SCHEDULED"

            val entity = io.github.runc0derun.watcharr.shared.data.db.DvrRecordingEntity(
                id = recId,
                programTitle = program.title,
                channelName = targetChannel.name,
                channelId = targetChannel.tvgId ?: targetChannel.url,
                startEpochMs = program.start,
                stopEpochMs = program.stop,
                status = initialStatus,
                streamUrl = targetChannel.url,
                posterUrl = program.iconUrl,
                description = program.desc,
                recordingEngine = "WATCHARR",
                storageType = _dvrStorageType.value
            )
            viewModelScope.launch {
                dvrRecordingDao.insertRecording(entity)
                if (isLiveNow) {
                    val serviceIntent = android.content.Intent(getApplication(), io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService::class.java).apply {
                        action = io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.ACTION_START_RECORDING
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_RECORDING_ID, entity.id)
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_PROGRAM_TITLE, entity.programTitle)
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_CHANNEL_NAME, entity.channelName)
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_CHANNEL_ID, entity.channelId)
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_STREAM_URL, entity.streamUrl)
                        putExtra(io.github.runc0derun.watcharr.shared.data.dvr.WatcharrRecordingService.EXTRA_STOP_EPOCH_MS, entity.stopEpochMs)
                    }
                    androidx.core.content.ContextCompat.startForegroundService(getApplication(), serviceIntent)
                    _sideEffects.emit(PlaybackSideEffect.ShowToast("Started active recording for '${program.title}'"))
                } else {
                    dvrAlarmScheduler.scheduleRecordingAlarm(entity)
                    _sideEffects.emit(PlaybackSideEffect.ShowToast("Scheduled Watcharr recording for '${program.title}'"))
                }
            }
            return
        }

        val dispatcharrUrl = getAuthenticatedDispatcharrUrl()
        if (dispatcharrUrl.isEmpty()) {
            viewModelScope.launch {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Dispatcharr server is not configured."))
            }
            return
        }
        viewModelScope.launch {
            _isDvrLoading.value = true
            val result = dvrClient.scheduleRecording(dispatcharrUrl, program, channel)
            _isDvrLoading.value = false
            result.onSuccess { rec ->
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Scheduled recording for '${program.title}'"))
                fetchRecordings()
            }.onFailure { err ->
                val safeMsg = sanitizeUrl(err.message ?: "Unknown error")
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Failed to schedule recording: $safeMsg"))
            }
        }
    }

    fun cancelRecording(recordingId: String) {
        viewModelScope.launch {
            val localRec = dvrRecordingDao.getRecordingById(recordingId)
            if (localRec != null) {
                dvrAlarmScheduler.cancelRecordingAlarm(recordingId)
                dvrRecordingDao.deleteRecording(recordingId)
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Watcharr recording cancelled."))
                return@launch
            }

            val dispatcharrUrl = getAuthenticatedDispatcharrUrl()
            if (!_useDispatcharr.value || dispatcharrUrl.isEmpty()) return@launch
            _isDvrLoading.value = true
            val result = dvrClient.deleteRecording(dispatcharrUrl, recordingId)
            _isDvrLoading.value = false
            result.onSuccess {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Recording cancelled/deleted."))
                fetchRecordings()
            }.onFailure { err ->
                val safeMsg = sanitizeUrl(err.message ?: "Unknown error")
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Failed to delete recording: $safeMsg"))
            }
        }
    }

    fun fetchRecordings(localEntities: List<io.github.runc0derun.watcharr.shared.data.db.DvrRecordingEntity>? = null) {
        viewModelScope.launch {
            val locals = localEntities ?: dvrRecordingDao.getAllRecordings()
            val localRecordings = locals.map { entity ->
                val status = io.github.runc0derun.watcharr.shared.data.dvr.DvrStatus.fromString(entity.status)
                io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording(
                    id = entity.id,
                    programTitle = entity.programTitle,
                    channelName = entity.channelName,
                    channelId = entity.channelId,
                    startEpochMs = entity.startEpochMs,
                    stopEpochMs = entity.stopEpochMs,
                    status = status,
                    streamUrl = entity.localFilePath ?: entity.streamUrl,
                    posterUrl = entity.posterUrl,
                    description = entity.description,
                    recordingEngine = entity.recordingEngine,
                    storageType = entity.storageType,
                    localFilePath = entity.localFilePath,
                    isDrmDecrypted = entity.isDrmDecrypted,
                    errorReason = entity.errorReason
                )
            }

            val dispatcharrUrl = getAuthenticatedDispatcharrUrl()
            if (!_useDispatcharr.value || dispatcharrUrl.isEmpty()) {
                updateCombinedRecordings(localRecordings, emptyList())
                return@launch
            }

            _isDvrLoading.value = true
            val result = dvrClient.fetchRecordings(dispatcharrUrl)
            _isDvrLoading.value = false
            result.onSuccess { list ->
                val now = System.currentTimeMillis()
                val evaluatedList = list.map { rec ->
                    if (rec.status != io.github.runc0derun.watcharr.shared.data.dvr.DvrStatus.COMPLETED &&
                        rec.status != io.github.runc0derun.watcharr.shared.data.dvr.DvrStatus.FAILED &&
                        rec.stopEpochMs > 0L && now >= rec.stopEpochMs
                    ) {
                        rec.copy(status = io.github.runc0derun.watcharr.shared.data.dvr.DvrStatus.COMPLETED)
                    } else {
                        rec
                    }
                }
                updateCombinedRecordings(localRecordings, evaluatedList)
            }.onFailure {
                updateCombinedRecordings(localRecordings, emptyList())
            }
        }
    }

    private fun updateCombinedRecordings(
        localRecordings: List<io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording>,
        dispatcharrRecordings: List<io.github.runc0derun.watcharr.shared.data.dvr.DvrRecording>
    ) {
        val combined = (localRecordings + dispatcharrRecordings).distinctBy { it.id }
        _recordings.value = combined
        val keys = combined.flatMap { rec ->
            val set = mutableSetOf<String>()
            if (!rec.channelId.isNullOrEmpty()) set.add("${rec.channelId}_${rec.startEpochMs}")
            set.add(rec.programTitle.lowercase())
            set
        }.toSet()
        _scheduledProgramKeys.value = keys
    }

    fun completeOnboarding(
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
        viewModelScope.launch {
            val trimmedPlaylist = playlistUrl.trim().removeSuffix("/")
            val trimmedEpg = epgUrl.trim().removeSuffix("/")
            val rawDispatcharr = dispatcharrUrl?.trim()?.removeSuffix("/") ?: ""
            val user = dispatcharrUsername.trim()
            val pass = dispatcharrPassword.trim()

            var finalDispatcharr = rawDispatcharr
            if (useDispatcharr && user.isNotEmpty() && rawDispatcharr.isNotEmpty()) {
                val scheme = if (rawDispatcharr.startsWith("https://")) "https://" else "http://"
                val cleanHost = rawDispatcharr.removePrefix("http://").removePrefix("https://")
                if (!cleanHost.contains("@")) {
                    val encodedUser = java.net.URLEncoder.encode(user, "UTF-8")
                    val encodedPass = java.net.URLEncoder.encode(pass, "UTF-8")
                    finalDispatcharr = "$scheme$encodedUser:$encodedPass@$cleanHost"
                }
            }

            if (trimmedPlaylist.isNotEmpty() && !isValidUrl(trimmedPlaylist)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid Playlist URL format!"))
                return@launch
            }
            if (trimmedEpg.isNotEmpty() && !isValidUrl(trimmedEpg)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid EPG URL format!"))
                return@launch
            }
            if (useDispatcharr && finalDispatcharr.isNotEmpty() && !isValidUrl(finalDispatcharr)) {
                _sideEffects.emit(PlaybackSideEffect.ShowToast("Invalid Dispatcharr URL format!"))
                return@launch
            }

            val safeRecordingMode = if (dvrRecordingMode.uppercase() == "DISPATCHARR") "DISPATCHARR" else "WATCHARR"
            val safeStorageType = if (dvrStorageType.uppercase() == "NETWORK_SHARE") "NETWORK_SHARE" else "ON_DEVICE"
            val safeProtocol = if (nfsSmbProtocol.uppercase() == "NFS") "NFS" else "SMB"

            prefs.edit().apply {
                putString("playlist_url", trimmedPlaylist)
                putString("epg_url", trimmedEpg)
                putString("dispatcharr_url", finalDispatcharr)
                putString("dispatcharr_username", user)
                putString("dispatcharr_password", pass)
                putBoolean("use_dispatcharr", useDispatcharr)
                putString("dvr_recording_mode", safeRecordingMode)
                putString("dvr_storage_type", safeStorageType)
                putString("nfs_smb_protocol", safeProtocol)
                putString("nfs_smb_host", nfsSmbHost.trim())
                putString("nfs_smb_share_path", nfsSmbSharePath.trim())
                putString("nfs_smb_user", nfsSmbUser.trim())
                putString("nfs_smb_pass", nfsSmbPass.trim())
                putBoolean("onboarding_completed", true)
                apply()
            }

            _playlistUrlInput.value = trimmedPlaylist
            _epgUrlInput.value = trimmedEpg
            _useDispatcharr.value = useDispatcharr
            _dispatcharrUrl.value = finalDispatcharr
            _dispatcharrUsername.value = user
            _dispatcharrPassword.value = pass
            _dvrRecordingMode.value = safeRecordingMode
            _dvrStorageType.value = safeStorageType
            _nfsSmbProtocol.value = safeProtocol
            _nfsSmbHost.value = nfsSmbHost.trim()
            _nfsSmbSharePath.value = nfsSmbSharePath.trim()
            _nfsSmbUser.value = nfsSmbUser.trim()
            _nfsSmbPass.value = nfsSmbPass.trim()
            _isOnboardingCompleted.value = true

            if (trimmedPlaylist.isNotEmpty()) {
                handleIntent(PlaybackIntent.LoadPlaylist(trimmedPlaylist))
            }
            if (trimmedEpg.isNotEmpty()) {
                loadEpg(trimmedEpg)
            }
            if (useDispatcharr && finalDispatcharr.isNotEmpty()) {
                fetchRecordings()
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
            is PlaybackIntent.FetchRecordings -> {
                fetchRecordings()
            }
            is PlaybackIntent.ScheduleRecording -> {
                scheduleRecording(intent.program, intent.channel)
            }
            is PlaybackIntent.CancelRecording -> {
                cancelRecording(intent.recordingId)
            }
            is PlaybackIntent.PlayRecording -> {
                val dispatcharrUrl = getAuthenticatedDispatcharrUrl()
                val authUrl = dvrClient.getAuthenticatedRecordingStreamUrl(dispatcharrUrl, intent.recording.streamUrl)
                val pseudoChannel = ChannelEntity(
                    url = authUrl,
                    name = intent.recording.programTitle,
                    tvgId = intent.recording.channelId,
                    tvgName = null,
                    logoUrl = intent.recording.posterUrl,
                    groupTitle = "Recordings"
                )
                onSelectChannel(pseudoChannel)
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
            
            if (TsnetManager.isEnabled()) {
                val connected = TsnetManager.awaitConnection()
                if (!connected) {
                    _isLoadingPlaylist.value = false
                    _sideEffects.emit(PlaybackSideEffect.ShowToast("Tailscale connection failed: ${TsnetManager.status.value}"))
                    return@launch
                }
            }

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
                verifyDrmChannelsOnStartup()
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

    protected suspend fun probeChannelDrm(channel: ChannelEntity): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val urlsToProbe = mutableListOf<String>()
        urlsToProbe.add(channel.url)

        try {
            val uri = java.net.URI(channel.url)
            val scheme = uri.scheme ?: "http"
            val authority = uri.authority
            val pathSegments = uri.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
            val channelId = channel.tvgId?.trim()?.ifEmpty { null }
                ?: (if (pathSegments.isNotEmpty()) pathSegments.last() else null)

            if (authority != null && channelId != null) {
                val liveUrl = "$scheme://$authority/live/$channelId"
                if (!urlsToProbe.contains(liveUrl)) {
                    urlsToProbe.add(liveUrl)
                }
            }
        } catch (e: Exception) {
            // ignore URI parsing errors
        }

        for (targetUrl in urlsToProbe) {
            if (!targetUrl.startsWith("http", ignoreCase = true)) continue
            try {
                val url = java.net.URL(targetUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Watcharr/1.0")
                conn.setRequestProperty("Accept", "*/*")

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val buffer = ByteArray(16384)
                    val bytesRead = conn.inputStream.use { it.read(buffer, 0, buffer.size) }
                    if (bytesRead > 0) {
                        val content = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        if (content.contains("<ContentProtection", ignoreCase = true) ||
                            content.contains("ContentProtection", ignoreCase = true) ||
                            content.contains("widevine", ignoreCase = true) ||
                            content.contains("license", ignoreCase = true)
                        ) {
                            conn.disconnect()
                            return@withContext true
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                // ignore network probe errors for individual endpoints
            }
        }
        return@withContext false
    }

    protected fun verifyDrmChannelsOnStartup() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val channels = channelDao.getAllChannels()
                for (channel in channels) {
                    if (channel.isDrm) continue
                    val isDrm = probeChannelDrm(channel)
                    if (isDrm) {
                        channelDao.updateDrmStatus(channel.url, true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadEpg(epgUrl: String) {
        viewModelScope.launch {
            _isLoadingEpg.value = true
            
            if (TsnetManager.isEnabled()) {
                val connected = TsnetManager.awaitConnection()
                if (!connected) {
                    _isLoadingEpg.value = false
                    _sideEffects.emit(PlaybackSideEffect.ShowToast("Tailscale connection failed: ${TsnetManager.status.value}"))
                    return@launch
                }
            }

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
        TsnetManager.stop()
    }
}
