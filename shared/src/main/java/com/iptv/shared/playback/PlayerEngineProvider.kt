package com.iptv.shared.playback

import android.content.Context

object PlayerEngineProvider {
    @Volatile
    private var instance: PlayerEngine? = null

    fun get(context: Context): PlayerEngine {
        return instance ?: synchronized(this) {
            instance ?: PlayerEngine(context.applicationContext).also { instance = it }
        }
    }

    fun clear() {
        synchronized(this) {
            instance = null
        }
    }
}
