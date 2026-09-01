package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiReel(
    val id: Int,
    @SerialName("shop_id") val shopId: Int,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_logo") val shopLogo: String? = null,
    @SerialName("video_url") val videoUrl: String,
    val description: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiReelsResponse(
    val reels: List<ApiReel>
)

@Serializable
data class ApiCreateReelBody(
    @SerialName("shop_id") val shopId: Int,
    @SerialName("video_url") val videoUrl: String,
    val description: String = "",
    @SerialName("product_id") val productId: Int? = null
)

@Serializable
data class ApiCreateReelResponse(
    val success: Boolean,
    val message: String = ""
)
