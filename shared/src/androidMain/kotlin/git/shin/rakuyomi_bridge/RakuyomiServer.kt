package git.shin.rakuyomi_bridge

/**
 * Low-level JNI bindings into librakuyomi_server.so.
 *
 * The native library is compiled from backend/server (Rust) with
 * crate-type = ["cdylib"] and must be placed under
 * androidApp/src/main/jniLibs/<abi>/librakuyomi_server.so.
 *
 * JNI function names (automatically derived from package + class + method):
 *   Java_git_shin_rakuyomi_1bridge_JniBridge_nativeStart
 *   Java_git_shin_rakuyomi_1bridge_JniBridge_nativeStop
 *   Java_git_shin_rakuyomi_1bridge_JniBridge_nativeIsRunning
 */
public object RakuyomiServer {
    /** Status: server started successfully. */
    const val OK = 0
    /** Status: a server instance is already running. */
    const val ALREADY_RUNNING = 1
    /** Status: home path argument is invalid. */
    const val INVALID_ARGUMENT = 2
    /** Status: the tokio runtime could not be initialised. */
    const val RUNTIME_INIT_FAILED = 3
    /** Status: no server is currently running (stop/isRunning). */
    const val NOT_RUNNING = 5
    /** Status: internal locking error. */
    const val INTERNAL_ERROR = 100

    init {
        System.loadLibrary("rakuyomi_server")
    }

    /**
     * Start the rakuyomi HTTP server on a background thread.
     *
     * @param homePath  Absolute path to the rakuyomi data directory.
     *                  Must be readable and writable.
     * @param port      TCP port to bind on 127.0.0.1.
     * @return          [OK] on success, or one of the error codes.
     */
    external fun nativeStart(homePath: String, port: Int): Int

    /**
     * Gracefully stop the running server. Safe to call even when
     * no server is running.
     * @return [OK] if the server was stopped, [NOT_RUNNING] otherwise.
     */
    external fun nativeStop(): Int

    /**
     * Check whether a server is currently running in this process.
     * @return 1 if running, 0 if not, -1 on internal error.
     */
    external fun nativeIsRunning(): Int

    /**
     * Poll for any new log entries from the server.
     * @return A semicolon-separated string of log entries, or null if none.
     */
    external fun nativePollLogs(): String?

    /**
     * Send the response for a network request back to Rust.
     * @param requestId The ID of the request being responded to.
     * @param statusCode The HTTP status code.
     * @param headers JSON string of headers.
     * @param body The response body bytes.
     */
    external fun nativeSendNetworkResponse(requestId: Long, statusCode: Int, headers: String, body: ByteArray?)

    /**
     * Report a network request error back to Rust.
     * @param requestId The ID of the request.
     * @param error Error message.
     */
    external fun nativeSendNetworkError(requestId: Long, error: String)

    // --- Callbacks from Rust ---

    private var networkHandler: ((Long, String) -> Unit)? = null

    fun setNetworkHandler(handler: (Long, String) -> Unit) {
        networkHandler = handler
    }

    @JvmStatic
    fun onNetworkRequest(id: Long, json: String) {
        networkHandler?.invoke(id, json)
    }
}
