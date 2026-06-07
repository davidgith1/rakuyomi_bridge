package git.shin.rakuyomi_bridge.headless

import android.app.Application
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.ServerConfig
import git.shin.rakuyomi_bridge.headless.service.NetworkBridgeWorker
import git.shin.rakuyomi_bridge.headless.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class HeadlessApp : Application() {

  lateinit var server: RakuyomiServerAdapter
    private set

  lateinit var networkBridge: NetworkBridgeWorker
    private set

  lateinit var settings: SettingsStore
    private set

  override fun onCreate() {
    super.onCreate()

    settings = SettingsStore(this)

    val homePath = runBlocking { settings.homePathFlow.first() }
    val config = ServerConfig(homePath = homePath)
    server = RakuyomiServerAdapter(config)

    networkBridge = NetworkBridgeWorker(OkHttpClient.Builder().build())
  }

  companion object {
    fun from(context: android.content.Context): HeadlessApp =
      context.applicationContext as HeadlessApp
  }
}
