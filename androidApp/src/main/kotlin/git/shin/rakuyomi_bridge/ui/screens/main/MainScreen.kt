package git.shin.rakuyomi_bridge.ui.screens.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import git.shin.rakuyomi_bridge.DEFAULT_SERVER_PORT
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.ServerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: MainViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val serverStatus by viewModel.serverStatus.collectAsState()

  val statusText = when (serverStatus) {
    ServerStatus.RUNNING -> stringResource(R.string.status_running)
    ServerStatus.STOPPED -> stringResource(R.string.status_stopped)
    ServerStatus.STARTING -> stringResource(R.string.status_starting)
    ServerStatus.ERROR -> stringResource(R.string.status_unknown)
  }

  val statusColor = when (serverStatus) {
    ServerStatus.RUNNING -> MaterialTheme.colorScheme.primary
    ServerStatus.STARTING -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.error
  }

  var showStorageDialog by remember { mutableStateOf(false) }
  var showNotificationDialog by remember { mutableStateOf(false) }

  // -- Permission launchers --

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      if (hasStoragePermission()) {
        viewModel.startServer(context)
      } else {
        showStorageDialog = true
      }
    } else {
      Toast.makeText(
        context,
        context.getString(R.string.notification_denied),
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  val storageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { /* result checked in onResume */ }

  // -- Permission logic --

  fun onPermissionsChecked() {
    if (hasNotificationPermission(context) && hasStoragePermission()) {
      viewModel.startServer(context)
    }
  }

  // -- Lifecycle observer --

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        onPermissionsChecked()
        viewModel.refreshStatus()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  // -- UI --

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = statusText,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = statusColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = context.getString(R.string.port_format, DEFAULT_SERVER_PORT),
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    val isRunning = serverStatus == ServerStatus.RUNNING
    val isStarting = serverStatus == ServerStatus.STARTING

    Button(
      onClick = {
        if (isRunning) {
          viewModel.stopServer(context)
        } else {
          if (!hasNotificationPermission(context)) {
            showNotificationDialog = true
          } else if (!hasStoragePermission()) {
            showStorageDialog = true
          } else {
            viewModel.startServer(context)
          }
        }
      },
      enabled = !isStarting,
      modifier = Modifier.fillMaxWidth(),
      colors = if (isRunning) {
        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
      } else {
        ButtonDefaults.buttonColors()
      }
    ) {
      if (isStarting) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp
        )
      } else {
        Text(
          text = if (isRunning) stringResource(R.string.stop_server)
          else stringResource(R.string.start_server)
        )
      }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Text(
      text = context.getString(R.string.info_text),
      fontSize = 14.sp,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }

  // -- Dialogs --

  if (showStorageDialog) {
    AlertDialog(
      onDismissRequest = { showStorageDialog = false },
      title = { Text(context.getString(R.string.storage_title)) },
      text = { Text(context.getString(R.string.storage_message)) },
      confirmButton = {
        TextButton(onClick = {
          showStorageDialog = false
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storageLauncher.launch(
              Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
              }
            )
          }
        }) {
          Text(context.getString(R.string.storage_go_to_settings))
        }
      },
      dismissButton = {
        TextButton(onClick = { showStorageDialog = false }) {
          Text(context.getString(R.string.cancel))
        }
      }
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
          if (Build.VERSION.SDK_INT >= 33) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }) {
          Text(context.getString(R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { showNotificationDialog = false }) {
          Text(context.getString(R.string.cancel))
        }
      }
    )
  }
}

private fun hasNotificationPermission(context: Context): Boolean {
  if (Build.VERSION.SDK_INT < 33) return true
  return ContextCompat.checkSelfPermission(
    context, Manifest.permission.POST_NOTIFICATIONS
  ) == PackageManager.PERMISSION_GRANTED
}

private fun hasStoragePermission(): Boolean {
  if (Build.VERSION.SDK_INT < 30) return true
  return Environment.isExternalStorageManager()
}
