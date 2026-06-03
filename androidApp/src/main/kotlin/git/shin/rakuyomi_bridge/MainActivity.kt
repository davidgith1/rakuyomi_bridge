package git.shin.rakuyomi_bridge

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RakuyomiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf(context.getString(R.string.status_stopped)) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // ── Permission launchers ──────────────────────────────────────

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                onPermissionsChecked()
            } else {
                Toast.makeText(context, context.getString(R.string.notification_denied), Toast.LENGTH_SHORT).show()
            }
        }
    )

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* result checked in onResume */ }

    // ── Permission logic ───────────────────────────────────────────

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < 30) return true
        return Environment.isExternalStorageManager()
    }

    fun requestAllPermissions(): Boolean {
        if (!hasNotificationPermission()) {
            showNotificationDialog = true
            return false
        }
        if (!hasStoragePermission()) {
            showStorageDialog = true
            return false
        }
        return true
    }

    fun onPermissionsChecked() {
        if (requestAllPermissions()) {
            doStartServer()
        }
    }

    fun doStartServer() {
        context.startService(Intent(context, ServerService::class.java).apply {
            action = ServerService.ACTION_START
        })
        statusText = context.getString(R.string.status_starting)
    }

    fun doStopServer() {
        context.startService(Intent(context, ServerService::class.java).apply {
            action = ServerService.ACTION_STOP
        })
        statusText = context.getString(R.string.status_stopped)
    }

    fun refreshStatus() {
        statusText = try {
            val running = RakuyomiServer().queryRunning()
            if (running) context.getString(R.string.status_running)
            else context.getString(R.string.status_stopped)
        } catch (_: Exception) {
            context.getString(R.string.status_unknown)
        }
    }

    // ── Lifecycle observer (onResume) ──────────────────────────────

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onPermissionsChecked()
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── UI ──────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = context.getString(R.string.title_rakuyomi_bridge),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = context.getString(R.string.port_format, DEFAULT_SERVER_PORT),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!requestAllPermissions()) return@Button
                doStartServer()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = context.getString(R.string.start_server))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { doStopServer() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = context.getString(R.string.stop_server))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = context.getString(R.string.info_text),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // ── Dialogs ─────────────────────────────────────────────────────

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text(context.getString(R.string.storage_title)) },
            text = { Text(context.getString(R.string.storage_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showStorageDialog = false
                    storageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                    )
                }) {
                    Text(context.getString(R.string.storage_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text(context.getString(R.string.notification_title)) },
            text = { Text(context.getString(R.string.notification_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDialog = false
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}
