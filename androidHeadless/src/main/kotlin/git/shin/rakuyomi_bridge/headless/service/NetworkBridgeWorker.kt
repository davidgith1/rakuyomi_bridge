package git.shin.rakuyomi_bridge.headless.service

import android.util.Log
import git.shin.rakuyomi_bridge.RakuyomiServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

@Serializable
data class BridgeRequest(
  val id: Long,
  val url: String,
  val method: String,
  val headers: Map<String, String>,
  val body: ByteArray? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as BridgeRequest

    if (id != other.id) return false
    if (url != other.url) return false
    if (method != other.method) return false
    if (headers != other.headers) return false
    if (!body.contentEquals(other.body)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + method.hashCode()
    result = 31 * result + headers.hashCode()
    result = 31 * result + (body?.contentHashCode() ?: 0)
    return result
  }
}

class NetworkBridgeWorker(private val client: OkHttpClient) {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val json = Json { ignoreUnknownKeys = true }

  fun start() {
    RakuyomiServer.setNetworkHandler { id, rawJson ->
      Log.d(TAG, "Received network request $id")
      scope.launch {
        try {
          val bridgeRequest = json.decodeFromString<BridgeRequest>(rawJson)
          Log.d(TAG, "Executing request $id: ${bridgeRequest.method} ${bridgeRequest.url}")
          executeRequest(bridgeRequest)
        } catch (e: Exception) {
          Log.e(TAG, "Error processing request $id", e)
          RakuyomiServer.nativeSendNetworkError(id, "Parse error: ${e.message}")
        }
      }
    }
  }

  fun stop() {
    RakuyomiServer.setNetworkHandler { _, _ -> }
  }

  private fun executeRequest(bridgeReq: BridgeRequest) {
    val body: RequestBody? = bridgeReq.body?.let { bytes ->
      RequestBody.create(MediaType.parse("application/octet-stream"), bytes)
    }

    val requestBuilder = Request.Builder()
      .url(bridgeReq.url)
      .method(bridgeReq.method, body)

    bridgeReq.headers.forEach { (k, v) ->
      requestBuilder.addHeader(k, v)
    }

    val request = requestBuilder.build()

    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        RakuyomiServer.nativeSendNetworkError(bridgeReq.id, e.message ?: "Unknown error")
      }

      override fun onResponse(call: Call, response: Response) {
        // OkHttp 3.x exposes `code`/`body`/`headers` as both a
        // package-private field and a public method. Kotlin's
        // property syntax picks the field and errors out, so we
        // call the getters explicitly.
        val status = response.code()
        val bodyBytes = response.body()?.bytes()

        val headers = response.headers()
        val headersMap = mutableMapOf<String, String>()
        for (i in 0 until headers.size()) {
          headersMap[headers.name(i)] = headers.value(i)
        }

        val headersJson = json.encodeToString(
          MapSerializer(String.serializer(), String.serializer()),
          headersMap
        )

        RakuyomiServer.nativeSendNetworkResponse(
          bridgeReq.id,
          status,
          headersJson,
          bodyBytes
        )
      }
    })
  }

  companion object {
    private const val TAG = "HeadlessNetBridge"
  }
}
