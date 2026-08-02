package io.github.runc0derun.watcharr.shared.data.dvr

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.runc0derun.watcharr.shared.data.db.AppDatabase
import io.github.runc0derun.watcharr.shared.data.db.DvrRecordingEntity

class DvrAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleRecordingAlarm(recording: DvrRecordingEntity) {
        val now = System.currentTimeMillis()
        // Wake up 1 minute before recording start time to ensure buffer setup
        val triggerTimeMs = (recording.startEpochMs - 60_000).coerceAtLeast(now + 1_000)

        val intent = Intent(context, WatcharrRecordingService::class.java).apply {
            action = WatcharrRecordingService.ACTION_START_RECORDING
            putExtra(WatcharrRecordingService.EXTRA_RECORDING_ID, recording.id)
            putExtra(WatcharrRecordingService.EXTRA_PROGRAM_TITLE, recording.programTitle)
            putExtra(WatcharrRecordingService.EXTRA_CHANNEL_NAME, recording.channelName)
            putExtra(WatcharrRecordingService.EXTRA_CHANNEL_ID, recording.channelId)
            putExtra(WatcharrRecordingService.EXTRA_STREAM_URL, recording.streamUrl)
            putExtra(WatcharrRecordingService.EXTRA_STOP_EPOCH_MS, recording.stopEpochMs)
        }

        val pendingIntent = PendingIntent.getForegroundService(
            context,
            recording.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("WatcharrDVR", "Failed to schedule exact alarm for recording ${recording.id}", e)
        }
    }

    fun cancelRecordingAlarm(recordingId: String) {
        val intent = Intent(context, WatcharrRecordingService::class.java).apply {
            action = WatcharrRecordingService.ACTION_START_RECORDING
        }
        val pendingIntent = PendingIntent.getForegroundService(
            context,
            recordingId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun rescheduleAllPendingAlarms() {
        val database = AppDatabase.getDatabase(context)
        val pendingRecordings = database.dvrRecordingDao().getUpcomingScheduledRecordings(System.currentTimeMillis())
        for (recording in pendingRecordings) {
            scheduleRecordingAlarm(recording)
        }
    }
}
