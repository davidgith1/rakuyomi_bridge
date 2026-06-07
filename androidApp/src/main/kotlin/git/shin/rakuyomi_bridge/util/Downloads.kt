package git.shin.rakuyomi_bridge.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

object Downloads {

  fun start(
    context: Context,
    url: String,
    userAgent: String,
    contentDisposition: String?,
    mimeType: String?
  ): Long? {
    if (url.isBlank() || !URLUtil.isValidUrl(url)) return null
    val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"

    val request = DownloadManager.Request(Uri.parse(url)).apply {
      setMimeType(resolvedMime)
      addRequestHeader("User-Agent", userAgent)
      setDescription(url)
      setTitle(guessFileName(url, contentDisposition, resolvedMime))
      setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOWNLOADS,
        guessFileName(url, contentDisposition, resolvedMime)
      )
    }

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
      ?: return null
    return try {
      manager.enqueue(request)
    } catch (e: Exception) {
      null
    }
  }

  private fun guessFileName(url: String, contentDisposition: String?, mimeType: String): String {
    val fromHeader = parseContentDisposition(contentDisposition)
    if (!fromHeader.isNullOrBlank()) return sanitize(fromHeader)
    val fromUrl = url.substringBefore('?').substringAfterLast('/')
    if (fromUrl.isNotBlank() && fromUrl.contains('.')) return sanitize(fromUrl)
    return "download_${System.currentTimeMillis()}.bin"
  }

  private fun parseContentDisposition(contentDisposition: String?): String? {
    if (contentDisposition.isNullOrBlank()) return null
    val match = Regex("(?i)\\bfilename\\*?=(?:UTF-8''|\")?([^\";]+)").find(contentDisposition)
    val raw = match?.groupValues?.getOrNull(1)?.trim()?.removeSurrounding("\"") ?: return null
    return try {
      URLDecoder.decode(raw, "UTF-8")
    } catch (e: UnsupportedEncodingException) {
      raw
    }
  }

  private fun sanitize(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
}
