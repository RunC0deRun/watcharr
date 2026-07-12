package io.github.runc0derun.shared.playback

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
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
