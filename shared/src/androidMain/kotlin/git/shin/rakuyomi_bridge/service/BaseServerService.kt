package git.shin.rakuyomi_bridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import git.shin.rakuyomi_bridge.DEFAULT_SERVER_PORT
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
abstract class BaseServerService : Service() {

  abstract val server: RakuyomiServerAdapter
  abstract val networkBridgeWorker: NetworkBridgeWorker
  protected abstract val appName: String
  protected abstract val mainActivityClass: Class<*>
  protected abstract val notificationIconRes: Int
  protected abstract val wakeLockTag: String

  protected abstract suspend fun getHomePath(): String

  protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private var wakeLock: PowerManager.WakeLock? = null

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

  protected open fun getStringResource(id: String, vararg args: Any): String {
    val resId = resources.getIdentifier(id, "string", packageName)
    return if (resId != 0) getString(resId, *args) else id
  }

  private fun startServer() {
    if (server.isRunning) return

    val startingText = getStringResource("notification_text_starting")
    val notification = buildNotification(startingText)
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
    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
    wakeLock?.acquire(4 * 60 * 60 * 1000L) // max 4 hours

    scope.launch {
      try {
        Log.e("RakDiag", "startServer: coroutine launched, resolving home path")
        val homePath = getHomePath()
        Log.e("RakDiag", "startServer: homePath=$homePath, starting networkBridgeWorker")
        networkBridgeWorker.start()
        Log.e("RakDiag", "startServer: calling server.start()")
        server.start(homePath)
        Log.e("RakDiag", "startServer: server.start() returned, status=${server.status.value}")
        val runningText = getStringResource("notification_text_running", DEFAULT_SERVER_PORT)
        updateNotification(runningText)
      } catch (e: Exception) {
        Log.e("RakDiag", "startServer: FAILED with exception", e)
        e.printStackTrace()
        stopSelf()
      }
    }
  }

  private fun stopServer() {
    if (!server.isRunning) return
    networkBridgeWorker.stop()
    scope.launch {
      runCatching { server.stop() }
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
      getStringResource("channel_name"),
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = getStringResource("channel_description")
      setShowBadge(false)
    }
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(channel)
  }

  private fun buildNotification(text: String): Notification {
    val openIntent = PendingIntent.getActivity(
      this, 0,
      Intent(this, mainActivityClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
      pendingIntentFlags()
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
    } else {
      Notification.Builder(this)
    }

    val notif = builder
      .setContentTitle(appName)
      .setContentText(text)
      .setSmallIcon(notificationIconRes)
      .setOngoing(true)
      .setContentIntent(openIntent)

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
