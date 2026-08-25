package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMessage(
    val id: Int = 0,
    @SerialName("sender_id") val senderId: Int = 0,
    @SerialName("receiver_id") val receiverId: Int = 0,
    @SerialName("sender_name") val senderName: String = "",
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_title") val productTitle: String? = null,
    @SerialName("product_image_url") val productImageUrl: String? = null,
    @SerialName("replied_to_id") val repliedToId: Int? = null,
    @SerialName("replied_text") val repliedText: String? = null,
    val text: String = "",
    @SerialName("audio_url") val audioUrl: String? = null,
    val duration: Int = 0,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    val reactions: List<ApiMessageReaction> = emptyList()
)

@Serializable
data class ApiMessageReaction(
    val emoji: String = "",
    val count: Int = 0,
    val users: List<Int> = emptyList()
)

@Serializable
data class ApiMessagesResponse(val messages: List<ApiMessage>)

@Serializable
data class ApiConversation(
    @SerialName("user_id") val userId: Int,
    @SerialName("user_name") val userName: String,
    val avatar: String? = null,
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_message") val lastMessage: String,
    @SerialName("last_message_at") val lastMessageAt: String,
    @SerialName("last_sender_id") val lastSenderId: Int,
    @SerialName("unread_count") val unreadCount: Int
)

@Serializable
data class ApiConversationsResponse(val conversations: List<ApiConversation>)

@Serializable
data class ApiSendMessageBody(
    @SerialName("receiver_id") val receiverId: Int,
    val text: String,
    @SerialName("audio_url") val audioUrl: String? = null,
    val duration: Int = 0,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_title") val productTitle: String? = null,
    @SerialName("product_image_url") val productImageUrl: String? = null,
    @SerialName("replied_to_id") val repliedToId: Int? = null
)

@Serializable
data class ApiUnreadCountResponse(
    @SerialName("unread_count") val unreadCount: Int = 0
)

@Serializable
data class ApiNotification(
    val id: Int,
    @SerialName("user_id") val userId: Int? = null,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("related_id") val relatedId: Int? = null,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiNotificationPreferences(
    @SerialName("allow_product") val allowProduct: Boolean = true,
    @SerialName("allow_order") val allowOrder: Boolean = true,
    @SerialName("allow_promo") val allowPromo: Boolean = true,
    @SerialName("allow_message") val allowMessage: Boolean = true,
    @SerialName("allow_system") val allowSystem: Boolean = true,
    @SerialName("push_enabled") val pushEnabled: Boolean = true
)

@Serializable
data class ApiNotificationPrefsResponse(
    val success: Boolean = false,
    val preferences: ApiNotificationPreferences? = null
)
