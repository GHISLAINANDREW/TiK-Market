package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiLiveStream(
    val id: Int,
    @SerialName("shop_id") val shopId: Int,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_logo") val shopLogo: String? = null,
    val title: String,
    @SerialName("stream_url") val streamUrl: String,
    @SerialName("viewer_count") val viewerCount: Int = 0,
    @SerialName("is_live") val isLive: Boolean = true,
    @SerialName("pinned_product_id") val pinnedProductId: Int? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiLiveComment(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int,
    @SerialName("user_name") val userName: String,
    val text: String,
    @SerialName("created_at") val createdAt: String = "",
    val likes_count: Int = 0,
    @SerialName("liked_by_me") val liked_by_me: Boolean = false,
    val city: String = "",
    val country: String = ""
) {
    val likedByMe: Boolean get() = liked_by_me
}

@Serializable
data class ApiLiveStreamsResponse(
    val streams: List<ApiLiveStream>
)

@Serializable
data class ApiStartLiveBody(
    val title: String,
    @SerialName("pinned_product_id") val pinnedProductId: Int? = null
)

@Serializable
data class ApiStartLiveResponse(
    val success: Boolean,
    val message: String = "",
    @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("stream_url") val streamUrl: String = ""
)

@Serializable
data class ApiLiveFrameResponse(
    val success: Boolean = false,
    val frame: String? = null,
    @SerialName("frame_at") val frameAt: String = ""
)

@Serializable
data class ApiLiveAudioChunk(
    val seq: Int = 0,
    val audio: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiLiveAudioResponse(
    val success: Boolean = false,
    val chunks: List<ApiLiveAudioChunk> = emptyList()
)

@Serializable
data class ApiLiveKitTokenResponse(
    val success: Boolean = false,
    val token: String = "",
    val url: String = "",
    val room: String = "",
    val identity: String = ""
)

@Serializable
data class ApiLiveKitTokenBody(
    val room: String,
    val identity: String? = null
)
