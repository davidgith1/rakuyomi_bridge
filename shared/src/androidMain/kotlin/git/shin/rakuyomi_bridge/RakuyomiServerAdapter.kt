package git.shin.rakuyomi_bridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Server status for UI and service tracking.
 */
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

/**
 * Public API for starting / stopping the rakuyomi server on Android.
 *
 * Usage (from a coroutine scope):
 * ```
 * val server = RakuyomiServer(ServerConfig(homePath = "/path/to/data"))
 * server.start()                     // blocking call on the current thread
 * // or from a coroutine:
 * launch(Dispatchers.IO) { server.start() }
 * ```
 */
class RakuyomiServerAdapter(private val config: ServerConfig) {

    private val _status = MutableStateFlow(ServerStatus.STOPPED)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    /** True while the server is believed to be alive. */
    val isRunning: Boolean get() = _status.value == ServerStatus.RUNNING

    /**
     * Start the server, blocking until it is ready or fails.
     *
     * @param homePath Custom path for the server to use. If null, use the path from config.
     * @throws RakuyomiServerException on failure.
     */
    fun start(homePath: String? = null) {
        if (_status.value == ServerStatus.RUNNING || _status.value == ServerStatus.STARTING) {
            return
        }

        _status.value = ServerStatus.STARTING

        val finalHomePath = homePath ?: config.homePath
        try {
            val rc = RakuyomiServer.nativeStart(finalHomePath, config.port)
            when (rc) {
                RakuyomiServer.OK -> {
                    _status.value = ServerStatus.RUNNING
                }
                RakuyomiServer.ALREADY_RUNNING -> {
                    _status.value = ServerStatus.RUNNING
                }
                else -> {
                    _status.value = ServerStatus.ERROR
                    throw RakuyomiServerException(
                        "Failed to start server (code=$rc). " +
                                "Check that the companion app has storage permissions."
                    )
                }
            }
        } catch (e: Exception) {
            _status.value = ServerStatus.ERROR
            if (e is RakuyomiServerException) throw e
            throw RakuyomiServerException("Native start failed: ${e.message}")
        }
    }

    /**
     * Stop the server gracefully. Safe to call even when not running.
     *
     * @throws RakuyomiServerException when the native call fails.
     */
    fun stop() {
        if (_status.value == ServerStatus.STOPPED) return

        val rc = RakuyomiServer.nativeStop()
        when (rc) {
            RakuyomiServer.OK, RakuyomiServer.NOT_RUNNING -> {
                _status.value = ServerStatus.STOPPED
            }
            else -> {
                _status.value = ServerStatus.ERROR
                throw RakuyomiServerException("Failed to stop server (code=$rc)")
            }
        }
    }

    /**
     * Check whether the native library has a running server.
     */
    fun queryRunning(): Boolean {
        val rc = RakuyomiServer.nativeIsRunning()
        val running = rc == 1
        if (_status.value != ServerStatus.STARTING) {
            _status.value = if (running) ServerStatus.RUNNING else ServerStatus.STOPPED
        }
        return running
    }
}

class RakuyomiServerException(message: String) : Exception(message)
