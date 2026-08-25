package com.tik_market.api.dto

import com.tik_market.api.ApiClient
import com.tik_market.data.models.Product
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("user_purchase_count") val userPurchaseCount: Int = 0,
    val reviews: List<ApiReview>? = null
)

@Serializable
data class ApiShopResponse(val shop: ApiShop)

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
data class ApiProductsResponse(val products: List<ApiProduct>, val pagination: ApiPagination? = null)

@Serializable
data class ApiPagination(val page: Int, val limit: Int, val total: Int, val pages: Int)

@Serializable
data class ApiReviewBody(
    @SerialName("product_id") val productId: Int,
    val rating: Int,
    val comment: String
)

@Serializable
data class ApiReviewResponse(val reviews: List<ApiReview> = emptyList())

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
data class ApiPromoCreateBody(
    @SerialName("shop_id") val shopId: Int,
    val code: String,
    @SerialName("discount_pct") val discountPct: Double = 0.0,
    @SerialName("discount_fixed") val discountFixed: Int = 0,
    @SerialName("min_amount") val minAmount: Int = 0,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class ApiVendorInteractionsResponse(
    val likes: List<ApiInteractionUser> = emptyList(),
    val reviews: List<ApiProductReview> = emptyList(),
    val subscribers: List<ApiInteractionUser> = emptyList()
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
data class ApiInteractionUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val avatar: String = "",
    @SerialName("liked_at") val likedAt: String = "",
    @SerialName("subscribed_at") val subscribedAt: String = ""
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
        userPurchaseCount = userPurchaseCount,
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
