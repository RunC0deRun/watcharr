package com.iptv.shared.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.mvi.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerEngine(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var retryJob: Job? = null
    private var retryAttempt = 0
    private var currentChannel: ChannelEntity? = null
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

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
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
                val errorMessage = error.localizedMessage ?: "Playback Error"
                _playbackState.value = PlaybackState.Error(errorMessage)
                scheduleRetry()
            }
        })

        return player
    }

    fun play(channel: ChannelEntity) {
        retryJob?.cancel()
        currentChannel = channel
        
        val player = getPlayer()
        
        val mediaItem = MediaItem.Builder()
            .setUri(channel.url)
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(3000)
                    .setMinPlaybackSpeed(0.95f)
                    .setMaxPlaybackSpeed(1.05f)
                    .build()
            )
            .build()

        _playbackState.value = PlaybackState.Loading
        player.setMediaItem(mediaItem)
        player.prepare()
        setVideoEnabled(!isVideoRestrictedState)
        player.playWhenReady = true
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
            delay(backoffMs)
            play(channel)
        }
    }

    fun release() {
        retryJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState.Idle
    }

    fun setActiveChannelList(channels: List<ChannelEntity>) {
        activeChannelList = channels
    }

    fun setCurrentChannel(channel: ChannelEntity) {
        currentChannel = channel
    }

    fun setVideoEnabled(enabled: Boolean) {
        val player = exoPlayer ?: return
        val parameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_VIDEO, !enabled)
            .build()
        player.trackSelectionParameters = parameters
    }

    fun updateVideoRestriction(restricted: Boolean) {
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

    companion object {
        private const val MAX_RETRIES = 5
        private const val RETRY_BASE_DELAY_MS = 2000L
    }
}
