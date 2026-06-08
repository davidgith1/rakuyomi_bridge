package git.shin.rakuyomi_bridge.service

import dagger.hilt.android.AndroidEntryPoint
import git.shin.rakuyomi_bridge.MainActivity
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ServerService : BaseServerService() {

  @Inject
  override lateinit var server: RakuyomiServerAdapter

  @Inject
  lateinit var settingsRepository: SettingsRepository

  @Inject
  override lateinit var networkBridgeWorker: NetworkBridgeWorker

  override val appName: String get() = getString(R.string.app_name)
  override val mainActivityClass: Class<*> = MainActivity::class.java
  override val notificationIconRes: Int = R.drawable.ic_notification
  override val wakeLockTag: String = "rakuyomi:server"

  override suspend fun getHomePath(): String = settingsRepository.homePathFlow.first()
}
