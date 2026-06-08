package git.shin.rakuyomi_bridge.headless.receiver

import git.shin.rakuyomi_bridge.headless.service.ServerService
import git.shin.rakuyomi_bridge.receiver.BaseBootReceiver

class BootReceiver : BaseBootReceiver() {
  override val serviceClass: Class<*> = ServerService::class.java
  override val startAction: String = ServerService.ACTION_START
}
