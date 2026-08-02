package io.github.runc0derun.watcharr.shared.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import io.github.runc0derun.watcharr.shared.data.db.ChannelEntity
import io.github.runc0derun.watcharr.shared.mvi.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import androidx.core.net.toUri
import android.net.Uri

@OptIn(UnstableApi::class)
class PlayerEngine(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val job = kotlinx.coroutines.SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var retryJob: Job? = null
    private var retryAttempt = 0
    internal var currentChannel: ChannelEntity? = null
    private var activeChannelList: List<ChannelEntity> = emptyList()
    private var isVideoRestrictedState = false

    fun getPlayer(): ExoPlayer {
        return exoPlayer ?: createPlayer().also { exoPlayer = it }
    }

    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1500, // minBufferMs
                5000, // maxBufferMs
                500,  // bufferForPlaybackMs
                1000  // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val mediaCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            var decoderInfos = MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            if (requiresSecureDecoder) {
                val nonSecureDecoders = MediaCodecSelector.DEFAULT.getDecoderInfos(
                    mimeType,
                    /* requiresSecureDecoder = */ false,
                    requiresTunnelingDecoder
                )
                val combined = ArrayList<MediaCodecInfo>(decoderInfos.size + nonSecureDecoders.size)
                combined.addAll(decoderInfos)
                combined.addAll(nonSecureDecoders)
                decoderInfos = combined
            }
            decoderInfos
        }

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(mediaCodecSelector)

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        val extractorsFactory = DefaultExtractorsFactory()
            .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_TFDT_BOX)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when(state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($state)"
                }
                android.util.Log.d("Watcharr", "onPlaybackStateChanged: $stateName, channel=${currentChannel?.name}, isVideoRestricted=$isVideoRestrictedState")
                when (state) {
                    Player.STATE_IDLE -> {
                        // Do not overwrite Error state if it was set
                        if (_playbackState.value !is PlaybackState.Error) {
                            _playbackState.value = PlaybackState.Idle
                        }
                    }
                    Player.STATE_BUFFERING -> {
                        _playbackState.value = PlaybackState.Loading
                    }
                    Player.STATE_READY -> {
                        retryAttempt = 0
                        currentChannel?.let {
                            _playbackState.value = PlaybackState.Playing(it, isVideoRestrictedState)
                        }
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.value = PlaybackState.Idle
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("Watcharr", "ExoPlayer playback error: ", error)
                val errorMessage = error.localizedMessage ?: "Playback Error"
                _playbackState.value = PlaybackState.Error(errorMessage)
                scheduleRetry()
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                android.util.Log.d("Watcharr", "onTracksChanged: ${tracks.groups.size} track groups")
                for ((i, group) in tracks.groups.withIndex()) {
                    val format = group.getTrackFormat(0)
                    android.util.Log.d("Watcharr", "  Track group $i: type=${format.sampleMimeType}, codecs=${format.codecs}, drmInitData=${format.drmInitData != null}")
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                android.util.Log.d("Watcharr", "onMediaItemTransition: uri=${mediaItem?.localConfiguration?.uri}, drm=${mediaItem?.localConfiguration?.drmConfiguration != null}, mimeType=${mediaItem?.localConfiguration?.mimeType}")
            }
        })

        return player
    }

    fun play(channel: ChannelEntity) {
        retryJob?.cancel()
        currentChannel = channel
        
        _playbackState.value = PlaybackState.Loading

        scope.launch {
            // Check for a pipe-separated dual-file DASH recording (video|audio)
            val isDualFile = (channel.url.startsWith("/") || channel.url.startsWith("file://")) &&
                    channel.url.contains("|")

            if (isDualFile) {
                playDualFileRecording(channel)
            } else {
                val mediaItem = resolveMediaItem(channel.url, channel.toMediaItem().mediaMetadata, channel)
                withContext(Dispatchers.Main) {
                    val player = getPlayer()
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    setVideoEnabled(!isVideoRestrictedState)
                    player.playWhenReady = true
                }
            }
        }
    }

    /**
     * Plays a DASH recording that was saved as two separate fragmented-MP4 files
     * (video and audio), combined via pipe separator in the channel URL.
     * Uses ExoPlayer's MergingMediaSource to play both tracks in sync.
     */
    /**
     * Plays a DASH recording that was saved as two separate fragmented-MP4 files
     * (video and audio), combined via pipe separator in the channel URL.
     * Uses ExoPlayer's MergingMediaSource to play both tracks in sync.
     */
    @OptIn(UnstableApi::class)
    private suspend fun playDualFileRecording(channel: ChannelEntity) {
        val parts = channel.url.split("|")
        val videoPath = parts[0].let { if (it.startsWith("/")) "file://$it" else it }
        val audioPath = parts.getOrNull(1)?.let { if (it.startsWith("/")) "file://$it" else it }

        android.util.Log.d("Watcharr", "playDualFileRecording: video=$videoPath, audio=$audioPath")

        val targetChannelId = findChannelId(channel, null)
        val recordingRecord = withContext(Dispatchers.IO) {
            try {
                val db = io.github.runc0derun.watcharr.shared.data.db.AppDatabase.getDatabase(context)
                db.dvrRecordingDao().getAllRecordings().firstOrNull { it.localFilePath?.contains(parts[0]) == true }
            } catch (e: Exception) { null }
        }
        val keySetIdB64 = recordingRecord?.drmKeySetId

        val drmConfig = if (targetChannelId != null) {
            buildDrmConfiguration(
                targetChannelId,
                null,
                try { channel.url.toUri() } catch (e: Exception) { null },
                keySetIdB64 = keySetIdB64
            )
        } else null

        withContext(Dispatchers.Main) {
            val player = getPlayer()
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val extractorsFactory = DefaultExtractorsFactory()
                .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_TFDT_BOX)

            val videoItemBuilder = MediaItem.Builder().setUri(videoPath)
            if (drmConfig != null) {
                videoItemBuilder.setDrmConfiguration(drmConfig)
            }
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(videoItemBuilder.build())

            val mediaSource = if (audioPath != null) {
                val audioItemBuilder = MediaItem.Builder().setUri(audioPath)
                if (drmConfig != null) {
                    audioItemBuilder.setDrmConfiguration(drmConfig)
                }
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                    .createMediaSource(audioItemBuilder.build())
                MergingMediaSource(videoSource, audioSource)
            } else {
                videoSource
            }

            player.setMediaSource(mediaSource)
            player.prepare()
            setVideoEnabled(!isVideoRestrictedState)
            player.playWhenReady = true
        }
    }

    private suspend fun getLicenseUri(targetChannelId: String, uri: Uri?, originalUri: Uri?): String? {
        var authority = uri?.authority?.takeIf { it.contains("delta") || (uri.pathSegments ?: emptyList()).contains("live") }
            ?: originalUri?.authority?.takeIf { it.contains("delta") || (originalUri.pathSegments ?: emptyList()).contains("live") }

        var scheme = uri?.scheme?.takeIf { !it.startsWith("file") } ?: originalUri?.scheme?.takeIf { !it.startsWith("file") } ?: "https"

        if (authority == null) {
            val matchedChannel = activeChannelList.firstOrNull { ch ->
                val chUri = try { ch.url.toUri() } catch (e: Exception) { null }
                ch.tvgId == targetChannelId || (chUri != null && chUri.pathSegments.contains(targetChannelId))
            } ?: currentChannel

            if (matchedChannel != null && matchedChannel.url.startsWith("http", ignoreCase = true)) {
                val resolved = resolveRedirect(matchedChannel.url)
                val resUri = try { resolved.url.toUri() } catch (e: Exception) { null }
                authority = resUri?.authority ?: try { matchedChannel.url.toUri().authority } catch (e: Exception) { null }
                scheme = resUri?.scheme ?: "https"
            }
        }

        if (authority == null) {
            authority = uri?.authority?.takeIf { !it.startsWith("file") } ?: originalUri?.authority?.takeIf { !it.startsWith("file") }
        }

        return if (authority != null) {
            "$scheme://$authority/license/$targetChannelId"
        } else null
    }

    private suspend fun buildDrmConfiguration(
        targetChannelId: String,
        uri: Uri?,
        originalUri: Uri?,
        keySetIdB64: String? = null
    ): MediaItem.DrmConfiguration? {
        if (!keySetIdB64.isNullOrEmpty()) {
            return try {
                val keySetId = android.util.Base64.decode(keySetIdB64, android.util.Base64.DEFAULT)
                android.util.Log.d("Watcharr", "buildDrmConfiguration: Using stored offline keySetId")
                MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.WIDEVINE_UUID)
                    .setKeySetId(keySetId)
                    .build()
            } catch (e: Exception) {
                android.util.Log.w("Watcharr", "Failed to decode keySetIdB64", e)
                null
            }
        }

        val licenseUri = getLicenseUri(targetChannelId, uri, originalUri) ?: return null
        val tokenParam = uri?.getQueryParameter("token") ?: originalUri?.getQueryParameter("token")

        val fullLicenseUri = if (!tokenParam.isNullOrEmpty() && !licenseUri.contains("token=")) {
            val sep = if (licenseUri.contains("?")) "&" else "?"
            "$licenseUri${sep}token=$tokenParam"
        } else {
            licenseUri
        }

        android.util.Log.d("Watcharr", "buildDrmConfiguration: DRM licenseUri=$fullLicenseUri")

        val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.WIDEVINE_UUID)
            .setLicenseUri(fullLicenseUri)
            .setMultiSession(true)
            .setForceDefaultLicenseUri(true)

        if (!tokenParam.isNullOrEmpty()) {
            drmConfigBuilder.setLicenseRequestHeaders(mapOf("Authorization" to "Bearer $tokenParam"))
        }

        return drmConfigBuilder.build()
    }

    internal fun findChannelId(channel: ChannelEntity?, uri: Uri?): String? {
        val pathSegments = uri?.pathSegments.takeIf { !it.isNullOrEmpty() } ?: run {
            val urlStr = channel?.url ?: ""
            try {
                java.net.URI(urlStr).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        val liveIndex = pathSegments.indexOf("live")
        if (liveIndex != -1 && liveIndex + 1 < pathSegments.size) {
            return pathSegments[liveIndex + 1]
        }

        val tvgId = channel?.tvgId?.trim()
        if (!tvgId.isNullOrEmpty()) {
            return tvgId
        }

        val targetChannel = channel ?: currentChannel
        if (targetChannel != null) {
            val matched = activeChannelList.firstOrNull {
                it.name.equals(targetChannel.name, ignoreCase = true) ||
                        (it.tvgName != null && it.tvgName.equals(targetChannel.name, ignoreCase = true))
            }
            if (matched != null) {
                val matchedTvgId = matched.tvgId?.trim()
                if (!matchedTvgId.isNullOrEmpty()) {
                    return matchedTvgId
                }
                val matchedUri = try { matched.url.toUri() } catch (e: Exception) { null }
                val matchedSegments = matchedUri?.pathSegments.takeIf { !it.isNullOrEmpty() } ?: try {
                    java.net.URI(matched.url).path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
                } catch (e: Exception) { emptyList() }
                val mLiveIndex = matchedSegments.indexOf("live")
                if (mLiveIndex != -1 && mLiveIndex + 1 < matchedSegments.size) {
                    return matchedSegments[mLiveIndex + 1]
                }
            }
        }
        return null
    }

    data class ResolvedUrlInfo(
        val url: String,
        val contentType: String? = null,
        val sniffedFormat: String? = null // "hls", "dash", or null (binary/unknown)
    )

    private suspend fun probeRecordingManifestEndpoint(recordingUrl: String): ResolvedUrlInfo? {
        val tokenParam = try { recordingUrl.toUri().getQueryParameter("token") } catch (e: Exception) { null }
        val baseUrl = recordingUrl.substringBefore("/file/").removeSuffix("/")
        if (baseUrl == recordingUrl || !baseUrl.contains("/recordings/")) return null

        val candidates = listOf(
            "$baseUrl/hls/index.m3u8",
            "$baseUrl/index.m3u8",
            "$baseUrl/master.m3u8",
            "$baseUrl/manifest.mpd",
            "$baseUrl/dash/manifest.mpd",
            "$baseUrl/stream/"
        )

        for (candidate in candidates) {
            val fullUrl = if (!tokenParam.isNullOrEmpty()) "$candidate?token=$tokenParam" else candidate
            android.util.Log.d("Watcharr", "Probing recording manifest candidate: $fullUrl")
            val probeResult = httpProbe(fullUrl, "HEAD", tokenParam)
            if (probeResult != null && probeResult.responseCode in 200..299) {
                val ct = probeResult.contentType?.lowercase() ?: ""
                android.util.Log.d("Watcharr", "Found valid recording manifest endpoint: $fullUrl, Content-Type=$ct")
                val sniffed = if (fullUrl.contains(".m3u8") || ct.contains("mpegurl")) "hls" else if (fullUrl.contains(".mpd") || ct.contains("dash")) "dash" else null
                return ResolvedUrlInfo(fullUrl, probeResult.contentType, sniffed)
            }
        }
        return null
    }

    internal suspend fun resolveMediaItem(
        url: String,
        metadata: androidx.media3.common.MediaMetadata,
        channel: ChannelEntity? = null
    ): MediaItem {
        android.util.Log.d("Watcharr", "resolveMediaItem: Original URL: $url")
        val isLocalFile = url.startsWith("/") || url.startsWith("file://")
        val formattedLocalUri = if (url.startsWith("/")) "file://$url" else url

        val resolvedInfo = if (isLocalFile) {
            ResolvedUrlInfo(formattedLocalUri, null, null)
        } else {
            resolveRedirect(url)
        }
        val targetChannel = channel ?: currentChannel
        val isRecording = isLocalFile || resolvedInfo.url.contains("/recordings/") || resolvedInfo.url.contains("/dvr/") || targetChannel?.groupTitle == "Recordings"

        var actualUrlInfo = resolvedInfo
        if (isRecording && resolvedInfo.url.contains("/file/")) {
            val manifestProbe = probeRecordingManifestEndpoint(resolvedInfo.url)
            if (manifestProbe != null) {
                actualUrlInfo = manifestProbe
            }
        }

        val finalUrl = actualUrlInfo.url
        val serverContentType = actualUrlInfo.contentType?.lowercase() ?: ""
        val sniffedFormat = actualUrlInfo.sniffedFormat
        android.util.Log.d("Watcharr", "resolveMediaItem: Resolved URL: $finalUrl, Content-Type: $serverContentType, sniffed: $sniffedFormat")

        val mediaItemBuilder = MediaItem.Builder()
            .setMediaId(url)
            .setUri(finalUrl)
            .setMediaMetadata(metadata)

        // Only apply LiveConfiguration for live streams, not recordings (which are VOD)
        if (!isRecording) {
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(3000)
                    .setMinPlaybackSpeed(0.95f)
                    .setMaxPlaybackSpeed(1.05f)
                    .build()
            )
        }

        // Detect container format from URL patterns, server Content-Type, and content sniffing
        val isHls = finalUrl.contains(".m3u8") || finalUrl.contains("/hls/") ||
                serverContentType.contains("mpegurl") || serverContentType.contains("m3u8") ||
                sniffedFormat == "hls"
        val isDash = finalUrl.contains(".mpd") || finalUrl.contains("/dash/") ||
                serverContentType.contains("dash+xml") ||
                sniffedFormat == "dash"
        val isTs = finalUrl.contains(".ts") || serverContentType.contains("video/mp2t") || serverContentType.contains("transport-stream")
        val isMp4 = finalUrl.contains(".mp4") || serverContentType.contains("video/mp4")
        val isMkv = finalUrl.contains(".mkv") || serverContentType.contains("matroska") || serverContentType.contains("webm")

        val uri = try { finalUrl.toUri() } catch (e: Exception) { null }
        val originalUri = try { url.toUri() } catch (e: Exception) { null }
        val pathSegments = uri?.pathSegments ?: emptyList()
        val liveIndex = pathSegments.indexOf("live")

        val isLiveWidevineProxy = liveIndex != -1 && pathSegments.size == liveIndex + 2 && !isHls

        val targetChannelId = findChannelId(targetChannel, uri)

        // Set MIME type based on detected format
        val mimeType = when {
            isLocalFile -> null
            isLiveWidevineProxy || isDash -> "application/dash+xml"
            isHls -> "application/x-mpegURL"
            isMp4 -> "video/mp4"
            isTs -> "video/mp2t"
            isMkv -> "video/x-matroska"
            else -> null
        }
        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }
        android.util.Log.d("Watcharr", "resolveMediaItem: isRecording=$isRecording, isHls=$isHls, isDash=$isDash, isMkv=$isMkv, isLiveProxy=$isLiveWidevineProxy, mimeType=$mimeType, channelId=$targetChannelId")

        // Apply DRM for live Widevine proxy, DASH streams, and encrypted recordings (both local and remote).
        if (targetChannelId != null && (isLiveWidevineProxy || isDash || isRecording)) {
            try {
                val recordingRecord = if (isRecording) {
                    try {
                        val db = io.github.runc0derun.watcharr.shared.data.db.AppDatabase.getDatabase(context)
                        val targetPath = finalUrl.removePrefix("file://")
                        db.dvrRecordingDao().getAllRecordings().firstOrNull { it.localFilePath?.contains(targetPath) == true }
                    } catch (e: Exception) { null }
                } else null

                val drmConfig = buildDrmConfiguration(
                    targetChannelId,
                    uri,
                    originalUri,
                    keySetIdB64 = recordingRecord?.drmKeySetId
                )
                if (drmConfig != null) {
                    mediaItemBuilder.setDrmConfiguration(drmConfig)
                }
            } catch (e: Exception) {
                android.util.Log.e("Watcharr", "resolveMediaItem: Failed to configure DRM", e)
            }
        }

        return mediaItemBuilder.build()
    }

    fun togglePlay() {
        exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    private fun scheduleRetry() {
        val channel = currentChannel ?: return
        if (retryAttempt >= MAX_RETRIES) {
            _playbackState.value = PlaybackState.Error("Failed to reconnect after $MAX_RETRIES attempts.")
            return
        }

        retryJob?.cancel()
        retryJob = scope.launch {
            retryAttempt++
            val backoffMs = retryAttempt * RETRY_BASE_DELAY_MS
            delay(backoffMs.milliseconds)
            play(channel)
        }
    }

    fun stop() {
        retryJob?.cancel()
        retryAttempt = 0
        currentChannel = null
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _playbackState.value = PlaybackState.Idle
    }

    fun release() {
        retryJob?.cancel()
        job.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState.Idle
        PlayerEngineProvider.clear()
    }

    fun setActiveChannelList(channels: List<ChannelEntity>) {
        activeChannelList = channels
    }

    fun setCurrentChannel(channel: ChannelEntity) {
        currentChannel = channel
    }

    fun setVideoEnabled(enabled: Boolean) {
        isVideoRestrictedState = !enabled
        exoPlayer?.let { player ->
            val videoTrackType = androidx.media3.common.C.TRACK_TYPE_VIDEO
            val parameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(videoTrackType, !enabled)
                .build()
            player.trackSelectionParameters = parameters
        }
        currentChannel?.let { ch ->
            if (_playbackState.value is PlaybackState.Playing) {
                _playbackState.value = PlaybackState.Playing(ch, isVideoRestrictedState)
            }
        }
    }

    fun updateVideoRestriction(restricted: Boolean) {
        android.util.Log.d("Watcharr", "updateVideoRestriction: restricted=$restricted")
        isVideoRestrictedState = restricted
        setVideoEnabled(!restricted)
        val state = _playbackState.value
        if (state is PlaybackState.Playing) {
            _playbackState.value = state.copy(isVideoRestricted = restricted)
        }
    }

    fun playNextChannel() {
        val current = currentChannel ?: return
        if (activeChannelList.isEmpty()) return
        val index = activeChannelList.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val nextIndex = (index + 1) % activeChannelList.size
            play(activeChannelList[nextIndex])
        }
    }

    fun playPreviousChannel() {
        val current = currentChannel ?: return
        if (activeChannelList.isEmpty()) return
        val index = activeChannelList.indexOfFirst { it.url == current.url }
        if (index != -1) {
            val prevIndex = (index - 1 + activeChannelList.size) % activeChannelList.size
            play(activeChannelList[prevIndex])
        }
    }

    /**
     * Resolves redirects and detects content format.
     * Uses HEAD first for efficiency, falls back to a small-range GET if HEAD fails
     * or returns no Content-Type. For URLs without file extensions (like recording
     * endpoints), also sniffs the first bytes to distinguish HLS/DASH/binary.
     */
    private suspend fun resolveRedirect(urlStr: String, redirectDepth: Int = 0): ResolvedUrlInfo {
        if (redirectDepth > 5 || (!urlStr.startsWith("http://", ignoreCase = true) && !urlStr.startsWith("https://", ignoreCase = true))) {
            return ResolvedUrlInfo(urlStr, null, null)
        }
        return withContext(Dispatchers.IO) {
            val originalToken = try { urlStr.toUri().getQueryParameter("token") } catch (e: Exception) { null }

            // Step 1: Try HEAD request for redirect resolution and Content-Type
            try {
                val headResult = httpProbe(urlStr, "HEAD", originalToken)
                if (headResult != null) {
                    if (headResult.isRedirect) {
                        var resolved = headResult.location!!
                        if (!originalToken.isNullOrEmpty() && !resolved.contains("token=")) {
                            val sep = if (resolved.contains("?")) "&" else "?"
                            resolved = "$resolved${sep}token=$originalToken"
                        }
                        return@withContext resolveRedirect(resolved, redirectDepth + 1)
                    }
                    // HEAD succeeded with a 2xx status code and a useful Content-Type — no need to sniff
                    if (headResult.responseCode in 200..299) {
                        val ct = headResult.contentType?.lowercase() ?: ""
                        if (ct.isNotEmpty() && !ct.contains("octet-stream") && !ct.contains("html") && !ct.contains("json")) {
                            android.util.Log.d("Watcharr", "resolveRedirect: HEAD Content-Type=$ct for $urlStr")
                            return@withContext ResolvedUrlInfo(urlStr, headResult.contentType)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("Watcharr", "resolveRedirect: HEAD failed for $urlStr: ${e.message}")
            }

            // Step 2: Small-range GET to sniff content format (reads first 32 bytes)
            try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Range", "bytes=0-31")
                if (!originalToken.isNullOrEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer $originalToken")
                }

                val responseCode = conn.responseCode
                val contentType = conn.contentType

                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrEmpty()) {
                        var resolved = java.net.URL(java.net.URL(urlStr), location).toString()
                        if (!originalToken.isNullOrEmpty() && !resolved.contains("token=")) {
                            val sep = if (resolved.contains("?")) "&" else "?"
                            resolved = "$resolved${sep}token=$originalToken"
                        }
                        return@withContext resolveRedirect(resolved, redirectDepth + 1)
                    }
                }

                // Read first bytes to sniff format
                var sniffedFormat: String? = null
                if (responseCode in 200..299) {
                    try {
                        val bytes = conn.inputStream.use { it.readNBytes(32) }
                        sniffedFormat = sniffContentFormat(bytes)
                        android.util.Log.d("Watcharr", "resolveRedirect: Sniffed format=$sniffedFormat, Content-Type=$contentType, first bytes=${bytes.take(16).joinToString(" ") { "%02x".format(it) }}")
                    } catch (e: Exception) {
                        android.util.Log.w("Watcharr", "resolveRedirect: Failed to sniff content: ${e.message}")
                    }
                }
                conn.disconnect()
                return@withContext ResolvedUrlInfo(urlStr, contentType, sniffedFormat)
            } catch (e: Exception) {
                android.util.Log.e("Watcharr", "resolveRedirect: Range GET failed for $urlStr", e)
            }

            return@withContext ResolvedUrlInfo(urlStr, null, null)
        }
    }

    private data class HttpProbeResult(
        val responseCode: Int,
        val contentType: String?,
        val location: String?,
        val isRedirect: Boolean
    )

    private fun httpProbe(urlStr: String, method: String, token: String?): HttpProbeResult? {
        var conn: java.net.HttpURLConnection? = null
        try {
            val url = java.net.URL(urlStr)
            conn = url.openConnection() as java.net.HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = method
            if (!token.isNullOrEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            val responseCode = conn.responseCode
            val contentType = conn.contentType
            val location = if (responseCode in 300..399) {
                val loc = conn.getHeaderField("Location")
                if (!loc.isNullOrEmpty()) java.net.URL(java.net.URL(urlStr), loc).toString() else null
            } else null
            return HttpProbeResult(responseCode, contentType, location, responseCode in 300..399)
        } catch (e: Exception) {
            return null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Sniff content format from the first bytes of a response.
     * Returns "hls" for HLS playlists, "dash" for DASH manifests, or null for binary/unknown.
     */
    private fun sniffContentFormat(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val text = try { String(bytes, Charsets.UTF_8) } catch (e: Exception) { return null }
        val trimmed = text.trimStart()
        return when {
            trimmed.startsWith("#EXTM3U") -> "hls"
            trimmed.startsWith("<?xml") || trimmed.startsWith("<MPD") -> "dash"
            else -> null
        }
    }

    companion object {
        private const val MAX_RETRIES = 5
        private const val RETRY_BASE_DELAY_MS = 2000L
    }
}
