package git.shin.rakuyomi_bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import kotlinx.coroutines.*

/**
 * Foreground service that hosts the rakuyomi Rust server.
 *
 * The server runs inside the native library (librakuyomi_server.so) on
 * its own tokio runtime. This service merely manages the Android
 * lifecycle (foreground notification, wake lock, start/stop).
 */
class ServerService : Service() {

    private lateinit var server: RakuyomiServer
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "rakuyomi_server"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "git.shin.rakuyomi_bridge.action.START"
        const val ACTION_STOP = "git.shin.rakuyomi_bridge.action.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        server = RakuyomiServer()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopServer()
                return START_NOT_STICKY
            }
            else -> {
                startServer()
                return START_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        scope.cancel()
        super.onDestroy()
    }

    private fun startServer() {
        if (server.isRunning) return

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            buildNotification26(getString(R.string.notification_text_starting))
        } else {
            buildNotificationLegacy()
        }
        startForeground(NOTIFICATION_ID, notification)

        // Acquire partial wake lock so the server stays alive while
        // the device is in deep sleep (common on e‑ink readers).
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rakuyomi:server")
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // max 4 hours

        scope.launch {
            try {
                server.start()
                if (Build.VERSION.SDK_INT >= 26) {
                    updateNotification26(getString(R.string.notification_text_running, DEFAULT_SERVER_PORT))
                }
            } catch (e: Exception) {
                stopSelf()
            }
        }
    }

    private fun stopServer() {
        if (!server.isRunning) return
        runCatching { server.stop() }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // ── Notification helpers ──────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
            setShowBadge(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun buildNotificationLegacy(): Notification {
        return Notification(
            android.R.drawable.ic_menu_share,
            getString(R.string.app_name),
            System.currentTimeMillis()
        ).apply {
            setLatestEventInfo(
                this@ServerService,
                getString(R.string.app_name),
                getString(R.string.notification_text_starting),
                null
            )
            flags = flags or Notification.FLAG_ONGOING_EVENT
        }
    }

    private fun buildNotification26(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .build()
    }

    private fun updateNotification26(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification26(text))
    }
}
