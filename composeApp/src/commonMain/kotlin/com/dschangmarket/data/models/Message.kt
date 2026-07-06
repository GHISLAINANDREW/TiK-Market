package com.dschangmarket.data.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderName: String = "",
    val productId: String? = null,
    val productTitle: String? = null,
    val text: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)
