package git.shin.rakuyomi_bridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import git.shin.rakuyomi_bridge.service.ServerService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
          val serviceIntent = Intent(context, ServerService::class.java).apply {
                action = ServerService.ACTION_START
          }
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
          } else {
            context.startService(serviceIntent)
          }
        }
    }
}
