package git.shin.rakuyomi_bridge.ui.screens.main

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import git.shin.rakuyomi_bridge.RakuyomiServerAdapter
import git.shin.rakuyomi_bridge.ServerStatus
import git.shin.rakuyomi_bridge.data.model.AppTheme
import git.shin.rakuyomi_bridge.data.repository.SettingsRepository
import git.shin.rakuyomi_bridge.data.repository.UpdateRepository
import git.shin.rakuyomi_bridge.service.ServerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
  private val server: RakuyomiServerAdapter,
  private val settingsRepository: SettingsRepository,
  private val updateRepository: UpdateRepository
) : ViewModel() {

  val homePath: StateFlow<String> = settingsRepository.homePathFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ""
  )

  val appTheme: StateFlow<AppTheme> = settingsRepository.themeFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = AppTheme.SYSTEM
  )

  val serverStatus: StateFlow<ServerStatus> = server.status

  val updateInfo = updateRepository.updateInfo
  val updateResult = updateRepository.lastResult

  fun refreshStatus() {
    server.queryRunning()
  }

  fun startServer(context: Context) {
    val intent = Intent(context, ServerService::class.java).apply {
      action = ServerService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
  }

  fun stopServer(context: Context) {
    context.startService(Intent(context, ServerService::class.java).apply {
      action = ServerService.ACTION_STOP
    })
  }

  fun updateHomePath(path: String) {
    viewModelScope.launch {
      settingsRepository.setHomePath(path)
    }
  }

  fun updateTheme(theme: AppTheme) {
    viewModelScope.launch {
      settingsRepository.setTheme(theme)
    }
  }

  fun checkForUpdate() {
    viewModelScope.launch {
      updateRepository.checkForUpdate()
    }
  }
}
