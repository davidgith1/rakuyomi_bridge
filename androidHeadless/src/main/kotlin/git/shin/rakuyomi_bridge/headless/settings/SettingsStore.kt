package git.shin.rakuyomi_bridge.headless.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import git.shin.rakuyomi_bridge.ServerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import java.io.File

/**
 * Lightweight replacement for `androidx.datastore.preferences` that works on
 * API 18+. Only stores the data directory path; the headless app does not
 * need theme/theme/etc. settings.
 */
class SettingsStore(context: Context) {

  private val prefs: SharedPreferences = context.applicationContext
    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val key = stringPreferencesKey(KEY_HOME_PATH)

  val homePathFlow: Flow<String> = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, changedKey ->
      if (changedKey == KEY_HOME_PATH) {
        trySend(p.getString(changedKey, null) ?: defaultHomePath())
      }
    }
    prefs.registerOnSharedPreferenceChangeListener(listener)
    awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
  }
    .onStart { emit(prefs.getString(key, null) ?: defaultHomePath()) }
    .distinctUntilChanged()

  fun setHomePath(path: String) {
    prefs.edit().putString(key, path).apply()
  }

  private fun defaultHomePath(): String {
    val baseDir = Environment.getExternalStorageDirectory()
    return File(baseDir, "sckoreader/${ServerConfig.DATA_FOLDER_NAME}").absolutePath
  }

  companion object {
    private const val PREFS_NAME = "rakuyomi_headless_settings"
    private const val KEY_HOME_PATH = "home_path"

    // Mirror of [androidx.datastore.preferences.core.stringPreferencesKey].
    // Exposed here only to keep the call sites close to the DataStore API.
    private fun stringPreferencesKey(name: String): String = name
  }
}
