package git.shin.rakuyomi_bridge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import git.shin.rakuyomi_bridge.util.AppIntegrityChecker

@HiltAndroidApp
class RakuyomiApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AppIntegrityChecker.checkIntegrity(this, BuildConfig.RELEASE_CERT_SHA256, BuildConfig.DEBUG)
  }
}
