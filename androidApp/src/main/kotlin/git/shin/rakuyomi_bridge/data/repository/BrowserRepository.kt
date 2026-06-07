package git.shin.rakuyomi_bridge.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import git.shin.rakuyomi_bridge.data.model.Bookmark
import git.shin.rakuyomi_bridge.data.model.BrowserStorage
import git.shin.rakuyomi_bridge.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "browser")

@Singleton
class BrowserRepository @Inject constructor(
  @param:ApplicationContext private val context: Context
) {
  private val storageKey = stringPreferencesKey("browser_storage_json")
  private val webViewStateKey = byteArrayPreferencesKey("webview_state_bytes")
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  val storage: Flow<BrowserStorage> = context.browserDataStore.data
    .catch { e ->
      if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
    }
    .map { prefs ->
      val raw = prefs[storageKey] ?: return@map BrowserStorage()
      runCatching { json.decodeFromString(BrowserStorage.serializer(), raw) }
        .getOrDefault(BrowserStorage())
    }

  suspend fun addBookmark(bookmark: Bookmark) {
    updateStorage { current ->
      val filtered = current.bookmarks.filterNot { it.url == bookmark.url }
      val merged = (listOf(bookmark) + filtered).take(BrowserStorage.MAX_BOOKMARKS)
      current.copy(bookmarks = merged)
    }
  }

  suspend fun removeBookmark(url: String) {
    updateStorage { current ->
      current.copy(bookmarks = current.bookmarks.filterNot { it.url == url })
    }
  }

  fun isBookmarked(url: String, bookmarks: List<Bookmark>): Boolean =
    bookmarks.any { it.url == url }

  suspend fun addHistory(entry: HistoryEntry) {
    updateStorage { current ->
      val filtered = current.history.filterNot { it.url == entry.url }
      val merged = (listOf(entry) + filtered).take(BrowserStorage.MAX_HISTORY)
      current.copy(history = merged)
    }
  }

  suspend fun clearHistory() {
    updateStorage { current -> current.copy(history = emptyList()) }
  }

  suspend fun saveWebViewState(bytes: ByteArray) {
    context.browserDataStore.edit { prefs ->
      prefs[webViewStateKey] = bytes
    }
  }

  suspend fun loadWebViewState(): ByteArray? {
    val prefs = context.browserDataStore.data
      .catch { e ->
        if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e
      }
      .first()
    return prefs[webViewStateKey]
  }

  private suspend fun updateStorage(transform: (BrowserStorage) -> BrowserStorage) {
    context.browserDataStore.edit { prefs ->
      val raw = prefs[storageKey]
      val current = if (raw.isNullOrEmpty()) {
        BrowserStorage()
      } else {
        runCatching { json.decodeFromString(BrowserStorage.serializer(), raw) }
          .getOrDefault(BrowserStorage())
      }
      val updated = transform(current)
      prefs[storageKey] = json.encodeToString(BrowserStorage.serializer(), updated)
    }
  }
}
