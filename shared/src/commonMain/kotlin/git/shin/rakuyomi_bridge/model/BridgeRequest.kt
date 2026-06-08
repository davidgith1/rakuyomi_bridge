package git.shin.rakuyomi_bridge.model

import kotlinx.serialization.Serializable

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
    if (other == null || this::class != other::class) return false

    other as BridgeRequest

    if (id != other.id) return false
    if (url != other.url) return false
    if (method != other.method) return false
    if (headers != other.headers) return false
    if (body != null) {
      if (other.body == null) return false
      if (!body.contentEquals(other.body)) return false
    } else if (other.body != null) return false

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
