package git.shin.rakuyomi_bridge

data class BridgeResponse(
    val success: Boolean,
    val statusCode: Int = 0,
    val body: String = "",
    val error: String = "",
)
