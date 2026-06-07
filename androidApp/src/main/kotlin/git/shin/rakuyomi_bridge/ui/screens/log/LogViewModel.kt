package git.shin.rakuyomi_bridge.ui.screens.log

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import git.shin.rakuyomi_bridge.RakuyomiServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class ServerRequest(
  val timestamp: String,
  val method: String,
  val path: String,
  val status: Int,
  val durationMs: Long
)

@HiltViewModel
class LogViewModel @Inject constructor() : ViewModel() {
  val requests = mutableStateListOf<ServerRequest>()
  private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

  init {
    startPolling()
  }

  private fun startPolling() {
    viewModelScope.launch {
      while (isActive) {
        try {
          val rawLogs = RakuyomiServer.nativePollLogs()
          if (rawLogs != null) {
            val time = dateFormat.format(Date())
            rawLogs.split(";").forEach { rawEntry ->
              if (rawEntry.isNotEmpty()) {
                // Parse: method|path|status|duration
                val parts = rawEntry.split("|")
                if (parts.size == 4) {
                  val request = ServerRequest(
                    timestamp = time,
                    method = parts[0],
                    path = parts[1],
                    status = parts[2].toIntOrNull() ?: 0,
                    durationMs = parts[3].toLongOrNull() ?: 0L
                  )
                  requests.add(0, request)
                }
              }
            }
            if (requests.size > 200) {
              repeat(requests.size - 200) { requests.removeAt(requests.size - 1) }
            }
          }
        } catch (e: Exception) {
          // Ignore JNI errors during polling
          print(e)
        }
        delay(1000.milliseconds)
      }
    }
  }

  fun clearLogs() {
    requests.clear()
  }
}
