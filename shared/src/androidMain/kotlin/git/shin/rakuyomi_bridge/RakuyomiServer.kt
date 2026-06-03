package git.shin.rakuyomi_bridge

import kotlinx.coroutines.*

/**
 * Public API for starting / stopping the rakuyomi server on Android.
 *
 * Usage (from a coroutine scope):
 * ```
 * val server = RakuyomiServer(ServerConfig())
 * server.start()                     // blocking call on the current thread
 * // or from a coroutine:
 * launch(Dispatchers.IO) { server.start() }
 * ```
 */
class RakuyomiServer(private val config: ServerConfig = ServerConfig()) {

    /** True while the server is believed to be alive. */
    var isRunning: Boolean = false
        private set

    /**
     * Start the server, blocking until it is ready or fails.
     *
     * This is a CPU-bound blocking call (it talks to the native bridge,
     * which spawns a server on its own thread pool).  Call from a
     * background dispatcher when on the main thread.
     *
     * @throws RakuyomiServerException on failure.
     */
    fun start() {
        if (isRunning) throw RakuyomiServerException("Server is already running")

        val rc = JniBridge.nativeStart(config.homePath, config.port)
        when (rc) {
            JniBridge.OK -> {
                isRunning = true
            }
            JniBridge.ALREADY_RUNNING -> {
                isRunning = true
            }
            else -> throw RakuyomiServerException(
                "Failed to start server (code=$rc). " +
                    "Check that the companion app has storage permissions."
            )
        }
    }

    /**
     * Stop the server gracefully. Safe to call even when not running.
     *
     * @throws RakuyomiServerException when the native call fails.
     */
    fun stop() {
        val rc = JniBridge.nativeStop()
        when (rc) {
            JniBridge.OK, JniBridge.NOT_RUNNING -> {
                isRunning = false
            }
            else -> throw RakuyomiServerException("Failed to stop server (code=$rc)")
        }
    }

    /**
     * Check whether the native library has a running server.
     */
    fun queryRunning(): Boolean {
        val rc = JniBridge.nativeIsRunning()
        isRunning = rc == 1
        return isRunning
    }
}

class RakuyomiServerException(message: String) : Exception(message)
