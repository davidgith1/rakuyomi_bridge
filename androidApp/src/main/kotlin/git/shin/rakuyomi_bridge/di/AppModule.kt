package git.shin.rakuyomi_bridge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.ServerConfig
import git.shin.rakuyomi_bridge.data.repository.SettingsRepository
import git.shin.rakuyomi_bridge.remote.WebViewCookieJar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import javax.inject.Singleton

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
  fun provideServerConfig(settingsRepository: SettingsRepository): ServerConfig {
    // We use runBlocking here because provideServerConfig is called once for the Singleton.
    val homePath = runBlocking { settingsRepository.homePathFlow.first() }
    return ServerConfig(homePath = homePath)
  }

  @Provides
  @Singleton
  fun provideRakuyomiServer(config: ServerConfig): RakuyomiServerAdapter =
    RakuyomiServerAdapter(config)
}
