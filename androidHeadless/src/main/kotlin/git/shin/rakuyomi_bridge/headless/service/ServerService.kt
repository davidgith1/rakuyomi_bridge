package git.shin.rakuyomi_bridge.headless.service

import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.headless.HeadlessApp
import git.shin.rakuyomi_bridge.headless.R
import git.shin.rakuyomi_bridge.headless.ui.MainActivity
import git.shin.rakuyomi_bridge.service.BaseServerService
import git.shin.rakuyomi_bridge.service.NetworkBridgeWorker
import kotlinx.coroutines.flow.first

class ServerService : BaseServerService() {

  private val app: HeadlessApp get() = HeadlessApp.from(this)

  override val server: RakuyomiServerAdapter get() = app.server
  override val networkBridgeWorker: NetworkBridgeWorker get() = app.networkBridge

  override val appName: String get() = getString(R.string.app_name)
  override val mainActivityClass: Class<*> = MainActivity::class.java
  override val notificationIconRes: Int = R.drawable.ic_notification
  override val wakeLockTag: String = "rakuyomi:headless:server"

  override suspend fun getHomePath(): String = app.settings.homePathFlow.first()
}
