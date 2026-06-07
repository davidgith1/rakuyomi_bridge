package git.shin.rakuyomi_bridge.headless.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import git.shin.rakuyomi_bridge.DEFAULT_SERVER_PORT
import git.shin.rakuyomi_bridge.headless.HeadlessApp
import git.shin.rakuyomi_bridge.headless.R
import git.shin.rakuyomi_bridge.headless.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class ServerService : Service() {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var wakeLock: PowerManager.WakeLock? = null

  private val app: HeadlessApp get() = HeadlessApp.from(this)

  companion object {
    const val CHANNEL_ID = "rakuyomi_server"
    const val NOTIFICATION_ID = 1
    const val ACTION_START = "git.shin.rakuyomi_bridge.action.START"
    const val ACTION_STOP = "git.shin.rakuyomi_bridge.action.STOP"
  }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return when (intent?.action) {
      ACTION_STOP -> {
        stopServer()
        START_NOT_STICKY
      }

      else -> {
        startServer()
        START_STICKY
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
    if (app.server.isRunning) return

    val notification = buildNotification(getString(R.string.notification_text_starting))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    val pm = getSystemService(POWER_SERVICE) as PowerManager
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rakuyomi:androidHeadless:server")
    wakeLock?.acquire(4 * 60 * 60 * 1000L) // max 4 hours

    scope.launch {
      try {
        val homePath = app.settings.homePathFlow.first()
        app.networkBridge.start()
        app.server.start(homePath)
        updateNotification(getString(R.string.notification_text_running, DEFAULT_SERVER_PORT))
      } catch (e: Exception) {
        e.printStackTrace()
        stopSelf()
      }
    }
  }

  private fun stopServer() {
    if (!app.server.isRunning) return
    app.networkBridge.stop()
    scope.launch {
      runCatching { app.server.stop() }
      withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
          stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
          stopForeground(true)
        }
        stopSelf()
      }
    }
    wakeLock?.let { if (it.isHeld) it.release() }
    wakeLock = null
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
      CHANNEL_ID,
      getString(R.string.channel_name),
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = getString(R.string.channel_description)
      setShowBadge(false)
    }
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(channel)
  }

  private fun buildNotification(text: String): Notification {
    val openIntent = PendingIntent.getActivity(
      this, 0,
      Intent(this, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      pendingIntentFlags()
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
    } else {
      Notification.Builder(this)
    }

    val notif = builder
      .setContentTitle(getString(R.string.app_name))
      .setContentText(text)
      .setSmallIcon(R.drawable.ic_notification)
      .setOngoing(true)
      .setContentIntent(openIntent)

    // Notification.PRIORITY_LOW is API 20+. For API 18-19 we simply skip
    // the priority hint — the notification is shown with the platform
    // default.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      notif.setPriority(Notification.PRIORITY_LOW)
    }

    return notif.build()
  }

  private fun pendingIntentFlags(): Int {
    val base = PendingIntent.FLAG_UPDATE_CURRENT
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      base or PendingIntent.FLAG_IMMUTABLE
    } else {
      base
    }
  }

  private fun updateNotification(text: String) {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, buildNotification(text))
  }
}
