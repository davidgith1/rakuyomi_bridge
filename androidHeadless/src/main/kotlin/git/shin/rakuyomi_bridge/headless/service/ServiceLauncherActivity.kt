package git.shin.rakuyomi_bridge.headless.service

import git.shin.rakuyomi_bridge.service.BaseServerService
import git.shin.rakuyomi_bridge.service.BaseServiceLauncherActivity

class ServiceLauncherActivity : BaseServiceLauncherActivity() {
  override val serviceClass: Class<*> = ServerService::class.java
  override val actionStart: String = BaseServerService.ACTION_START
  override val actionStop: String = BaseServerService.ACTION_STOP
}
