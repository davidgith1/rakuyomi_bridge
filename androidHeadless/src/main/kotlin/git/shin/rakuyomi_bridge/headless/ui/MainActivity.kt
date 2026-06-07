package git.shin.rakuyomi_bridge.headless.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import git.shin.rakuyomi_bridge.ServerStatus
import git.shin.rakuyomi_bridge.headless.HeadlessApp
import git.shin.rakuyomi_bridge.headless.R
import git.shin.rakuyomi_bridge.headless.service.ServerService
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

/**
 * Minimal control screen for the headless build.
 *
 * No XML layout, no Compose, no Hilt — just a [LinearLayout] built in code so
 * the APK stays small and works on Android 4.3 (API 18). It exposes:
 *  - the current server status (polled every second),
 *  - Start / Stop buttons (delegating to [ServerService]),
 *  - permission grant buttons for Storage (API 30+) and Notifications (API 33+).
 */
class MainActivity : Activity() {

  private val app: HeadlessApp get() = HeadlessApp.from(this)

  private lateinit var statusText: TextView
  private lateinit var startStopButton: Button
  private lateinit var storageButton: Button
  private lateinit var notificationButton: Button

  private val handler = Handler(Looper.getMainLooper())
  private val pollStatus = object : Runnable {
    override fun run() {
      refreshUi()
      handler.postDelayed(this, STATUS_POLL_INTERVAL_MS)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(buildUi())
  }

  override fun onResume() {
    super.onResume()
    refreshUi()
    handler.post(pollStatus)
  }

  override fun onPause() {
    handler.removeCallbacks(pollStatus)
    super.onPause()
  }

  private fun buildUi(): View {
    val padding = dp(20)
    val gap = dp(12)

    val root = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.WHITE)
      setPadding(padding, padding, padding, padding)
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }

    root.addView(makeTitle())
    root.addView(makeVersion())
    root.addView(spacer(gap))

    statusText = TextView(this).apply {
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
      setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    root.addView(statusText)
    root.addView(spacer(gap))

    val portText = TextView(this).apply {
      text = getString(R.string.port_format, git.shin.rakuyomi_bridge.DEFAULT_SERVER_PORT)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setTextColor(Color.DKGRAY)
    }
    root.addView(portText)
    root.addView(spacer(gap * 2))

    startStopButton = Button(this).apply {
      setOnClickListener { onStartStopClicked() }
    }
    root.addView(startStopButton)
    root.addView(spacer(gap))

    storageButton = Button(this).apply {
      text = getString(R.string.grant_storage)
      setOnClickListener { openAllFilesAccessSettings() }
    }
    root.addView(storageButton)
    root.addView(spacer(gap))

    notificationButton = Button(this).apply {
      text = getString(R.string.grant_notification)
      setOnClickListener { requestNotificationPermission() }
    }
    root.addView(notificationButton)
    root.addView(spacer(gap * 2))

    root.addView(makeInfoText())
    return root
  }

  private fun makeTitle(): TextView = TextView(this).apply {
    text = getString(R.string.app_name)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
    setTypeface(typeface, android.graphics.Typeface.BOLD)
    gravity = Gravity.CENTER_HORIZONTAL
  }

  private fun makeVersion(): TextView = TextView(this).apply {
    val pkg = packageManager.getPackageInfo(packageName, 0)
    text = getString(R.string.version_format, pkg.versionName)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    setTextColor(Color.GRAY)
    gravity = Gravity.CENTER_HORIZONTAL
  }

  private fun makeInfoText(): TextView = TextView(this).apply {
    text = getString(R.string.info_text)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    setTextColor(Color.DKGRAY)
    gravity = Gravity.CENTER_HORIZONTAL
  }

  private fun spacer(height: Int): View = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      height
    )
  }

  private fun dp(value: Int): Int =
    (value * resources.displayMetrics.density).toInt()

  private fun refreshUi() {
    app.server.queryRunning()
    val status = app.server.status.value
    statusText.text = when (status) {
      ServerStatus.RUNNING -> getString(R.string.status_running)
      ServerStatus.STARTING -> getString(R.string.status_starting)
      ServerStatus.STOPPED -> getString(R.string.status_stopped)
      ServerStatus.ERROR -> getString(R.string.status_unknown)
    }
    statusText.setTextColor(
      when (status) {
        ServerStatus.RUNNING -> "#2E7D32".toColorInt()
        ServerStatus.STARTING -> "#1565C0".toColorInt()
        else -> "#C62828".toColorInt()
      }
    )
    startStopButton.text = getString(
      if (status == ServerStatus.RUNNING) R.string.stop_server else R.string.start_server
    )
    startStopButton.isEnabled = status != ServerStatus.STARTING

    storageButton.visibility = if (needsStoragePermission()) View.VISIBLE else View.GONE
    notificationButton.visibility = if (needsNotificationPermission()) View.VISIBLE else View.GONE
  }

  private fun onStartStopClicked() {
    val status = app.server.status.value
    if (status == ServerStatus.RUNNING) {
      stopService()
    } else {
      startService()
    }
  }

  private fun startService() {
    val intent = Intent(this, ServerService::class.java).apply {
      action = ServerService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ContextCompat.startForegroundService(this, intent)
    } else {
      startService(intent)
    }
  }

  private fun stopService() {
    val intent = Intent(this, ServerService::class.java).apply {
      action = ServerService.ACTION_STOP
    }
    startService(intent)
  }

  private fun openAllFilesAccessSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    try {
      val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = "package:$packageName".toUri()
      }
      startActivity(intent)
    } catch (e: Exception) {
      print(e)
      // Some OEM ROMs hide the per-app action; fall back to the generic
      // manage-storage page so the user can still navigate manually.
      try {
        startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
      } catch (e2: Exception) {
        print(e2)
        Toast.makeText(
          this,
          getString(R.string.storage_settings_unavailable),
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQ_NOTIFICATION) {
      val granted = grantResults.isNotEmpty() &&
        grantResults[0] == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        Toast.makeText(
          this,
          getString(R.string.notification_denied),
          Toast.LENGTH_SHORT
        ).show()
      }
    }
  }

  private fun needsStoragePermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    return !Environment.isExternalStorageManager()
  }

  private fun needsNotificationPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ContextCompat.checkSelfPermission(
      this, Manifest.permission.POST_NOTIFICATIONS
    ) != PackageManager.PERMISSION_GRANTED
  }

  companion object {
    private const val STATUS_POLL_INTERVAL_MS = 1000L
    private const val REQ_NOTIFICATION = 0x4211
  }
}
