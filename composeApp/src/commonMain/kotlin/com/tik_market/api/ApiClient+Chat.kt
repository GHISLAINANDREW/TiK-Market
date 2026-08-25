package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// ── Messages ──

suspend fun ApiClient.fetchConversations(): List<ApiConversation> {
    return safeRequest<ApiConversationsResponse>("GET", ApiClient.Endpoints.MESSAGES).conversations
}

suspend fun ApiClient.fetchMessages(contactId: Int, limit: Int = 200, sinceId: Int = 0): List<ApiMessage> {
    return safeRequest<ApiMessagesResponse>("GET", "${ApiClient.Endpoints.MESSAGES}?conversation_with=$contactId&limit=$limit" + if (sinceId > 0) "&since_id=$sinceId" else "").messages
}

suspend fun ApiClient.sendMessage(
    receiverId: Int,
    text: String,
    audioUrl: String? = null,
    duration: Int = 0,
    productId: Int? = null,
    productTitle: String? = null,
    productImageUrl: String? = null,
    repliedToId: Int? = null
): ApiMessage {
    val body = json.encodeToString(ApiSendMessageBody(receiverId, text, audioUrl, duration, productId, productTitle, productImageUrl, repliedToId))
    return safeRequest<ApiMessage>("POST", ApiClient.Endpoints.MESSAGES, body)
}

suspend fun ApiClient.deleteMessage(messageId: Int) {
    delete("${ApiClient.Endpoints.MESSAGES}?id=$messageId")
}

suspend fun ApiClient.deleteConversation(contactId: Int) {
    delete("${ApiClient.Endpoints.MESSAGES}?delete_conversation=1&contact_id=$contactId")
}

suspend fun ApiClient.addReaction(messageId: Int, emoji: String): Boolean {
    return try {
        val resp = post(ApiClient.Endpoints.MESSAGES + "?react=1", """{"message_id":$messageId,"emoji":"$emoji"}""")
        json.decodeFromString<ApiSuccessResponse>(resp).success
    } catch (_: Exception) { false }
}

suspend fun ApiClient.removeReaction(messageId: Int, emoji: String) {
    delete("${ApiClient.Endpoints.MESSAGES}?react=1&message_id=$messageId&emoji=$emoji")
}

suspend fun ApiClient.searchMessages(contactId: Int, query: String): List<ApiMessage> {
    return safeRequest<ApiMessagesResponse>("GET", "${ApiClient.Endpoints.MESSAGES}?conversation_with=$contactId&search=$query").messages
}

suspend fun ApiClient.markMessagesAsRead(contactId: Int) {
    try {
        put("${ApiClient.Endpoints.MESSAGES}?read_contact_id=$contactId", "")
    } catch (_: Exception) {}
}

suspend fun ApiClient.fetchUnreadCount(): Int {
    return try {
        val resp = safeRequest<ApiUnreadCountResponse>("GET", ApiClient.Endpoints.UNREAD_COUNT)
        resp.unreadCount
    } catch (_: Exception) {
        0
    }
}

// ── Notifications ──

suspend fun ApiClient.fetchNotifications(): List<ApiNotification> {
    return safeRequest("GET", ApiClient.Endpoints.NOTIFICATIONS)
}

suspend fun ApiClient.fetchAdminNotifications(): List<ApiNotification> {
    return safeRequest("GET", "${ApiClient.Endpoints.NOTIFICATIONS}?admin=1")
}

suspend fun ApiClient.markNotificationAsRead(id: Int? = null) {
    val body = if (id != null) "{\"id\":$id}" else "{}"
    put(ApiClient.Endpoints.NOTIFICATIONS, body)
}

suspend fun ApiClient.deleteNotification(id: Int) {
    delete("${ApiClient.Endpoints.NOTIFICATIONS}?id=$id")
}

// ── Notifications Push ──

suspend fun ApiClient.fetchNotificationPrefs(): ApiNotificationPreferences? {
    return try {
        val resp = safeRequest<ApiNotificationPrefsResponse>("GET", ApiClient.Endpoints.NOTIF_PREFS)
        resp.preferences
    } catch (_: Exception) { null }
}

suspend fun ApiClient.updateNotificationPrefs(prefs: ApiNotificationPreferences): Boolean {
    return try {
        val body = json.encodeToString(prefs)
        val resp = safeRequest<ApiNotificationPrefsResponse>("PUT", ApiClient.Endpoints.NOTIF_PREFS, body)
        resp.success
    } catch (_: Exception) { false }
}

suspend fun ApiClient.registerDeviceToken(token: String, platform: String = "web"): Boolean {
    return try {
        val body = buildJsonObject {
            put("token", token)
            put("platform", platform)
        }.toString()
        val resp = safeRequest<ApiSuccessResponse>("POST", ApiClient.Endpoints.NOTIF_TOKENS, body)
        resp.success
    } catch (_: Exception) { false }
}

suspend fun ApiClient.unregisterDeviceToken(token: String): Boolean {
    return try {
        val body = buildJsonObject {
            put("token", token)
        }.toString()
        val resp = safeRequest<ApiSuccessResponse>("DELETE", ApiClient.Endpoints.NOTIF_TOKENS, body)
        resp.success
    } catch (_: Exception) { false }
}
