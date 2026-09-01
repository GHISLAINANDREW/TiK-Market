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
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiLiveStreamsResponse(
    val streams: List<ApiLiveStream>
)
