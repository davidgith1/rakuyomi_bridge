package git.shin.rakuyomi_bridge

import kotlinx.coroutines.delay

class BridgeClient(private val config: ServerConfig = ServerConfig()) {

    suspend fun waitForReady(timeoutSeconds: Int = 10): Boolean {
        val deadline = System.currentTimeMillis() + timeoutSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            if (healthCheck()) return true
            delay(500)
        }
        return healthCheck()
    }

    fun healthCheck(): Boolean {
        return try {
            val response = httpGet(config.healthCheckUrl)
            response.success
        } catch (_: Exception) {
            false
        }
    }

    fun httpGet(url: String): BridgeResponse = Platform.httpGet(url)
    fun httpPost(url: String, body: String): BridgeResponse = Platform.httpPost(url, body)
}

expect object Platform {
    fun httpGet(url: String): BridgeResponse
    fun httpPost(url: String, body: String): BridgeResponse
}
