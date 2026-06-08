package git.shin.rakuyomi_bridge.util

/**
 * Common URL normalization logic.
 *
 * This is in commonMain so it can be used by all platforms and modules.
 * Note: Does not use URLEncoder here because it's platform-specific.
 * Instead, we use it for basic scheme/host checking.
 */
object UrlUtils {
  private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
  private val hostLikeRegex = Regex("^[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)+(/.*)?$")

  /**
   * Basic normalization. Full normalization with encoding should be done
   * in platform-specific code if needed.
   */
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
    // Search is handled in platform-specific logic or simply returned as is
    return trimmed
  }

  const val DEFAULT_HOME: String = "https://www.google.com"
}
