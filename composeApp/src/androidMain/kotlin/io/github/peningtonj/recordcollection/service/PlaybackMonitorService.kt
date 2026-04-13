package io.github.peningtonj.recordcollection.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.MainActivity
import io.github.peningtonj.recordcollection.R
import io.github.peningtonj.recordcollection.RecordCollectionApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the app process exempt from Android Doze mode.
 *
 * Why this is needed:
 * - When the screen turns off, Android's Doze mode restricts network access
 *   and may suspend CPU execution for background apps.
 * - A Foreground Service posts a visible notification and tells the OS that
 *   this app is doing active, user-visible work — granting it an exemption
 *   from Doze network/CPU restrictions.
 * - The [PlaybackSessionManager] (which owns the polling coroutine) runs in
 *   its own process-scoped CoroutineScope. This service simply keeps Android
 *   from throttling that work.
 * - A PARTIAL_WAKE_LOCK prevents the CPU from being suspended between polls.
 *
 * Lifecycle:
 * - Started by [MainActivity] when [PlaybackSessionManager.currentSession] becomes non-null.
 * - Stops itself when the session becomes null.
 */
class PlaybackMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "playback_monitor_channel"
        // 4-hour timeout; service always releases the lock in onDestroy regardless
        private const val WAKE_LOCK_TIMEOUT_MS = 4 * 60 * 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, PlaybackMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RecordCollection::PlaybackWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Napier.d("PlaybackMonitorService started")

        val notification = buildNotification("Monitoring playback queue…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeLock?.takeIf { !it.isHeld }?.acquire(WAKE_LOCK_TIMEOUT_MS)

        val sessionManager =
            (applicationContext as RecordCollectionApplication)
                .dependencyContainer.playbackSessionManager

        serviceScope.launch {
            sessionManager.currentSession.collect { session ->
                if (session == null) {
                    Napier.d("PlaybackMonitorService: session ended, stopping self")
                    stopSelf()
                } else {
                    updateNotification(session.album.name)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Napier.d("PlaybackMonitorService destroyed")
        serviceScope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notification helpers ──────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Queue Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the album queue active when the screen is off"
                setSound(null, null)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(albumName: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Record Collection")
            .setContentText(albumName)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(albumName: String) {
        // On Android 13+ (API 33), notify() requires POST_NOTIFICATIONS permission.
        // If not granted the update is silently skipped — the initial foreground
        // notification posted by startForeground() remains visible.
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(albumName))
        }
    }
}
