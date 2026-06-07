package git.shin.rakuyomi_bridge.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
  val url: String,
  val title: String,
  val addedAt: Long
)

@Serializable
data class HistoryEntry(
  val url: String,
  val title: String,
  val visitedAt: Long
)

@Serializable
data class BrowserStorage(
  val bookmarks: List<Bookmark> = emptyList(),
  val history: List<HistoryEntry> = emptyList()
) {
  companion object {
    const val MAX_BOOKMARKS = 100
    const val MAX_HISTORY = 200
  }
}
