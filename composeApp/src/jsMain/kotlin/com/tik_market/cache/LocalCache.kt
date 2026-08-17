package com.tik_market.cache

import kotlinx.browser.localStorage
import kotlin.js.Date

private val now: Long get() = Date.now().toLong()

/**
 * Local cache for web (JS target).
 * Uses localStorage for simple key-value storage.
 * Relies on the service worker for media (images, audio) caching.
 */
object LocalCache {

    private const val PREFIX = "dc_"

    // ── Simple key-value ──

    fun putString(key: String, value: String) {
        try {
            localStorage.setItem("$PREFIX$key", value)
        } catch (_: Exception) {}
    }

    fun getString(key: String): String? {
        return try {
            localStorage.getItem("$PREFIX$key")
        } catch (_: Exception) { null }
    }

    fun removeString(key: String) {
        try {
            localStorage.removeItem("$PREFIX$key")
        } catch (_: Exception) {}
    }

    // ── JSON cache with TTL ──

    fun putJson(key: String, json: String, ttlMinutes: Int = 10) {
        try {
            val expiryMs = now + ttlMinutes * 60_000L
            val entry = """{"data":${json},"expires":$expiryMs}"""
            localStorage.setItem("$PREFIX$key", entry)
        } catch (_: Exception) {}
    }

    fun getJson(key: String): String? {
        return try {
            val raw = localStorage.getItem("$PREFIX$key") ?: return null
            // Simple manual JSON parsing to avoid js() calls
            val dataMatch = raw.split("\"expires\":")
            if (dataMatch.size < 2) return raw // backward compat
            val expiresStr = dataMatch[1].trimEnd('}')
            val expires = expiresStr.toLongOrNull() ?: return raw
            if (now > expires) {
                localStorage.removeItem("$PREFIX$key")
                return null
            }
            // Extract data field
            val prefix = "\"data\":"
            val dataStart = raw.indexOf(prefix)
            if (dataStart < 0) return raw
            val afterData = raw.substring(dataStart + prefix.length)
            // Find the matching "," before "expires"
            val commaPos = afterData.lastIndexOf(',')
            if (commaPos < 0) return raw
            afterData.substring(0, commaPos)
        } catch (_: Exception) { null }
    }

    // ── Messages cache ──

    fun cacheMessages(conversationPartnerId: Int, messagesJson: String) {
        putJson("msgs_$conversationPartnerId", messagesJson, 30) // 30 min TTL
    }

    fun getCachedMessages(conversationPartnerId: Int): String? {
        return getJson("msgs_$conversationPartnerId")
    }

    // ── Conversations list ──

    fun cacheConversations(json: String) {
        putJson("conversations", json, 5) // 5 min TTL
    }

    fun getCachedConversations(): String? = getJson("conversations")

    // ── Unread count ──

    fun cacheUnreadCount(count: Int) {
        putString("unread", count.toString())
    }

    fun getCachedUnreadCount(): Int = getString("unread")?.toIntOrNull() ?: 0

    // ── API responses ──

    fun cacheApiResponse(endpoint: String, json: String) {
        putJson("api_${endpoint.hashCode()}", json, 10) // 10 min TTL
    }

    fun getCachedApiResponse(endpoint: String): String? {
        return getJson("api_${endpoint.hashCode()}")
    }

    // ── Media cache tracking ──

    fun markMediaCached(url: String) {
        try {
            localStorage.setItem("${PREFIX}media_${url.hashCode()}", "1")
        } catch (_: Exception) {}
    }

    fun isMediaCached(url: String): Boolean {
        return try {
            localStorage.getItem("${PREFIX}media_${url.hashCode()}") != null
        } catch (_: Exception) { false }
    }

    // ── Auth token ──

    fun cacheToken(token: String) {
        putString("token", token)
    }

    fun getCachedToken(): String? = getString("token")

    fun clearToken() {
        removeString("token")
    }
}
