package git.shin.rakuyomi_bridge.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import git.shin.rakuyomi_bridge.R
import git.shin.rakuyomi_bridge.data.model.AppTheme
import git.shin.rakuyomi_bridge.data.repository.UpdateRepository
import git.shin.rakuyomi_bridge.ui.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: HomeViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val homePath by viewModel.homePath.collectAsState()
  val appTheme by viewModel.appTheme.collectAsState()
  val updateResult by viewModel.updateResult.collectAsState()
  var showThemeMenu by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  var isChecking by remember { mutableStateOf(false) }

  val folderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri: Uri? ->
    uri?.let {
      val path = getAbsolutePathFromUri(it)
      if (path != null) {
        viewModel.updateHomePath(path)
      }
    }
  }

  LaunchedEffect(updateResult) {
    isChecking = false
    when (val result = updateResult) {
      is UpdateRepository.UpdateResult.UpToDate -> {
        snackbarHostState.showSnackbar(context.getString(R.string.update_no_update))
      }

      is UpdateRepository.UpdateResult.Failed -> {
        snackbarHostState.showSnackbar(
          context.getString(R.string.update_check_failed, result.message)
        )
      }

      is UpdateRepository.UpdateResult.UpdateAvailable -> {
        // The auto-show happens in MainActivity.
      }

      UpdateRepository.UpdateResult.Idle -> Unit
    }
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.settings_title)) }
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(24.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Appearance Section
      Text(
        text = stringResource(R.string.theme_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Box {
          Row(
            modifier = Modifier
              .clickable { showThemeMenu = true }
              .padding(16.dp)
              .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = when (appTheme) {
                  AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                  AppTheme.LIGHT -> stringResource(R.string.theme_light)
                  AppTheme.DARK -> stringResource(R.string.theme_dark)
                },
                style = MaterialTheme.typography.bodyMedium
              )
            }

            Icon(Icons.Default.Palette, contentDescription = null)
          }

          DropdownMenu(
            expanded = showThemeMenu,
            onDismissRequest = { showThemeMenu = false }
          ) {
            AppTheme.entries.forEach { theme ->
              DropdownMenuItem(
                text = {
                  Text(
                    when (theme) {
                      AppTheme.SYSTEM -> stringResource(R.string.theme_system)
                      AppTheme.LIGHT -> stringResource(R.string.theme_light)
                      AppTheme.DARK -> stringResource(R.string.theme_dark)
                    }
                  )
                },
                onClick = {
                  viewModel.updateTheme(theme)
                  showThemeMenu = false
                }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Updates Section
      Text(
        text = stringResource(R.string.update_section_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = !isChecking) {
            isChecking = true
            viewModel.checkForUpdate()
          },
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Row(
          modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.check_for_updates),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(
                R.string.update_current_version,
                git.shin.rakuyomi_bridge.BuildConfig.VERSION_NAME
              ),
              style = MaterialTheme.typography.bodyMedium
            )
          }

          if (isChecking) {
            CircularProgressIndicator(
              modifier = Modifier
                .height(24.dp),
              strokeWidth = 2.dp
            )
          } else {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Data Directory Section
      Text(
        text = stringResource(R.string.data_directory),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.align(Alignment.Start)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
      ) {
        Row(
          modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.current_path),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = homePath.ifEmpty { stringResource(R.string.not_set) },
              style = MaterialTheme.typography.bodyMedium
            )
          }

          IconButton(onClick = { folderLauncher.launch(null) }) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.select_folder))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.data_dir_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.weight(1f))

      if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
        Button(
          onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
              data = "package:${context.packageName}".toUri()
            }
            context.startActivity(intent)
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(stringResource(R.string.grant_all_files))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.all_files_required),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

private fun getAbsolutePathFromUri(uri: Uri): String? {
  val docId = DocumentsContract.getTreeDocumentId(uri)
  val split = docId.split(":")
  val type = split[0]

  return if ("primary".equals(type, ignoreCase = true)) {
    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
  } else {
    null
  }
}
