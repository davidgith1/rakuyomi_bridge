package git.shin.rakuyomi_bridge.headless.service

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Theme-less entry point that translates deep-link intents into service
 * start/stop actions. Registered for `rakuyomi_bridge_headless://start`
 * and `...://stop` so that external tools (Tasker, KOReader, adb) can drive
 * the server without showing any UI.
 */
class ServiceLauncherActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val data = intent.data

    val serviceAction = when (data?.host) {
      "start" -> ServerService.ACTION_START
      "stop" -> ServerService.ACTION_STOP
      else -> null
    }

    if (serviceAction != null) {
      val serviceIntent = Intent(this, ServerService::class.java).apply {
        action = serviceAction
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(this, serviceIntent)
      } else {
        startService(serviceIntent)
      }
    }

    finish()
  }
}
