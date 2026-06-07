package git.shin.rakuyomi_bridge

/**
 * Default TCP port the server listens on.
 * Must match the value in backend/server/src/listener.rs (DEFAULT_TCP_PORT).
 */
const val DEFAULT_SERVER_PORT = 8787

/**
 * Address the server binds to — only reachable from the local device.
 */
const val SERVER_HOST = "127.0.0.1"

/**
 * Configuration shared across common and platform-specific code.
 */
data class ServerConfig(
    val homePath: String,
    val port: Int = DEFAULT_SERVER_PORT,
    val host: String = SERVER_HOST,
) {
    val healthCheckUrl: String get() = "http://$host:$port/health-check"

    companion object {
        /**
         * Default folder name for rakuyomi data.
         */
        const val DATA_FOLDER_NAME = "rakuyomi"
    }
}
