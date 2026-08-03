package io.github.runc0derun.watcharr.shared.data.dvr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DvrBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val scheduler = DvrAlarmScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                scheduler.rescheduleAllPendingAlarms()
            }
        }
    }
}
