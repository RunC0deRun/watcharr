package com.iptv.mobile

import android.app.Application
import com.iptv.shared.data.db.AppDatabase

class MobileApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
