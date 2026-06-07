package git.shin.rakuyomi_bridge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RakuyomiApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AppIntegrityChecker.checkIntegrity(this)
  }
}
