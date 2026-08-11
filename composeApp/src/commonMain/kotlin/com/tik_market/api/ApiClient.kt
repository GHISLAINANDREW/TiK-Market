package com.tik_market.api

import com.tik_market.data.models.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// ── Request body DTOs ──────────────────────────────────────────

@Serializable
data class ApiLoginBody(
    val email: String,
    val password: String
)

@Serializable
data class ApiRegisterBody(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "buyer"
)

@Serializable
data class ApiCreateProductBody(
    @SerialName("shop_id") val shopId: Int,
    val title: String,
    val description: String,
    val price: Double,
    @SerialName("compare_price") val comparePrice: Double? = null,
    val category: String,
    val stock: Int,
    val unit: String,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("is_story") val isStory: Boolean = false
)

@Serializable
data class ApiReviewBody(
    @SerialName("product_id") val productId: Int,
    val rating: Int,
    val comment: String
)

@Serializable
data class ApiInitiatePaymentBody(
    @SerialName("order_id") val orderId: Int,
    val provider: String,
    val phone: String
)

@Serializable
data class ApiPaymentResponse(val payment: ApiPayment)

@Serializable
data class ApiCartActionBody(
    @SerialName("product_id") val productId: Int,
    val quantity: Int = 1
)

@Serializable
data class ApiCartItemBody(
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    val price: Double,
    val title: String = ""
)

@Serializable
data class ApiCreateOrderBody(
    @SerialName("shipping_address") val shippingAddress: String,
    val phone: String,
    val notes: String = "",
    @SerialName("payment_method") val paymentMethod: String = "Mobile Money",
    @SerialName("payment_type") val paymentType: String = "delivery",
    val items: List<ApiCartItemBody> = emptyList()
)

@Serializable
data class ApiReportBody(
    val type: String,
    @SerialName("target_id") val targetId: Int,
    val reason: String,
    val comment: String = ""
)

@Serializable
data class ApiCreateShopBody(
    val name: String,
    val description: String,
    val phone: String,
    val location: String,
    val category: String,
    @SerialName("image_url") val imageUrl: String? = null
)

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
data class ApiUploadBody(
    val image: String, // base64 data
    val filename: String
)

// ── Story models ───────────────────────────────────────────────

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

// ── Hero Section models ───────────────────────────────────────

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

@Serializable
data class ApiSuperAdminResponse(
    val stats: ApiSuperStats,
    val reports: List<ApiReport>,
    val config: ApiSystemConfig
)

@Serializable
data class ApiSuperStats(
    val users: List<ApiStatItem>,
    val shops: List<ApiStatItem>,
    val products: List<ApiStatItem>,
    val orders: List<ApiStatItem>,
    val revenue: List<ApiRevenueItem>
)

@Serializable
data class ApiStatItem(
    val role: String? = null,
    val status: String? = null,
    @SerialName("is_verified") val isVerified: Int? = null,
    @SerialName("is_active") val isActive: Int? = null,
    val count: Int
)

@Serializable
data class ApiRevenueItem(
    val month: String,
    val total: Double
)

@Serializable
data class ApiReport(
    val id: Int,
    @SerialName("reporter_id") val reporterId: Int,
    @SerialName("reporter_name") val reporterName: String,
    val type: String,
    @SerialName("target_id") val targetId: Int,
    val reason: String,
    val comment: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ApiSystemConfig(
    @SerialName("maintenance_mode") val maintenanceMode: Boolean,
    @SerialName("app_version") val appVersion: String,
    @SerialName("min_version") val minVersion: String,
    @SerialName("commission_rate") val commissionRate: Double
)

// ── Result Wrapper ─────────────────────────────────────────────

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

// ── Main API client ────────────────────────────────────────────

object ApiClient {

    // ── Routes ──
    private object Endpoints {
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

    private val json = Json {
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
        "https://dschang-market.onrender.com",
        "https://dschang-market-proxy.gtankou.workers.dev"
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

    private suspend fun request(
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

    private fun buildUrl(path: String, params: Map<String, Any?> = emptyMap()): String {
        val query = params.filterValues { it != null }
            .map { (k, v) -> "$k=${encodeUri(v.toString())}" }
            .joinToString("&")
        return if (query.isNotEmpty()) "$path?$query" else path
    }

    private suspend inline fun <reified T> safeRequest(
        method: String,
        path: String,
        body: String? = null
    ): T {
        val resp = request(method, path, body)
        return json.decodeFromString<T>(resp)
    }

    private suspend fun get(path: String): String = request("GET", path)
    private suspend fun post(path: String, body: String): String = request("POST", path, body)
    private suspend fun put(path: String, body: String): String = request("PUT", path, body)
    private suspend fun delete(path: String): String = request("DELETE", path)

    // ── Auth ──

    suspend fun login(email: String, password: String): ApiAuthResponse {
        val body = json.encodeToString(ApiLoginBody(email, password))
        val result = safeRequest<ApiAuthResponse>("POST", Endpoints.LOGIN, body)
        sessionToken = result.token
        sessionUser = result.user
        TokenStorage.save(result.token)
        return result
    }

    suspend fun googleLogin(idToken: String, location: String = ""): ApiAuthResponse {
        val body = buildJsonObject { 
            put("id_token", idToken) 
            if (location.isNotBlank()) put("location", location)
        }.toString()
        val result = safeRequest<ApiAuthResponse>("POST", Endpoints.GOOGLE_LOGIN, body)
        sessionToken = result.token
        sessionUser = result.user
        TokenStorage.save(result.token)
        return result
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String = "buyer"
    ): ApiAuthResponse {
        val body = json.encodeToString(ApiRegisterBody(name, email, phone, password, role))
        val result = safeRequest<ApiAuthResponse>("POST", Endpoints.REGISTER, body)
        sessionToken = result.token
        sessionUser = result.user
        TokenStorage.save(result.token)
        return result
    }



    suspend fun fetchMe(): ApiUser {
        val user = safeRequest<ApiUserResponse>("GET", Endpoints.ME).user
        sessionUser = user
        return user
    }

    // ── Products ──

    suspend fun fetchProducts(
        category: String? = null,
        search: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        shopId: Int? = null,
        sortBy: String? = null,
        location: String? = null,
        includeInactive: Boolean = false
    ): List<ApiProduct> {
        val path = buildUrl(Endpoints.PRODUCTS, mapOf(
            "category" to category,
            "search" to search,
            "min_price" to minPrice,
            "max_price" to maxPrice,
            "shop_id" to shopId,
            "sort_by" to sortBy,
            "location" to location,
            "include_inactive" to if (includeInactive) 1 else null
        ))
        return safeRequest<ApiProductsResponse>("GET", path).products
    }

    suspend fun fetchProduct(id: Int): ApiProduct {
        return safeRequest<ApiProduct>("GET", "${Endpoints.PRODUCTS}?id=$id")
    }

    suspend fun submitReview(productId: Int, rating: Int, comment: String, imageUrl: String = "") {
        val body = buildJsonObject {
            put("product_id", productId)
            put("rating", rating)
            put("comment", comment)
            if (imageUrl.isNotBlank()) put("image_url", imageUrl)
        }.toString()
        post(Endpoints.REVIEWS, body)
    }

    suspend fun fetchProductReviews(productId: Int): ApiReviewResponse? {
        return try {
            safeRequest<ApiReviewResponse>("GET", "${Endpoints.REVIEWS}?product_id=$productId")
        } catch (_: Exception) { null }
    }

    suspend fun markReviewUseful(reviewId: Int) {
        try { post("${Endpoints.REVIEWS}?useful=$reviewId", "") } catch (_: Exception) { }
    }

    suspend fun replyToReview(reviewId: Int, reply: String) {
        try {
            val body = buildJsonObject { put("reply", reply) }.toString()
            put("${Endpoints.REVIEWS}?reply=$reviewId", body)
        } catch (_: Exception) { }
    }

    suspend fun createProduct(
        shopId: Int,
        title: String,
        description: String,
        price: Double,
        comparePrice: Double?,
        category: String,
        stock: Int,
        unit: String,
        imageUrl: String,
        isStory: Boolean = false
    ): ApiProduct {
        val body = json.encodeToString(
            ApiCreateProductBody(shopId, title, description, price, comparePrice, category, stock, unit, imageUrl, isStory)
        )
        return safeRequest<ApiProduct>("POST", Endpoints.PRODUCTS, body)
    }

    // ── Categories ──

    suspend fun fetchCategories(): List<String> {
        // Mocking as the backend doesn't have a dedicated endpoint yet
        return listOf(
            "Alimentation", "Mode", "Électronique", "Artisanat",
            "Boutique", "Services", "Agriculture", "Autres"
        )
    }

    // ── Wishlist / Favoris ──

    suspend fun fetchWishlist(): List<ApiWishlistItem> {
        return safeRequest<ApiWishlistResponse>("GET", Endpoints.WISHLIST).items
    }

    suspend fun addToWishlist(productId: Int) {
        post(Endpoints.WISHLIST, """{"product_id":$productId}""")
    }

    suspend fun removeFromWishlist(productId: Int) {
        delete("${Endpoints.WISHLIST}?product_id=$productId")
    }

    // ── Favorite Shops ──

    suspend fun fetchFavoriteShops(): List<ApiFavoriteShop> {
        return try {
            safeRequest<ApiFavoriteShopsResponse>("GET", Endpoints.FAVORITE_SHOPS).favorites
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addFavoriteShop(shopId: Int) {
        post("${Endpoints.FAVORITE_SHOPS}?shop_id=$shopId", "")
    }

    suspend fun removeFavoriteShop(shopId: Int) {
        delete("${Endpoints.FAVORITE_SHOPS}?shop_id=$shopId")
    }

    // ── Promotions / Promo Codes ──

    suspend fun validatePromoCode(code: String, amount: Double): ApiPromoValidationResponse {
        return try {
            val path = buildUrl(Endpoints.PROMOTIONS, mapOf("code" to code, "amount" to amount))
            safeRequest<ApiPromoValidationResponse>("GET", path)
        } catch (_: Exception) {
            ApiPromoValidationResponse(valid = false, error = "Erreur de validation")
        }
    }

    suspend fun fetchShopPromotions(shopId: Int): List<ApiPromotion> {
        return try {
            safeRequest<ApiPromotionsResponse>("GET", "${Endpoints.PROMOTIONS}?shop_id=$shopId").promotions
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createPromotion(body: ApiPromoCreateBody) {
        post(Endpoints.PROMOTIONS, json.encodeToString(body))
    }

    suspend fun deletePromotion(id: Int) {
        delete("${Endpoints.PROMOTIONS}?id=$id")
    }

    // ── Cart ──

    suspend fun fetchCart(): List<ApiCartItem> {
        return safeRequest("GET", Endpoints.CART)
    }

    suspend fun addToCart(productId: Int, quantity: Int = 1) {
        val body = json.encodeToString(ApiCartActionBody(productId, quantity))
        post(Endpoints.CART, body)
    }

    suspend fun updateCart(productId: Int, quantity: Int) {
        val body = json.encodeToString(ApiCartActionBody(productId, quantity))
        put(Endpoints.CART, body)
    }

    suspend fun removeFromCart(productId: Int) {
        delete("${Endpoints.CART}?product_id=$productId")
    }

    // ── Orders ──

    suspend fun fetchOrders(): List<ApiOrder> {
        val resp = safeRequest<ApiOrdersResponse>("GET", Endpoints.ORDERS)
        return resp.orders
    }

    suspend fun createOrder(
        shippingAddress: String,
        phone: String,
        notes: String? = null,
        paymentMethod: String = "Mobile Money",
        paymentType: String = "delivery",
        items: List<ApiCartItemBody> = emptyList()
    ): ApiOrder {
        val body = json.encodeToString(ApiCreateOrderBody(shippingAddress, phone, notes ?: "", paymentMethod, paymentType, items))
        return safeRequest<ApiOrderResponse>("POST", Endpoints.ORDERS, body).order
    }

    suspend fun deleteOrder(orderId: Int) {
        delete("${Endpoints.ORDERS}?id=$orderId")
    }

    // ── Payments ──

    suspend fun initiatePayment(orderId: Int, provider: String, phone: String): ApiPayment {
        val body = json.encodeToString(ApiInitiatePaymentBody(orderId, provider, phone))
        return safeRequest<ApiPaymentResponse>("POST", Endpoints.PAYMENTS, body).payment
    }

    suspend fun getPaymentStatus(orderId: Int): ApiPayment? {
        return try {
            safeRequest<ApiPayment>("GET", "${Endpoints.PAYMENTS}?order_id=$orderId")
        } catch (_: Exception) { null }
    }

    // ── Reports ──

    suspend fun submitReport(type: String, targetId: Int, reason: String, comment: String = "") {
        val body = json.encodeToString(ApiReportBody(type, targetId, reason, comment))
        post(Endpoints.REPORTS, body)
    }

    // ── Shops ──

    suspend fun fetchAllShops(location: String? = null): List<ApiShop> {
        return try {
            val path = buildUrl(Endpoints.SHOPS, mapOf("location" to location))
            safeRequest<List<ApiShop>>("GET", path)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchShopByVendor(): ApiShop? {
        val user = sessionUser ?: return null
        return try {
            safeRequest<ApiShopResponse>("GET", "${Endpoints.SHOPS}?vendor_id=${user.id}").shop
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createShop(
        name: String,
        description: String,
        phone: String,
        location: String,
        category: String,
        imageUrl: String? = null
    ): ApiShop {
        val body = json.encodeToString(ApiCreateShopBody(name, description, phone, location, category, imageUrl))
        return safeRequest<ApiShopResponse>("POST", Endpoints.SHOPS, body).shop
    }

    suspend fun fetchShopById(shopId: Int): ApiShop? {
        return try {
            safeRequest<ApiShopResponse>("GET", "${Endpoints.SHOPS}?id=$shopId").shop
        } catch (_: Exception) {
            null
        }
    }

    // ── User Management ──

    suspend fun updateUserAvatar(imageUrl: String) {
        updateUserProfile(avatar = imageUrl)
    }

    suspend fun updateUserProfile(
        name: String? = null,
        phone: String? = null,
        location: String? = null,
        avatar: String? = null,
        coverPhoto: String? = null,
        password: String? = null
    ): ApiUser {
        val body = buildJsonObject {
            name?.let { put("name", it) }
            phone?.let { put("phone", it) }
            location?.let { put("location", it) }
            avatar?.let { put("avatar", it) }
            coverPhoto?.let { put("cover_photo", it) }
            password?.let { put("password", it) }
        }.toString()
        val result = safeRequest<ApiUserResponse>("PUT", Endpoints.ME, body).user
        sessionUser = result
        return result
    }

    // ── Messages ──

    suspend fun fetchConversations(): List<ApiConversation> {
        return safeRequest<ApiConversationsResponse>("GET", Endpoints.MESSAGES).conversations
    }

    suspend fun fetchMessages(contactId: Int, limit: Int = 200, sinceId: Int = 0): List<ApiMessage> {
        return safeRequest<ApiMessagesResponse>("GET", "${Endpoints.MESSAGES}?conversation_with=$contactId&limit=$limit" + if (sinceId > 0) "&since_id=$sinceId" else "").messages
    }

    suspend fun sendMessage(
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
        return safeRequest<ApiMessage>("POST", Endpoints.MESSAGES, body)
    }

    suspend fun deleteMessage(messageId: Int) {
        delete("${Endpoints.MESSAGES}?id=$messageId")
    }

    suspend fun deleteConversation(contactId: Int) {
        delete("${Endpoints.MESSAGES}?delete_conversation=1&contact_id=$contactId")
    }

    suspend fun addReaction(messageId: Int, emoji: String): Boolean {
        return try {
            val resp = post(Endpoints.MESSAGES + "?react=1", """{"message_id":$messageId,"emoji":"$emoji"}""")
            json.decodeFromString<ApiSuccessResponse>(resp).success
        } catch (_: Exception) { false }
    }

    suspend fun removeReaction(messageId: Int, emoji: String) {
        delete("${Endpoints.MESSAGES}?react=1&message_id=$messageId&emoji=$emoji")
    }

    suspend fun searchMessages(contactId: Int, query: String): List<ApiMessage> {
        return safeRequest<ApiMessagesResponse>("GET", "${Endpoints.MESSAGES}?conversation_with=$contactId&search=$query").messages
    }

    suspend fun markMessagesAsRead(contactId: Int) {
        try {
            put("${Endpoints.MESSAGES}?read_contact_id=$contactId", "")
        } catch (_: Exception) {}
    }

    suspend fun fetchUnreadCount(): Int {
        return try {
            val resp = safeRequest<ApiUnreadCountResponse>("GET", Endpoints.UNREAD_COUNT)
            resp.unreadCount
        } catch (_: Exception) {
            0
        }
    }

    // ── Image upload ──

    suspend fun uploadImage(dataUrl: String, fileName: String): String {
        val base64Data = dataUrl.substringAfter(",", dataUrl)
        val body = json.encodeToString(ApiUploadBody(base64Data, fileName))
        return safeRequest<ApiUploadResponse>("POST", Endpoints.UPLOADS, body).imageUrl
    }

    // ── Stories ─────────────────────────────────────────────────

    suspend fun fetchStories(replies: Boolean = false): List<ApiStory> {
        val path = buildUrl(Endpoints.STORIES, mapOf("replies" to if (replies) "1" else "0"))
        return safeRequest<ApiStoriesResponse>("GET", path).stories
    }

    suspend fun fetchShopStories(shopId: Int, replies: Boolean = false): List<ApiStory> {
        val path = buildUrl(Endpoints.STORIES, mapOf(
            "shop_id" to shopId.toString(),
            "replies" to if (replies) "1" else "0"
        ))
        return safeRequest<ApiStoriesResponse>("GET", path).stories
    }

    suspend fun fetchStoryById(storyId: Int): ApiStory {
        return safeRequest("GET", "${Endpoints.STORIES}?id=$storyId")
    }

    suspend fun createStory(shopId: Int, mediaUrl: String, mediaType: String = "image", caption: String? = null, duration: Int = 0): ApiStory {
        val body = json.encodeToString(ApiCreateStoryBody(shopId, mediaUrl, mediaType, caption, duration))
        return safeRequest("POST", Endpoints.STORIES, body)
    }



    suspend fun replyToStory(storyId: Int, text: String): ApiStoryReplyResponse {
        val body = json.encodeToString(ApiStoryReplyBody(text))
        return safeRequest("POST", "${Endpoints.STORIES}?reply=$storyId", body)
    }

    suspend fun deleteStory(storyId: Int) {
        delete("${Endpoints.STORIES}?id=$storyId")
    }

    // ── Hero Section ──────────────────────────────────────────

    suspend fun fetchHeroItems(): List<ApiHeroItem> {
        return try {
            safeRequest<List<ApiHeroItem>>("GET", Endpoints.HERO)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createHeroItem(body: ApiCreateHeroBody): ApiHeroItem {
        return safeRequest("POST", Endpoints.HERO, json.encodeToString(body))
    }

    suspend fun deleteHeroItem(id: Int) {
        delete("${Endpoints.HERO}?id=$id")
    }

    // ── Vendor Orders ──

    suspend fun fetchVendorOrders(): List<ApiOrder> {
        val resp = safeRequest<ApiOrdersResponse>("GET", Endpoints.ORDERS_VENDOR)
        return resp.orders
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        put("${Endpoints.ORDERS_VENDOR}?id=$orderId&status=$status", "")
    }

    suspend fun confirmOrderReceived(orderId: Int) {
        put("${Endpoints.ORDERS_VENDOR}?id=$orderId&action=confirm_received", "")
    }

    // ── Shop Management ──

    suspend fun fetchVendorStats(): ApiVendorStatsResponse {
        return try {
            safeRequest("GET", Endpoints.VENDOR_STATS)
        } catch (_: Exception) {
            ApiVendorStatsResponse()
        }
    }

    suspend fun updateShop(
        shopId: Int,
        name: String? = null,
        description: String? = null,
        phone: String? = null,
        location: String? = null,
        category: String? = null,
        imageUrl: String? = null
    ) {
        val body = buildJsonObject {
            name?.let { put("name", it) }
            description?.let { put("description", it) }
            phone?.let { put("phone", it) }
            location?.let { put("location", it) }
            category?.let { put("category", it) }
            imageUrl?.let { put("image_url", it) }
        }.toString()
        
        if (body == "{}") return
        put("${Endpoints.SHOPS}?id=$shopId", body)
    }

    suspend fun updateProduct(
        productId: Int,
        title: String? = null,
        description: String? = null,
        price: Double? = null,
        comparePrice: Double? = null,
        category: String? = null,
        stock: Int? = null,
        unit: String? = null,
        imageUrl: String? = null,
        isStory: Boolean? = null
    ) {
        val body = buildJsonObject {
            title?.let { put("title", it) }
            description?.let { put("description", it) }
            price?.let { put("price", it) }
            comparePrice?.let { put("compare_price", it) }
            category?.let { put("category", it) }
            stock?.let { put("stock", it) }
            unit?.let { put("unit", it) }
            imageUrl?.let { put("image_url", it) }
            isStory?.let { put("is_story", it) }
        }.toString()

        if (body == "{}") return
        put("${Endpoints.PRODUCTS}?id=$productId", body)
    }

    suspend fun deleteProduct(productId: Int) {
        delete("${Endpoints.PRODUCTS}?id=$productId")
    }

    // ── Admin ──

    suspend fun fetchOnlineUsers(): ApiOnlineUsersResponse {
        return safeRequest<ApiOnlineUsersResponse>("GET", "/admin/online.php")
    }

    suspend fun fetchAdminUsers(): List<ApiAdminUser> {
        return safeRequest<ApiAdminUsersResponse>("GET", Endpoints.ADMIN_USERS).users
    }

    suspend fun fetchAdminShops(): List<ApiAdminShop> {
        return try {
            safeRequest<ApiAdminShopsResponse>("GET", Endpoints.ADMIN_SHOPS).shops
        } catch (e: Exception) {
            try {
                safeRequest<List<ApiAdminShop>>("GET", Endpoints.ADMIN_SHOPS)
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun updateUserRole(userId: Int, role: String, managedCity: String? = null) {
        val path = buildUrl(Endpoints.ADMIN_USERS, mapOf("id" to userId, "role" to role, "managed_city" to managedCity))
        put(path, "")
    }

    suspend fun addUser(name: String, email: String, phone: String, password: String, role: String = "buyer", managedCity: String? = null) {
        val body = buildJsonObject {
            put("name", name)
            put("email", email)
            put("phone", phone)
            put("password", password)
            put("role", role)
            managedCity?.let { put("managed_city", it) }
        }.toString()
        post(Endpoints.ADMIN_USERS, body)
    }

    suspend fun deleteUser(userId: Int) {
        delete("${Endpoints.ADMIN_USERS}?id=$userId")
    }

    suspend fun banUser(userId: Int, status: String = "banned") {
        put("${Endpoints.ADMIN_USERS}?id=$userId&status=$status", "")
    }

    suspend fun toggleShopVerification(shopId: Int, verified: Boolean) {
        put("${Endpoints.ADMIN_SHOPS}?id=$shopId&verified=$verified", "")
    }

    suspend fun deleteShop(shopId: Int) {
        delete("${Endpoints.ADMIN_SHOPS}?id=$shopId")
    }

    suspend fun banShop(shopId: Int, status: String = "banned") {
        put("${Endpoints.ADMIN_SHOPS}?id=$shopId&status=$status", "")
    }

    suspend fun promoteShop(shopId: Int, featured: Boolean = true) {
        put("${Endpoints.ADMIN_SHOPS}?id=$shopId&featured=$featured", "")
    }

    suspend fun sendSystemNotification(title: String, message: String) {
        val body = buildJsonObject {
            put("title", title)
            put("message", message)
        }.toString()
        post(Endpoints.NOTIFICATIONS, body)
    }

    suspend fun sendIndividualNotification(userId: Int, title: String, message: String): Boolean {
        val body = buildJsonObject {
            put("user_id", userId)
            put("title", title)
            put("message", message)
        }.toString()
        val respStr = post(Endpoints.NOTIFICATIONS, body)
        val resp = json.parseToJsonElement(respStr).jsonObject
        return resp["success"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    // ── Admin Dashboard Analytics ──

    suspend fun fetchAdminDashboard(city: String? = null): ApiAdminDashboardResponse {
        val path = buildUrl("/admin/dashboard.php", mapOf("city" to city))
        return safeRequest<ApiAdminDashboardResponse>("GET", path)
    }

    // ── Super Admin ──

    suspend fun fetchSuperAdminData(): ApiSuperAdminResponse {
        return safeRequest("GET", Endpoints.SUPER_ADMIN)
    }

    suspend fun updateReportStatus(reportId: Int, status: String): Boolean {
        val body = buildJsonObject {
            put("action", "update_report_status")
            put("report_id", reportId)
            put("status", status)
        }.toString()
        val resp = post(Endpoints.SUPER_ADMIN, body)
        return json.parseToJsonElement(resp).jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    suspend fun broadcastSystemMessage(title: String, message: String): Boolean {
        val body = buildJsonObject {
            put("action", "broadcast_system")
            put("title", title)
            put("message", message)
        }.toString()
        val resp = post(Endpoints.SUPER_ADMIN, body)
        return json.parseToJsonElement(resp).jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
    }

    // ── Group Buying ──

    suspend fun createGroupBuy(productId: Int, minQty: Int = 5, discountPct: Double = 5.0, hours: Int = 48): ApiGroupBuyCreateResponse {
        val body = buildJsonObject {
            put("product_id", productId)
            put("min_quantity", minQty)
            put("discount_pct", discountPct)
            put("expires_in_hours", hours)
        }.toString()
        return safeRequest<ApiGroupBuyCreateResponse>("POST", "/group-buy/create.php", body)
    }

    suspend fun joinGroupBuy(groupBuyId: Int, quantity: Int = 1): ApiGroupBuyJoinResponse {
        val body = buildJsonObject {
            put("group_buy_id", groupBuyId)
            put("quantity", quantity)
        }.toString()
        return safeRequest<ApiGroupBuyJoinResponse>("POST", "/group-buy/join.php", body)
    }

    suspend fun cancelGroupBuy(groupBuyId: Int): ApiSuccessResponse {
        val body = buildJsonObject {
            put("group_buy_id", groupBuyId)
        }.toString()
        return try {
            safeRequest<ApiSuccessResponse>("POST", "/group-buy/cancel.php", body)
        } catch (e: Exception) {
            ApiSuccessResponse(success = false, error = e.message)
        }
    }

    suspend fun deleteGroupBuy(groupBuyId: Int): ApiSuccessResponse {
        val body = buildJsonObject {
            put("group_buy_id", groupBuyId)
        }.toString()
        return try {
            safeRequest<ApiSuccessResponse>("POST", "/group-buy/delete.php", body)
        } catch (e: Exception) {
            ApiSuccessResponse(success = false, error = e.message)
        }
    }

    suspend fun notifyGroupParticipants(groupBuyId: Int, title: String, message: String): ApiSuccessResponse {
        val body = buildJsonObject {
            put("group_buy_id", groupBuyId)
            put("title", title)
            put("message", message)
        }.toString()
        return try {
            safeRequest<ApiSuccessResponse>("POST", "/group-buy/notify.php", body)
        } catch (e: Exception) {
            ApiSuccessResponse(success = false, error = e.message)
        }
    }

    suspend fun fetchGroupBuys(productId: Int): List<ApiGroupBuy> {
        return try {
            val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?product_id=$productId")
            resp.groupBuys
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchMyGroupBuys(): List<ApiGroupBuy> {
        return try {
            val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?my=1")
            resp.groupBuys
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchShopGroupBuys(shopId: Int): List<ApiGroupBuy> {
        return try {
            val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?shop_id=$shopId")
            resp.groupBuys
        } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchGroupBuyDetails(groupBuyId: Int): ApiGroupBuyDetailResponse {
        return try {
            safeRequest<ApiGroupBuyDetailResponse>("GET", "/group-buy/details.php?id=$groupBuyId")
        } catch (_: Exception) { ApiGroupBuyDetailResponse() }
    }

    // ── Vendor Interactions (likes, reviews, subscribers) ──

    suspend fun fetchVendorInteractions(productId: Int): ApiVendorInteractionsResponse {
        return try {
            safeRequest<ApiVendorInteractionsResponse>("GET", "${Endpoints.VENDOR_INTERACTIONS}?product_id=$productId")
        } catch (_: Exception) {
            ApiVendorInteractionsResponse()
        }
    }

    suspend fun fetchShopSubscribers(shopId: Int): List<ApiInteractionUser> {
        return try {
            val resp = safeRequest<ApiVendorInteractionsResponse>("GET", "${Endpoints.VENDOR_INTERACTIONS}?shop_id=$shopId")
            resp.subscribers
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Notifications ──

    suspend fun fetchNotifications(): List<ApiNotification> {
        return safeRequest("GET", Endpoints.NOTIFICATIONS)
    }

    suspend fun fetchAdminNotifications(): List<ApiNotification> {
        return safeRequest("GET", "${Endpoints.NOTIFICATIONS}?admin=1")
    }

    suspend fun markNotificationAsRead(id: Int? = null) {
        val body = if (id != null) "{\"id\":$id}" else "{}"
        put(Endpoints.NOTIFICATIONS, body)
    }

    suspend fun deleteNotification(id: Int) {
        delete("${Endpoints.NOTIFICATIONS}?id=$id")
    }

    // ── OTP / Phone Auth ──

    suspend fun sendOtp(phone: String): ApiSendOtpResponse {
        val body = json.encodeToString(ApiSendOtpBody(phone))
        return safeRequest("POST", Endpoints.OTP_SEND, body)
    }

    suspend fun verifyOtp(phone: String, code: String): ApiVerifyOtpResponse {
        val body = json.encodeToString(ApiVerifyOtpBody(phone, code))
        val result = safeRequest<ApiVerifyOtpResponse>("POST", Endpoints.OTP_VERIFY, body)
        if (result.success && result.token.isNotBlank()) {
            sessionToken = result.token
            sessionUser = result.user
            TokenStorage.save(result.token)
        }
        return result
    }

    // ── Wallet / Fidélité ──

    suspend fun fetchWallet(): ApiWallet? {
        return try {
            val ts = com.tik_market.currentTimeMillis()
            val resp = safeRequest<ApiWalletResponse>("GET", "${Endpoints.WALLET}?t=$ts")
            resp.wallet
        } catch (_: Exception) { null }
    }

    suspend fun fetchWalletTransactions(): List<ApiWalletTransaction> {
        return try {
            val resp = safeRequest<ApiWalletTransactionsResponse>("GET", Endpoints.WALLET_TRANSACTIONS)
            resp.transactions
        } catch (_: Exception) { emptyList() }
    }

    suspend fun earnPoints(amount: Double, orderId: Int): ApiEarnResponse {
        val body = buildJsonObject {
            put("amount", amount)
            put("order_id", orderId)
        }.toString()
        return safeRequest("POST", Endpoints.WALLET_EARN, body)
    }

    suspend fun redeemPoints(points: Int): ApiRedeemResponse {
        val body = buildJsonObject {
            put("points", points)
        }.toString()
        return safeRequest("POST", Endpoints.WALLET_REDEEM, body)
    }

    suspend fun rechargeWallet(amount: Double, method: String = "other"): ApiRechargeResponse {
        val body = buildJsonObject {
            put("amount", amount)
            put("method", method)
        }.toString()
        return safeRequest("POST", Endpoints.WALLET_RECHARGE, body)
    }

    suspend fun fetchCoupons(): List<ApiCoupon> {
        return try {
            val resp = safeRequest<ApiCouponsResponse>("GET", Endpoints.COUPONS)
            resp.coupons
        } catch (_: Exception) { emptyList() }
    }

    suspend fun useCoupon(code: String): ApiCoupon? {
        return try {
            val body = buildJsonObject { put("code", code) }.toString()
            val resp = safeRequest<ApiCouponsResponse>("POST", Endpoints.COUPONS_USE, body)
            resp.coupons.firstOrNull()
        } catch (_: Exception) { null }
    }

    // ── Notifications Push ──

    suspend fun fetchNotificationPrefs(): ApiNotificationPreferences? {
        return try {
            val resp = safeRequest<ApiNotificationPrefsResponse>("GET", Endpoints.NOTIF_PREFS)
            resp.preferences
        } catch (_: Exception) { null }
    }

    suspend fun updateNotificationPrefs(prefs: ApiNotificationPreferences): Boolean {
        return try {
            val body = json.encodeToString(prefs)
            val resp = safeRequest<ApiNotificationPrefsResponse>("PUT", Endpoints.NOTIF_PREFS, body)
            resp.success
        } catch (_: Exception) { false }
    }

    suspend fun registerDeviceToken(token: String, platform: String = "web"): Boolean {
        return try {
            val body = buildJsonObject {
                put("token", token)
                put("platform", platform)
            }.toString()
            val resp = safeRequest<ApiSuccessResponse>("POST", Endpoints.NOTIF_TOKENS, body)
            resp.success
        } catch (_: Exception) { false }
    }

    suspend fun unregisterDeviceToken(token: String): Boolean {
        return try {
            val body = buildJsonObject {
                put("token", token)
            }.toString()
            val resp = safeRequest<ApiSuccessResponse>("DELETE", Endpoints.NOTIF_TOKENS, body)
            resp.success
        } catch (_: Exception) { false }
    }

    // ── Helper ──

    private fun encodeUri(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~' -> sb.append(c)
                c == ' ' -> sb.append("%20")
                else -> {
                    sb.append("%")
                    sb.append(c.code.toString(16).uppercase())
                }
            }
        }
        return sb.toString()
    }
}
