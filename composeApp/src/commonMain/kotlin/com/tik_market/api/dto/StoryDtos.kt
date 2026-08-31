package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiStory(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("shop_id") val shopId: Int,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "image",
    val caption: String? = null,
    val duration: Int = 0,
    @SerialName("is_admin") val isAdmin: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_avatar") val userAvatar: String? = null,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("shop_logo") val shopLogo: String? = null,
    @SerialName("user_role") val userRole: String = "",
    @SerialName("user_managed_city") val userManagedCity: String? = null,
    val city: String? = null,
    val replies: List<ApiStoryReply>? = null,
    @SerialName("reply_count") val replyCount: Int = 0
)

@Serializable
data class ApiStoryReply(
    val id: Int,
    @SerialName("story_id") val storyId: Int,
    @SerialName("user_id") val userId: Int,
    val text: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("user_name") val userName: String = ""
)

@Serializable
data class ApiStoriesResponse(
    val stories: List<ApiStory>,
    @SerialName("deleted_expired") val deletedExpired: Int = 0
)

@Serializable
data class ApiCreateStoryBody(
    @SerialName("shop_id") val shopId: Int,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("media_type") val mediaType: String = "image",
    val caption: String? = null,
    val duration: Int = 0
)

@Serializable
data class ApiStoryReplyBody(
    val text: String
)

@Serializable
data class ApiStoryReplyResponse(
    val success: Boolean,
    @SerialName("reply_id") val replyId: Int = 0
)

@Serializable
data class ApiStoryDeleteResponse(
    val success: Boolean,
    val message: String = ""
)

@Serializable
data class ApiHeroItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("shop_id") val shopId: Int? = null,
    @SerialName("shop_name") val shopName: String? = null,
    val priority: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ApiCreateHeroBody(
    val title: String,
    val subtitle: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("shop_id") val shopId: Int? = null,
    val priority: Int = 0
)
