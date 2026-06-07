@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package git.shin.rakuyomi_bridge

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual object Platform {
    actual fun httpGet(url: String): BridgeResponse = httpRequest(url, "GET")
    actual fun httpPost(url: String, body: String): BridgeResponse = httpRequest(url, "POST", body)

    private fun httpRequest(urlString: String, method: String, body: String? = null): BridgeResponse {
        return try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(connection.outputStream).use { it.write(body) }
            }

            val statusCode = connection.responseCode
            val reader = BufferedReader(
                InputStreamReader(
                    if (statusCode in 200..299) connection.inputStream else connection.errorStream
                )
            )
            val responseBody = reader.readText()
            connection.disconnect()

            BridgeResponse(success = true, statusCode = statusCode, body = responseBody)
        } catch (e: Exception) {
            BridgeResponse(success = false, error = e.message ?: "Unknown error")
        }
    }
}
