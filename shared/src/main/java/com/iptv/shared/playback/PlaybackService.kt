package com.iptv.shared.playback

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.iptv.shared.data.db.AppDatabase
import com.iptv.shared.data.db.ChannelEntity
import com.iptv.shared.data.epg.EpgMatcher
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var playerEngine: PlayerEngine
    private lateinit var carRestrictionsManager: CarRestrictionsManager
    private lateinit var database: AppDatabase

    private val serviceJob = kotlinx.coroutines.SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getDatabase(this)
        playerEngine = PlayerEngineProvider.get(this)
        
        val basePlayer = playerEngine.getPlayer()
        
        // Wrap the ExoPlayer with a ForwardingPlayer to route next/prev events
        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {
            override fun seekToNext() {
                playerEngine.playNextChannel()
            }
            override fun seekToPrevious() {
                playerEngine.playPreviousChannel()
            }
            override fun seekToNextMediaItem() {
                playerEngine.playNextChannel()
            }
            override fun seekToPreviousMediaItem() {
                playerEngine.playPreviousChannel()
            }
        }
        
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            forwardingPlayer,
            LibraryCallback()
        ).build()

        carRestrictionsManager = CarRestrictionsManager(this)
        
        // Listen to car driving restrictions
        serviceScope.launch {
            carRestrictionsManager.isVideoRestricted.collect { restricted ->
                playerEngine.updateVideoRestriction(restricted)
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceJob.cancel()
        carRestrictionsManager.release()
        mediaLibrarySession.release()
        super.onDestroy()
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val packageName = controller.packageName
            val isTrusted = packageName == this@PlaybackService.packageName ||
                    packageName == "com.android.systemui" ||
                    packageName == "com.google.android.projection.gearhead" ||
                    packageName == "com.google.android.googlequicksearchbox" ||
                    controller.uid == android.os.Process.SYSTEM_UID ||
                    controller.uid == android.os.Process.myUid()

            if (isTrusted && packageName == "com.google.android.projection.gearhead") {
                playerEngine.updateVideoRestriction(true)
            }

            return if (isTrusted) {
                super.onConnect(session, controller)
            } else {
                MediaSession.ConnectionResult.reject()
            }
        }

        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onDisconnected(session, controller)
            if (controller.packageName == "com.google.android.projection.gearhead") {
                val hasOtherAA = session.connectedControllers.any {
                    it.packageName == "com.google.android.projection.gearhead" && it != controller
                }
                if (!hasOtherAA) {
                    playerEngine.updateVideoRestriction(false)
                }
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("ROOT")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val items = mutableListOf<MediaItem>()
            
            if (parentId == "ROOT") {
                items.add(createFolderItem("FAVORITES", "★ Favorites"))
                items.add(createFolderItem("RADIO", "📻 Radio & Music"))
                items.add(createFolderItem("ALL_CHANNELS", "📺 All Channels"))
                
                return Futures.immediateFuture(LibraryResult.ofItemList(items, params))
            }
            
            val listenable = SettableFuture.create<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val dbChannels = when (parentId) {
                        "FAVORITES" -> {
                            val favUrls = database.favoriteDao().getFavoriteUrls().toSet()
                            database.channelDao().getAllChannels().filter { favUrls.contains(it.url) }
                        }
                        "RADIO" -> {
                            database.channelDao().getAllChannels().filter { it.isRadioOrAudioOnly() }
                        }
                        "ALL_CHANNELS" -> {
                            database.channelDao().getAllChannels()
                        }
                        else -> emptyList()
                    }
                    
                    val now = System.currentTimeMillis()
                    val activePrograms = database.programDao().getActiveProgramsFlow(now).first()
                    
                    val mediaItems = dbChannels.map { channel ->
                        val currentProgram = activePrograms.firstOrNull {
                            EpgMatcher.isMatch(channel.tvgId, channel.name, it.channelId)
                        }
                        channel.toMediaItem(currentProgram?.title)
                    }
                    
                    items.addAll(mediaItems)
                    listenable.set(LibraryResult.ofItemList(items, params))
                } catch (e: Exception) {
                    listenable.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                }
            }
            return listenable
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val listenable = SettableFuture.create<LibraryResult<MediaItem>>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val channel = database.channelDao().getAllChannels().firstOrNull { it.url == mediaId }
                    if (channel != null) {
                        listenable.set(LibraryResult.ofItem(channel.toMediaItem(), null))
                    } else {
                        listenable.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                } catch (e: Exception) {
                    listenable.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
                }
            }
            return listenable
        }
        
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolvedItems = mediaItems.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.mediaId)
                    .setUri(item.mediaId)
                    .setMediaMetadata(item.mediaMetadata)
                    .build()
            }.toMutableList()
            
            serviceScope.launch(Dispatchers.IO) {
                val firstItem = resolvedItems.firstOrNull() ?: return@launch
                val channels = database.channelDao().getAllChannels()
                val channel = channels.firstOrNull { it.url == firstItem.mediaId }
                if (channel != null) {
                    launch(Dispatchers.Main) {
                        playerEngine.setActiveChannelList(channels)
                        playerEngine.setCurrentChannel(channel)
                    }
                }
            }
            
            return Futures.immediateFuture(resolvedItems)
        }
    }

    private fun createFolderItem(id: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .build()
            )
            .build()
    }
}
