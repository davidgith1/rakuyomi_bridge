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
import dagger.hilt.android.AndroidEntryPoint
import git.shin.rakuyomi_bridge.DEFAULT_SERVER_PORT
import git.shin.rakuyomi_bridge.MainActivity
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class ServerService : Service() {

  @Inject
  lateinit var server: RakuyomiServerAdapter

  @Inject
  lateinit var settingsRepository: SettingsRepository

  @Inject
  lateinit var networkBridgeWorker: NetworkBridgeWorker

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
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rakuyomi:server")
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // max 4 hours

        scope.launch {
            try {
              val homePath = settingsRepository.homePathFlow.first()
              networkBridgeWorker.start()
              server.start(homePath)
              updateNotification(getString(R.string.notification_text_running, DEFAULT_SERVER_PORT))
            } catch (e: Exception) {
              print(e)
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
            @Suppress("DEPRECATION")
            stopForeground(true)
          }
          stopSelf()
        }
      }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

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
      val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

  private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    val builder = if (Build.VERSION.SDK_INT >= 26) {
      Notification.Builder(this, CHANNEL_ID)
    } else {
      @Suppress("DEPRECATION")
      Notification.Builder(this)
    }

    return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .build()
    }

  private fun updateNotification(text: String) {
    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
