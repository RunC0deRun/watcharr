package io.github.runc0derun.watcharr.shared.data.dvr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WatcharrRecordingService : Service() {

    companion object {
        const val ACTION_START_RECORDING = "io.github.runc0derun.watcharr.action.START_RECORDING"
        const val EXTRA_RECORDING_ID = "extra_recording_id"
        const val EXTRA_PROGRAM_TITLE = "extra_program_title"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_STREAM_URL = "extra_stream_url"
        const val EXTRA_STOP_EPOCH_MS = "extra_stop_epoch_ms"

        private const val NOTIFICATION_CHANNEL_ID = "watcharr_dvr_channel"
        private const val NOTIFICATION_ID = 9001
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var dvrEngine: WatcharrDvrEngine

    override fun onCreate() {
        super.onCreate()
        dvrEngine = WatcharrDvrEngine(applicationContext)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Watcharr::DVRRecordingWakeLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_RECORDING) {
            val recordingId = intent.getStringExtra(EXTRA_RECORDING_ID) ?: return START_NOT_STICKY
            val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: "Live Program"
            val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "IPTV Channel"
            val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
            val stopEpochMs = intent.getLongExtra(EXTRA_STOP_EPOCH_MS, System.currentTimeMillis() + 1_800_000)

            val prefs = getSharedPreferences("watcharr_prefs", Context.MODE_PRIVATE)
            val storageType = prefs.getString("dvr_storage_type", "ON_DEVICE") ?: "ON_DEVICE"
            val shareConfig = NetworkStorageManager.NetworkShareConfig(
                protocol = prefs.getString("nfs_smb_protocol", "SMB") ?: "SMB",
                host = prefs.getString("nfs_smb_host", "") ?: "",
                sharePath = prefs.getString("nfs_smb_share_path", "") ?: "",
                username = prefs.getString("nfs_smb_user", "") ?: "",
                password = prefs.getString("nfs_smb_pass", "") ?: ""
            )

            wakeLock?.acquire(stopEpochMs - System.currentTimeMillis() + 60_000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(programTitle, channelName, "Recording in progress..."),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(programTitle, channelName, "Recording in progress..."))
            }

            serviceScope.launch {
                try {
                    dvrEngine.recordStream(
                        recordingId = recordingId,
                        streamUrl = streamUrl,
                        stopEpochMs = stopEpochMs,
                        storageType = storageType,
                        shareConfig = shareConfig
                    ) { bytesRead ->
                        val mb = bytesRead / (1024 * 1024)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, buildNotification(programTitle, channelName, "Recorded $mb MB"))
                    }
                } finally {
                    if (wakeLock?.isHeld == true) {
                        wakeLock?.release()
                    }
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Watcharr DVR Recordings",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notifications for active Watcharr DVR recordings"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, channel: String, status: String) =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Recording: $title")
            .setContentText("$channel • $status")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
}
