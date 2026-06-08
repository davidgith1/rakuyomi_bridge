package git.shin.rakuyomi_bridge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import git.shin.rakuyomi_bridge.BuildConfig
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.ServerConfig
import git.shin.rakuyomi_bridge.data.repository.SettingsRepository
import git.shin.rakuyomi_bridge.remote.UpdateManager
import git.shin.rakuyomi_bridge.remote.WebViewCookieJar
import git.shin.rakuyomi_bridge.service.NetworkBridgeWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton
import android.content.Context

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
      .cookieJar(WebViewCookieJar())
      .build()
  }

  @Provides
  @Singleton
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
  }

  @Provides
  @Singleton
  fun provideServerConfig(settingsRepository: SettingsRepository): ServerConfig {
    val homePath = runBlocking { settingsRepository.homePathFlow.first() }
    return ServerConfig(homePath = homePath)
  }

  @Provides
  @Singleton
  fun provideRakuyomiServer(config: ServerConfig): RakuyomiServerAdapter =
    RakuyomiServerAdapter(config)

  @Provides
  @Singleton
  fun provideUpdateManager(
    @ApplicationContext context: Context,
    client: OkHttpClient,
    json: Json
  ): UpdateManager = UpdateManager(context, client, json, BuildConfig.VERSION_NAME, false)

  @Provides
  @Singleton
  fun provideNetworkBridgeWorker(client: OkHttpClient): NetworkBridgeWorker =
    NetworkBridgeWorker(client)
}
