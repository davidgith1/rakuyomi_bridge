package git.shin.rakuyomi_bridge.service

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat

abstract class BaseServiceLauncherActivity : Activity() {
  protected abstract val serviceClass: Class<*>
  protected abstract val actionStart: String
  protected abstract val actionStop: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val data = intent.data

    val serviceAction = when (data?.host) {
      "start" -> actionStart
      "stop" -> actionStop
      else -> null
    }

    if (serviceAction != null) {
      val serviceIntent = Intent(this, serviceClass).apply {
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
