package git.shin.rakuyomi_bridge.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.security.MessageDigest
import kotlin.system.exitProcess

object AppIntegrityChecker {

  fun checkIntegrity(context: Context, expectedSha256: String, isDebug: Boolean) {
    if (isDebug) return

    if (expectedSha256.isEmpty()) {
      return
    }

    val actualSha256 = getSignatureSha256(context)

    if (expectedSha256.lowercase() != actualSha256?.lowercase()) {
      Log.e(
        "IntegrityChecker",
        "App integrity check failed. Expected: $expectedSha256, Actual: $actualSha256"
      )
      exitProcess(0)
    }
  }

  private fun getSignatureSha256(context: Context): String? {
    return try {
      val pm = context.packageManager
      val packageName = context.packageName
      val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        packageInfo.signingInfo?.apkContentsSigners
      } else {
        @Suppress("DEPRECATION")
        val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        packageInfo.signatures
      }

      if (signatures.isNullOrEmpty()) return null

      val md = MessageDigest.getInstance("SHA-256")
      val digest = md.digest(signatures[0].toByteArray())
      digest.joinToString(":") { "%02X".format(it) }
    } catch (e: Exception) {
      Log.e("IntegrityChecker", "Failed to read signature", e)
      null
    }
  }
}
