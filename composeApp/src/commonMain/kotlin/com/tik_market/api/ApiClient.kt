package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


// ── Result Wrapper ─────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

// ── Main API client ────────────────────────────────────────────

object ApiClient {

    // ── Routes ──
    internal object Endpoints {
        const val LOGIN = "/auth/login.php"
        const val GOOGLE_LOGIN = "/auth/google_login.php"
        const val REGISTER = "/auth/register.php"
        const val ME = "/auth/me.php"
        const val OTP_SEND = "/auth/send-otp.php"
        const val OTP_VERIFY = "/auth/verify-otp.php"
        const val PRODUCTS = "/products/products.php"
        const val SHOPS = "/shops/shops.php"
        const val CART = "/cart/cart.php"
        const val ORDERS = "/orders/orders.php"
        const val ORDERS_VENDOR = "/orders/vendor.php"
        const val MESSAGES = "/messages/messages.php"
        const val UNREAD_COUNT = "/messages/unread_count.php"
        const val REVIEWS = "/reviews/reviews.php"
        const val WISHLIST = "/wishlist/wishlist.php"
        const val FAVORITE_SHOPS = "/favorites/shops.php"
        const val PROMOTIONS = "/promotions/promotions.php"
        const val PAYMENTS = "/payments/payments.php"
        const val REPORTS = "/reports/reports.php"
        const val UPLOADS = "/uploads/upload.php"
        const val VENDOR_STATS = "/vendor/stats.php"
        const val VENDOR_INTERACTIONS = "/vendor/interactions.php"
        const val NOTIFICATIONS = "/notifications/notifications.php"
        const val NOTIF_PREFS = "/notifications/prefs.php"
        const val NOTIF_TOKENS = "/notifications/tokens.php"
        const val WALLET = "/wallet/get.php"
        const val WALLET_TRANSACTIONS = "/wallet/transactions.php"
        const val WALLET_EARN = "/wallet/earn.php"
        const val WALLET_REDEEM = "/wallet/redeem.php"
        const val WALLET_RECHARGE = "/wallet/recharge.php"
        const val COUPONS = "/coupons/list.php"
        const val COUPONS_USE = "/coupons/use.php"
        const val STORIES = "/stories/stories.php"
        const val HERO = "/admin/hero.php"
        const val ADMIN_USERS = "/admin/users.php"
        const val ADMIN_SHOPS = "/admin/shops.php"
        const val SUPER_ADMIN = "/admin/super.php"
    }

    private var sessionToken: String? = null
    private var sessionUser: ApiUser? = null
    var isLoggingEnabled: Boolean = true

    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        coerceInputValues = true
    }

    /**
     * API endpoints, in priority order.
     * Le premier domaine (Render) peut être bloqué par certains opérateurs mobiles
     * (ex: Orange Cameroun bloque onrender.com). En cas d'échec réseau, on tente
     * automatiquement les URLs de secours (domaine personnalisé / proxy Cloudflare).
     */
    private var _baseUrls: List<String> = listOf(
        "https://tik-market.onrender.com",
        "https://tik-market-proxy.gtankou.workers.dev"
    )
    private var _activeBaseUrlIndex = 0

    var baseUrl: String
        get() = _baseUrls[_activeBaseUrlIndex]
        set(value) {
            _baseUrls = listOf(value.trimEnd('/'))
            _activeBaseUrlIndex = 0
        }

    /** Liste complète des endpoints utilisables (utile pour le diagnostic). */
    fun getBaseUrls(): List<String> = _baseUrls.toList()

    // ── Session Helpers ──
    
    fun initToken() {
        val saved = TokenStorage.load()
        if (saved != null) sessionToken = saved
    }

    fun setToken(t: String?) {
        sessionToken = t
        if (t != null) TokenStorage.save(t) else TokenStorage.clear()
    }
    
    fun getToken(): String? = sessionToken
    fun setCurrentUser(u: ApiUser?) { sessionUser = u }
    fun getCurrentUser(): ApiUser? = sessionUser
    fun getCurrentUserId(): Int = sessionUser?.id ?: 0
    fun isLoggedIn(): Boolean = sessionToken != null
    fun isVendor(): Boolean = sessionUser?.role == "vendor"
    fun isAdmin(): Boolean = sessionUser?.role == "admin" || sessionUser?.role == "super_admin"
    fun isSuperAdmin(): Boolean = sessionUser?.role == "super_admin"

    fun logout() {
        sessionToken = null
        sessionUser = null
        TokenStorage.clear()
    }

    // ── Private HTTP helpers ──

    private fun buildHeaders(): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "X-Platform" to "KMP"
            // Le header bypass-tunnel-reminder n'est plus nécessaire avec Cloudflare
        )
        sessionToken?.let { headers["Authorization"] = "Bearer $it" }
        return headers
    }

    internal suspend fun request(
        method: String,
        path: String,
        body: String? = null
    ): String {
        // Les URLs en dur (uploads directs, etc.) ne passent pas par le fallback.
        if (path.startsWith("http")) {
            val directUrl = path
            if (isLoggingEnabled) println("[API] $method $directUrl")
            return try {
                val response = HttpEngine.request(method, directUrl, buildHeaders(), body)
                if (isLoggingEnabled) println("[API] Response: ${response.take(200)}...")
                response
            } catch (e: Exception) {
                if (isLoggingEnabled) println("[API] Error: ${e.message}")
                throw e
            }
        }

        // Fallback multi-URL : si le domaine actif échoue (timeout/blocage réseau),
        // on tente les suivants et on mémorise celui qui fonctionne.
        var lastError: Exception? = null
        val startIndex = _activeBaseUrlIndex
        for (offset in 0 until _baseUrls.size) {
            val index = (startIndex + offset) % _baseUrls.size
            val url = "${_baseUrls[index]}$path"
            if (isLoggingEnabled) println("[API] $method $url" + (body?.let { " | Body: $it" } ?: ""))
            try {
                val response = HttpEngine.request(method, url, buildHeaders(), body)
                if (isLoggingEnabled) println("[API] Success on ${_baseUrls[index]}")
                _activeBaseUrlIndex = index
                return response
            } catch (e: Exception) {
                if (isLoggingEnabled) println("[API] Error on ${_baseUrls[index]}: ${e.message}")
                lastError = e
            }
        }
        throw lastError ?: Exception("Network error")
    }

    internal fun buildUrl(path: String, params: Map<String, Any?> = emptyMap()): String {
        val query = params.filterValues { it != null }
            .map { (k, v) -> "$k=${encodeUri(v.toString())}" }
            .joinToString("&")
        return if (query.isNotEmpty()) "$path?$query" else path
    }

    internal suspend inline fun <reified T> safeRequest(
        method: String,
        path: String,
        body: String? = null
    ): T {
        val resp = request(method, path, body)
        return json.decodeFromString<T>(resp)
    }

    internal suspend fun get(path: String): String = request("GET", path)
    internal suspend fun post(path: String, body: String): String = request("POST", path, body)
    internal suspend fun put(path: String, body: String): String = request("PUT", path, body)
    internal suspend fun delete(path: String): String = request("DELETE", path)

    // ── Helper ──

    internal fun encodeUri(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~' -> sb.append(c)
                c == ' ' -> sb.append("%20")
                else -> {
                    sb.append("%")
                    val hex = c.code.toString(16).uppercase()
                    if (hex.length < 2) sb.append('0')
                    sb.append(hex)
                }
            }
        }
        return sb.toString()
    }
}
