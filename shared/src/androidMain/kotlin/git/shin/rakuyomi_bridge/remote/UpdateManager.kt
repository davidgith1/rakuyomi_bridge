package git.shin.rakuyomi_bridge.remote

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import git.shin.rakuyomi_bridge.model.GitHubRelease
import git.shin.rakuyomi_bridge.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import androidx.core.net.toUri

class UpdateManager(
  private val context: Context,
  private val client: OkHttpClient,
  private val json: Json,
  private val currentVersionName: String,
  private val headless: Boolean
) {
  private val githubRepo = "tachibana-shin/rakuyomi_bridge"

  suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url("https://api.github.com/repos/$githubRepo/releases/latest")
        .header("Accept", "application/vnd.github.v3+json")
        .build()

      client.newCall(request).execute().use { res ->
        if (!res.isSuccessful) {
          return@use Result.failure(Exception("Failed to fetch release: ${res.code()}"))
        }

        val body = res.body()?.string().orEmpty()
        if (body.isEmpty()) {
          return@use Result.failure(Exception("Empty response body"))
        }

        val release = json.decodeFromString<GitHubRelease>(body)
        val latestVersion = release.tagName.removePrefix("v")

        val isNewer = isVersionNewer(latestVersion)

        val apkAsset = release.assets.find { it.name.endsWith(".apk") && (if (headless) it.name.startsWith("headless") else true) }

        Result.success(
          UpdateInfo(
            version = latestVersion,
            description = release.body,
            downloadUrl = apkAsset?.downloadUrl ?: "",
            isNewer = isNewer
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun isVersionNewer(latest: String): Boolean {
    val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
    val currentParts = currentVersionName.split(".").mapNotNull { it.toIntOrNull() }

    for (i in 0 until minOf(latestParts.size, currentParts.size)) {
      if (latestParts[i] > currentParts[i]) return true
      if (latestParts[i] < currentParts[i]) return false
    }
    return latestParts.size > currentParts.size
  }

  fun downloadAndInstall(url: String, fileName: String, title: String, description: String) {
    if (url.isEmpty()) return

    val destination = File(
      context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
      fileName
    )
    if (destination.exists()) destination.delete()

    val request = DownloadManager.Request(url.toUri())
      .setTitle(title)
      .setDescription(description)
      .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
      .setDestinationUri(Uri.fromFile(destination))
      .setAllowedOverMetered(true)
      .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = downloadManager.enqueue(request)

    val onComplete = object : BroadcastReceiver() {
      override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (id == downloadId) {
          installApk(destination)
          try {
            ctx.unregisterReceiver(this)
          } catch (_: Exception) {
          }
        }
      }
    }

    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    if (android.os.Build.VERSION.SDK_INT >= 33) { // TIRAMISU
      context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
    } else {
      context.registerReceiver(onComplete, filter)
    }
  }

  private fun installApk(file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/vnd.android.package-archive")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }
}
