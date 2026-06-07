package git.shin.rakuyomi_bridge.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat

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
      ContextCompat.startForegroundService(this, serviceIntent)
    }

    finish()

  }
}
