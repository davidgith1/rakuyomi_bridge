package git.shin.rakuyomi_bridge.data.repository

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import git.shin.rakuyomi_bridge.ServerConfig
import git.shin.rakuyomi_bridge.data.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
  @param:ApplicationContext private val context: Context
) {
  private val homePathKey = stringPreferencesKey("home_path")
  private val themeKey = intPreferencesKey("app_theme")

  val homePathFlow: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[homePathKey] ?: getDefaultHomePath()
  }

  val themeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
    val themeIndex = preferences[themeKey] ?: AppTheme.SYSTEM.ordinal
    AppTheme.entries.getOrElse(themeIndex) { AppTheme.SYSTEM }
  }

  suspend fun setHomePath(path: String) {
    context.dataStore.edit { preferences ->
      preferences[homePathKey] = path
    }
  }

  suspend fun setTheme(theme: AppTheme) {
    context.dataStore.edit { preferences ->
      preferences[themeKey] = theme.ordinal
    }
  }

  private fun getDefaultHomePath(): String {
    val baseDir = Environment.getExternalStorageDirectory()
    return File(baseDir, "sckoreader/${ServerConfig.DATA_FOLDER_NAME}").absolutePath
  }
}
