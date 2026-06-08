package git.shin.rakuyomi_bridge.headless

import android.app.Application
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.ServerConfig
import git.shin.rakuyomi_bridge.headless.settings.SettingsStore
import git.shin.rakuyomi_bridge.remote.UpdateManager
import git.shin.rakuyomi_bridge.remote.WebViewCookieJar
import git.shin.rakuyomi_bridge.service.NetworkBridgeWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class HeadlessApp : Application() {

  lateinit var server: RakuyomiServerAdapter
    private set

  lateinit var networkBridge: NetworkBridgeWorker
    private set

  lateinit var settings: SettingsStore
    private set

  lateinit var updateManager: UpdateManager
    private set

  private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
  }

  override fun onCreate() {
    super.onCreate()

    settings = SettingsStore(this)

    val homePath = runBlocking { settings.homePathFlow.first() }
    val config = ServerConfig(homePath = homePath)
    server = RakuyomiServerAdapter(config)

    val client = OkHttpClient.Builder()
      .cookieJar(WebViewCookieJar())
      .build()
    networkBridge = NetworkBridgeWorker(client)
    updateManager = UpdateManager(this, client, json, BuildConfig.VERSION_NAME, true)
  }

  companion object {
    fun from(context: android.content.Context): HeadlessApp =
      context.applicationContext as HeadlessApp
  }
}
