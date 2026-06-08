package git.shin.rakuyomi_bridge.receiver

import git.shin.rakuyomi_bridge.receiver.BaseBootReceiver
import git.shin.rakuyomi_bridge.service.ServerService

class BootReceiver : BaseBootReceiver() {
  override val serviceClass: Class<*> = ServerService::class.java
  override val startAction: String = ServerService.ACTION_START
}
