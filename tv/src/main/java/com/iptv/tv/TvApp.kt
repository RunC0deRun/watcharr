package com.iptv.tv

import android.app.Application
import com.iptv.shared.data.db.AppDatabase

class TvApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
