package git.shin.rakuyomi_bridge.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.model.UpdateInfo
import git.shin.rakuyomi_bridge.remote.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val updateManager: UpdateManager
) {
  private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
  val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

  private val _lastResult = MutableStateFlow<UpdateResult>(UpdateResult.Idle)
  val lastResult: StateFlow<UpdateResult> = _lastResult.asStateFlow()

  suspend fun checkForUpdate(): UpdateResult {
    return updateManager.checkForUpdate("rakuyomi-bridge-compose").fold(
      onSuccess = { info ->
        if (info.isNewer && info.downloadUrl.isNotEmpty()) {
          _updateInfo.value = info
          _lastResult.value = UpdateResult.UpdateAvailable(info)
        } else {
          _updateInfo.value = null
          _lastResult.value = UpdateResult.UpToDate
        }
        _lastResult.value
      },
      onFailure = { error ->
        _updateInfo.value = null
        val failed = UpdateResult.Failed(error.message ?: "Unknown error")
        _lastResult.value = failed
        failed
      }
    )
  }

  fun dismissUpdate() {
    _updateInfo.value = null
  }

  fun downloadAndInstall(info: UpdateInfo) {
    updateManager.downloadAndInstall(
      info.downloadUrl,
      "RakuyomiBridge_v${info.version}.apk",
      context.getString(R.string.update_downloading_title),
      context.getString(R.string.update_downloading_description)
    )
    dismissUpdate()
  }

  sealed class UpdateResult {
    data object Idle : UpdateResult()
    data object UpToDate : UpdateResult()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    data class Failed(val message: String) : UpdateResult()
  }
}
