package git.shin.rakuyomi_bridge.util

import java.net.URLEncoder

object UrlUtils {
  private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
  private val hostLikeRegex = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(/.*)?$")

  fun normalize(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
      return DEFAULT_HOME
    }
    if (schemeRegex.containsMatchIn(trimmed)) {
      return trimmed
    }
    if (hostLikeRegex.matches(trimmed)) {
      return "https://$trimmed"
    }
    return "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
  }

  const val DEFAULT_HOME: String = "https://www.google.com"
}
