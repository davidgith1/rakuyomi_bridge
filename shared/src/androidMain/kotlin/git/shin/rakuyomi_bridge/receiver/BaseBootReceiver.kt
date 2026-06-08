package git.shin.rakuyomi_bridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

abstract class BaseBootReceiver : BroadcastReceiver() {
  protected abstract val serviceClass: Class<*>
  protected abstract val startAction: String

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      val serviceIntent = Intent(context, serviceClass).apply {
        action = startAction
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
    }
  }
}
