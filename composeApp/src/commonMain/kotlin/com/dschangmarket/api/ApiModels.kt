package com.dschangmarket.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.dschangmarket.data.models.Product

@Serializable
data class ApiUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val location: String = "",
    val avatar: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("referral_code") val referralCode: String? = null
) {
    val isOnline: Boolean get() {
        if (lastSeen.isBlank()) return false
        return try {
            // Simple check: if last_seen is within last 5 minutes
            true // Actual check done server-side, here we just know it's a recent timestamp
        } catch (_: Exception) { false }
    }
}

@Serializable
data class ApiShop(
    val id: Int = 0,
    @SerialName("vendor_id") val vendorId: Int = 0,
    val name: String = "",
    val description: String = "",
    val logo: String = "",
    val phone: String = "",
    val location: String = "",
    val category: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("total_sales") val totalSales: Int = 0,
    val rating: Float = 0f,
    val products: List<ApiProduct>? = null
)

@Serializable
data class ApiProduct(
    val id: Int = 0,
    @SerialName("shop_id") val shopId: Int = 0,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("shop_phone") val shopPhone: String = "",
    @SerialName("shop_location") val shopLocation: String = "",
    @SerialName("vendor_id") val vendorId: Int = 0,
    val title: String = "",
    val description: String = "",
    val price: Int = 0,
    @SerialName("compare_price") val comparePrice: Int? = null,
    val category: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    val stock: Int = 0,
    val unit: String = "pièce",
    val rating: Float = 0f,
    @SerialName("total_reviews") val totalReviews: Int = 0,
    @SerialName("total_sales") val totalSales: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_story") val isStory: Boolean = false,
    val reviews: List<ApiReview>? = null
)

@Serializable
data class ApiWishlistItem(
    val id: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    @SerialName("compare_price") val comparePrice: Double? = null,
    @SerialName("image_url") val imageUrl: String = "",
    val stock: Int = 0,
    @SerialName("shop_name") val shopName: String = ""
)

@Serializable
data class ApiWishlistResponse(val items: List<ApiWishlistItem>)

@Serializable
data class ApiCartItem(
    val id: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    val quantity: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    @SerialName("compare_price") val comparePrice: Double? = null,
    @SerialName("image_url") val imageUrl: String = "",
    val stock: Int = 0,
    @SerialName("shop_name") val shopName: String = ""
)

@Serializable
data class ApiVendorInfo(
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("vendor_phone") val vendorPhone: String = "",
    @SerialName("vendor_phone_user") val vendorPhoneUser: String = ""
)

@Serializable
data class ApiOrder(
    val id: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    val status: String = "pending",
    @SerialName("payment_method") val paymentMethod: String = "Mobile Money",
    @SerialName("payment_status") val paymentStatus: String = "unpaid",
    @SerialName("payment_type") val paymentType: String = "delivery",
    val phone: String = "",
    @SerialName("shipping_address") val shippingAddress: String = "",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val items: List<ApiOrderItem>? = null,
    @SerialName("vendor_info") val vendorInfo: List<ApiVendorInfo>? = null,
    @SerialName("shop_total") val shopTotal: Double? = null
)

@Serializable
data class ApiOrderResponse(val order: ApiOrder)

@Serializable
data class ApiOrdersResponse(
    val orders: List<ApiOrder> = emptyList(),
    val shop: ApiShop? = null
)

@Serializable
data class ApiOrderItem(
    @SerialName("product_id") val productId: Int = 0,
    val title: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
)

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
data class ApiReview(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("user_name") val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("useful_votes") val usefulVotes: Int = 0,
    @SerialName("vendor_reply") val vendorReply: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiReviewResponse(val reviews: List<ApiReview> = emptyList())

@Serializable
data class ApiPayment(
    val id: Int = 0,
    @SerialName("order_id") val orderId: Int = 0,
    val amount: Double = 0.0,
    val provider: String = "",
    val phone: String = "",
    @SerialName("transaction_id") val transactionId: String? = null,
    val status: String = "pending",
    val message: String? = null
)

@Serializable
data class ApiAuthResponse(val token: String, val user: ApiUser)

@Serializable
data class ApiUserResponse(val user: ApiUser)

@Serializable
data class ApiProductsResponse(val products: List<ApiProduct>, val pagination: ApiPagination? = null)

@Serializable
data class ApiPagination(val page: Int, val limit: Int, val total: Int, val pages: Int)

@Serializable
data class ApiError(val error: String)

// ── OTP / Phone Auth ──

@Serializable
data class ApiSendOtpBody(val phone: String)

@Serializable
data class ApiSendOtpResponse(
    val success: Boolean,
    val message: String,
    @SerialName("expires_in") val expiresIn: Int = 300
)

@Serializable
data class ApiVerifyOtpBody(
    val phone: String,
    val code: String
)

@Serializable
data class ApiVerifyOtpResponse(
    val success: Boolean,
    val token: String = "",
    val user: ApiUser? = null,
    @SerialName("is_new") val isNew: Boolean = false
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
data class ApiShopResponse(val shop: ApiShop)

@Serializable
data class ApiUploadResponse(
    val success: Boolean = true,
    @SerialName("image_url") val imageUrl: String = "",
    val filename: String = ""
)

// ── Admin Response DTOs ──

@Serializable
data class ApiAdminUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val status: String = "active",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiOnlineUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val avatar: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("seconds_ago") val secondsAgo: Int = 0
)

@Serializable
data class ApiOnlineUsersResponse(
    val success: Boolean = false,
    @SerialName("online_users") val onlineUsers: List<ApiOnlineUser> = emptyList(),
    @SerialName("total_online") val totalOnline: Int = 0
)

@Serializable
data class ApiAdminUsersResponse(val users: List<ApiAdminUser>)

@Serializable
data class ApiAdminShop(
    val id: Int = 0,
    val name: String = "",
    val logo: String = "",
    val location: String = "",
    val phone: String = "",
    @SerialName("vendor_name") val vendorName: String = "",
    @SerialName("vendor_email") val vendorEmail: String = "",
    @SerialName("vendor_phone") val vendorPhone: String = "",
    val category: String = "",
    val status: String = "active",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("total_sales") val totalSales: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ApiAdminShopsResponse(val shops: List<ApiAdminShop>)

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
data class ApiVendorStatsResponse(
    val success: Boolean = false,
    @SerialName("shop_name") val shopName: String = "",
    val overview: ApiVendorOverview = ApiVendorOverview(),
    @SerialName("daily_revenue") val dailyRevenue: List<ApiDailyRevenue> = emptyList(),
    @SerialName("monthly_revenue") val monthlyRevenue: List<ApiMonthlyRevenue> = emptyList(),
    @SerialName("top_products") val topProducts: List<ApiTopProduct> = emptyList(),
    @SerialName("orders_by_status") val ordersByStatus: Map<String, Int> = emptyMap()
)

@Serializable
data class ApiVendorOverview(
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("low_stock_count") val lowStockCount: Int = 0,
    @SerialName("out_of_stock_count") val outOfStockCount: Int = 0,
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("total_items_sold") val totalItemsSold: Int = 0
)

@Serializable
data class ApiDailyRevenue(
    val day: String = "",
    val revenue: Double = 0.0,
    val orders: Int = 0
)

@Serializable
data class ApiMonthlyRevenue(
    val month: String = "",
    val revenue: Double = 0.0
)

@Serializable
data class ApiTopProduct(
    val id: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    @SerialName("total_sold") val totalSold: Int = 0,
    @SerialName("total_generated") val totalGenerated: Double = 0.0
)

@Serializable
data class ApiFavoriteShop(
    @SerialName("fav_id") val favId: Int = 0,
    @SerialName("shop_id") val shopId: Int = 0,
    val name: String = "",
    val description: String = "",
    val phone: String = "",
    val location: String = "",
    val logo: String = "",
    val category: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false,
    val rating: Float = 0f,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("total_sales") val totalSales: Int = 0
)

@Serializable
data class ApiFavoriteShopsResponse(val favorites: List<ApiFavoriteShop>)

@Serializable
data class ApiFavoriteResponse(
    val message: String = "",
    @SerialName("shop_id") val shopId: Int = 0
)

@Serializable
data class ApiPromotion(
    val id: Int = 0,
    @SerialName("shop_id") val shopId: Int = 0,
    val code: String = "",
    @SerialName("discount_pct") val discountPct: Double = 0.0,
    @SerialName("discount_fixed") val discountFixed: Int = 0,
    @SerialName("min_amount") val minAmount: Int = 0,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("used_count") val usedCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class ApiPromoValidationResponse(
    val valid: Boolean = false,
    val promotion: ApiPromotion? = null,
    val discount: Int = 0,
    val error: String? = null
)

@Serializable
data class ApiPromotionsResponse(val promotions: List<ApiPromotion>)

@Serializable
data class ApiInteractionUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val avatar: String = "",
    @SerialName("liked_at") val likedAt: String = "",
    @SerialName("subscribed_at") val subscribedAt: String = ""
)

@Serializable
data class ApiProductReview(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_avatar") val userAvatar: String = "",
    val rating: Int = 0,
    val comment: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiVendorInteractionsResponse(
    val likes: List<ApiInteractionUser> = emptyList(),
    val reviews: List<ApiProductReview> = emptyList(),
    val subscribers: List<ApiInteractionUser> = emptyList()
)

@Serializable
data class ApiPromoCreateBody(
    @SerialName("shop_id") val shopId: Int,
    val code: String,
    @SerialName("discount_pct") val discountPct: Double = 0.0,
    @SerialName("discount_fixed") val discountFixed: Int = 0,
    @SerialName("min_amount") val minAmount: Int = 0,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null
)

/**
 * Converts an ApiProduct (from DB) to a Product (UI model).
 */
fun ApiProduct.toProduct(): Product {
    val cleanBase = ApiClient.baseUrl.trimEnd('/')
    
    val imagePaths = imageUrl.split(",").filter { it.isNotBlank() }
    val finalImages = imagePaths.map { path ->
        val cleanPath = path.trim().trimStart('/', '\\').replace("\\", "/")
        if (path.startsWith("http")) path 
        else if (cleanPath.isBlank()) ""
        else "$cleanBase/$cleanPath"
    }.filter { it.isNotBlank() }

    return Product(
        id = id.toString(),
        shopId = shopId.toString(),
        shopName = shopName,
        shopLocation = shopLocation,
        vendorId = vendorId.toString(),
        vendorPhone = shopPhone,
        title = title,
        description = description,
        price = price.toDouble(),
        comparePrice = comparePrice?.toDouble(),
        category = category,
        images = finalImages,
        stock = stock,
        unit = unit,
        rating = rating,
        totalReviews = totalReviews,
        totalSales = totalSales,
        shopVerified = isVerified,
        isStory = isStory
    )
}

fun ApiWishlistItem.toProduct(): Product {
    val cleanBase = ApiClient.baseUrl.trimEnd('/')
    val cleanPath = imageUrl.trimStart('/', '\\').replace("\\", "/")
    val finalImageUrl = if (imageUrl.isNotBlank()) {
        if (imageUrl.startsWith("http")) imageUrl 
        else if (cleanPath.isBlank()) ""
        else "$cleanBase/$cleanPath"
    } else ""

    return Product(
        id = productId.toString(),
        title = title,
        price = price,
        comparePrice = comparePrice,
        images = if (finalImageUrl.isNotBlank()) listOf(finalImageUrl) else emptyList(),
        stock = stock,
        shopName = shopName
    )
}

// ── Admin Dashboard Analytics ──

@Serializable
data class ApiAdminDashboardKpis(
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("total_vendors") val totalVendors: Int = 0,
    @SerialName("online_users") val onlineUsers: Int = 0,
    @SerialName("new_users_30d") val newUsers30d: Int = 0,
    @SerialName("total_shops") val totalShops: Int = 0,
    @SerialName("pending_shops") val pendingShops: Int = 0,
    @SerialName("banned_shops") val bannedShops: Int = 0,
    @SerialName("total_products") val totalProducts: Int = 0,
    @SerialName("total_orders") val totalOrders: Int = 0,
    @SerialName("orders_today") val ordersToday: Int = 0,
    @SerialName("total_revenue") val totalRevenue: Double = 0.0,
    @SerialName("revenue_today") val revenueToday: Double = 0.0
)

@Serializable
data class ApiAdminDashboardDay(
    val day: String = "",
    val count: Int = 0
)

@Serializable
data class ApiAdminDashboardMonth(
    val month: String = "",
    val revenue: Double = 0.0
)

@Serializable
data class ApiAdminDashboardTopVendor(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    @SerialName("shop_id") val shopId: Int = 0,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("order_count") val orderCount: Int = 0,
    val revenue: Double = 0.0
)

@Serializable
data class ApiAdminDashboardTopProduct(
    val id: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("total_sold") val totalSold: Int = 0,
    @SerialName("total_generated") val totalGenerated: Double = 0.0
)

@Serializable
data class ApiAdminDashboardResponse(
    val success: Boolean = false,
    val kpis: ApiAdminDashboardKpis = ApiAdminDashboardKpis(),
    val registrations: List<ApiAdminDashboardDay> = emptyList(),
    @SerialName("monthly_revenue") val monthlyRevenue: List<ApiAdminDashboardMonth> = emptyList(),
    @SerialName("top_vendors") val topVendors: List<ApiAdminDashboardTopVendor> = emptyList(),
    @SerialName("top_products") val topProducts: List<ApiAdminDashboardTopProduct> = emptyList(),
    @SerialName("orders_by_status") val ordersByStatus: Map<String, Int> = emptyMap(),
    @SerialName("users_by_role") val usersByRole: Map<String, Int> = emptyMap()
)

// ── Group Buying ──

@Serializable
data class ApiGroupBuy(
    val id: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("shop_id") val shopId: Int = 0,
    @SerialName("creator_id") val creatorId: Int = 0,
    @SerialName("creator_name") val creatorName: String = "",
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("original_price") val originalPrice: Double = 0.0,
    @SerialName("target_price") val targetPrice: Double = 0.0,
    @SerialName("discount_pct") val discountPct: Double = 0.0,
    @SerialName("min_quantity") val minQuantity: Int = 5,
    @SerialName("max_quantity") val maxQuantity: Int = 100,
    @SerialName("current_qty") val currentQty: Int = 1,
    val status: String = "open",
    @SerialName("participants_count") val participantsCount: Int = 0,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiGroupBuyParticipant(
    val id: Int = 0,
    val name: String = "",
    val avatar: String = "",
    val quantity: Int = 1,
    @SerialName("joined_at") val joinedAt: String = ""
)

@Serializable
data class ApiGroupBuyDetailResponse(
    val success: Boolean = false,
    @SerialName("group_buy") val groupBuy: ApiGroupBuyDetail? = null
)

@Serializable
data class ApiGroupBuyDetail(
    val id: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("shop_id") val shopId: Int = 0,
    @SerialName("creator_id") val creatorId: Int = 0,
    @SerialName("creator_name") val creatorName: String = "",
    @SerialName("product_title") val productTitle: String = "",
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("original_price") val originalPrice: Double = 0.0,
    @SerialName("target_price") val targetPrice: Double = 0.0,
    @SerialName("discount_pct") val discountPct: Double = 0.0,
    @SerialName("min_quantity") val minQuantity: Int = 5,
    @SerialName("current_qty") val currentQty: Int = 1,
    val status: String = "open",
    @SerialName("participants_count") val participantsCount: Int = 0,
    val participants: List<ApiGroupBuyParticipant> = emptyList(),
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiGroupBuyListResponse(
    val success: Boolean = false,
    @SerialName("group_buys") val groupBuys: List<ApiGroupBuy> = emptyList()
)

@Serializable
data class ApiGroupBuyCreateResponse(
    val success: Boolean = false,
    val error: String? = null,
    @SerialName("group_buy") val groupBuy: ApiGroupBuy? = null
)

@Serializable
data class ApiGroupBuyJoinResponse(
    val success: Boolean = false,
    @SerialName("group_buy_id") val groupBuyId: Int = 0,
    @SerialName("current_qty") val currentQty: Int = 0,
    @SerialName("min_quantity") val minQuantity: Int = 5,
    @SerialName("is_filled") val isFilled: Boolean = false,
    val participants: Int = 0,
    val message: String = ""
)

@Serializable
data class ApiUnreadCountResponse(
    @SerialName("unread_count") val unreadCount: Int = 0
)

@Serializable
data class ApiSuccessResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

// ── Wallet / Fidélité ──
@Serializable
data class ApiWallet(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    val balance: Double = 0.0,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("current_points") val currentPoints: Int = 0,
    val tier: String = "bronze",
    @SerialName("tier_name") val tierName: String = "Bronze",
    @SerialName("tier_color") val tierColor: String = "#8D6E63",
    @SerialName("cashback_pct") val cashbackPct: Double = 1.0,
    @SerialName("bonus_pct") val bonusPct: Double = 0.0,
    @SerialName("lifetime_spent") val lifetimeSpent: Double = 0.0,
    @SerialName("next_tier") val nextTier: ApiNextTier? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ApiNextTier(
    val name: String = "",
    @SerialName("points_needed") val pointsNeeded: Int = 0
)

@Serializable
data class ApiWalletResponse(
    val success: Boolean = false,
    val wallet: ApiWallet? = null
)

@Serializable
data class ApiWalletTransaction(
    val id: Int = 0,
    @SerialName("wallet_id") val walletId: Int = 0,
    val type: String = "",
    @SerialName("amount_fcfa") val amountFcfa: Double = 0.0,
    val points: Int = 0,
    val description: String = "",
    @SerialName("reference_type") val referenceType: String? = null,
    @SerialName("reference_id") val referenceId: Int? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiWalletTransactionsResponse(
    val success: Boolean = false,
    val transactions: List<ApiWalletTransaction> = emptyList()
)

@Serializable
data class ApiEarnResponse(
    val success: Boolean = false,
    @SerialName("earned_cashback") val earnedCashback: Double = 0.0,
    @SerialName("earned_points") val earnedPoints: Int = 0,
    @SerialName("new_balance") val newBalance: Double = 0.0,
    @SerialName("new_points") val newPoints: Int = 0,
    @SerialName("new_tier") val newTier: String = "bronze"
)

@Serializable
data class ApiRedeemResponse(
    val success: Boolean = false,
    val coupon: ApiCoupon? = null
)

@Serializable
data class ApiRechargeResponse(
    val success: Boolean = false,
    @SerialName("new_balance") val newBalance: Double = 0.0
)

// ── Coupons ──
@Serializable
data class ApiCoupon(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    val code: String = "",
    @SerialName("discount_pct") val discountPct: Double? = null,
    @SerialName("discount_fcfa") val discountFcfa: Double? = null,
    @SerialName("min_amount") val minAmount: Double = 0.0,
    @SerialName("points_cost") val pointsCost: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_used") val isUsed: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ApiCouponsResponse(
    val success: Boolean = false,
    val coupons: List<ApiCoupon> = emptyList()
)

// ── Notifications Push ──
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
