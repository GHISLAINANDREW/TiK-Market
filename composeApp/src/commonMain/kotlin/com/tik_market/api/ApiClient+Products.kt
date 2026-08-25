package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Extension functions for ApiClient related to Products and Shops.
 */

suspend fun ApiClient.fetchProducts(
    category: String? = null,
    search: String? = null,
    minPrice: Double? = null,
    maxPrice: Double? = null,
    shopId: Int? = null,
    sortBy: String? = null,
    location: String? = null,
    includeInactive: Boolean = false
): List<ApiProduct> {
    val path = buildUrl(ApiClient.Endpoints.PRODUCTS, mapOf(
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

suspend fun ApiClient.fetchProduct(id: Int): ApiProduct {
    return safeRequest<ApiProduct>("GET", "${ApiClient.Endpoints.PRODUCTS}?id=$id")
}

suspend fun ApiClient.submitReview(productId: Int, rating: Int, comment: String, imageUrl: String = "") {
    val body = buildJsonObject {
        put("product_id", productId)
        put("rating", rating)
        put("comment", comment)
        if (imageUrl.isNotBlank()) put("image_url", imageUrl)
    }.toString()
    post(ApiClient.Endpoints.REVIEWS, body)
}

suspend fun ApiClient.fetchProductReviews(productId: Int): ApiReviewResponse? {
    return try {
        safeRequest<ApiReviewResponse>("GET", "${ApiClient.Endpoints.REVIEWS}?product_id=$productId")
    } catch (_: Exception) { null }
}

suspend fun ApiClient.markReviewUseful(reviewId: Int) {
    try { post("${ApiClient.Endpoints.REVIEWS}?useful=$reviewId", "") } catch (_: Exception) { }
}

suspend fun ApiClient.replyToReview(reviewId: Int, reply: String) {
    try {
        val body = buildJsonObject { put("reply", reply) }.toString()
        put("${ApiClient.Endpoints.REVIEWS}?reply=$reviewId", body)
    } catch (_: Exception) { }
}

suspend fun ApiClient.createProduct(
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
    return safeRequest<ApiProduct>("POST", ApiClient.Endpoints.PRODUCTS, body)
}

suspend fun ApiClient.fetchCategories(): List<String> {
    return listOf(
        "Alimentation", "Mode", "Électronique", "Artisanat",
        "Boutique", "Services", "Agriculture", "Autres"
    )
}

suspend fun ApiClient.fetchWishlist(): List<ApiWishlistItem> {
    return safeRequest<ApiWishlistResponse>("GET", ApiClient.Endpoints.WISHLIST).items
}

suspend fun ApiClient.addToWishlist(productId: Int) {
    post(ApiClient.Endpoints.WISHLIST, """{"product_id":$productId}""")
}

suspend fun ApiClient.removeFromWishlist(productId: Int) {
    delete("${ApiClient.Endpoints.WISHLIST}?product_id=$productId")
}

suspend fun ApiClient.fetchFavoriteShops(): List<ApiFavoriteShop> {
    return try {
        safeRequest<ApiFavoriteShopsResponse>("GET", ApiClient.Endpoints.FAVORITE_SHOPS).favorites
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.addFavoriteShop(shopId: Int) {
    val body = buildJsonObject { put("shop_id", shopId) }.toString()
    post(ApiClient.Endpoints.FAVORITE_SHOPS, body)
}

suspend fun ApiClient.removeFavoriteShop(shopId: Int) {
    delete("${ApiClient.Endpoints.FAVORITE_SHOPS}?shop_id=$shopId")
}

suspend fun ApiClient.validatePromoCode(code: String, amount: Double): ApiPromoValidationResponse {
    return try {
        val path = buildUrl(ApiClient.Endpoints.PROMOTIONS, mapOf("code" to code, "amount" to amount))
        safeRequest<ApiPromoValidationResponse>("GET", path)
    } catch (_: Exception) {
        ApiPromoValidationResponse(valid = false, error = "Erreur de validation")
    }
}

suspend fun ApiClient.fetchShopPromotions(shopId: Int): List<ApiPromotion> {
    return try {
        safeRequest<ApiPromotionsResponse>("GET", "${ApiClient.Endpoints.PROMOTIONS}?shop_id=$shopId").promotions
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.createPromotion(body: ApiPromoCreateBody) {
    post(ApiClient.Endpoints.PROMOTIONS, json.encodeToString(body))
}

suspend fun ApiClient.deletePromotion(id: Int) {
    delete("${ApiClient.Endpoints.PROMOTIONS}?id=$id")
}

suspend fun ApiClient.fetchShops(location: String? = null): List<ApiShop> {
    return try {
        val path = buildUrl(ApiClient.Endpoints.SHOPS, mapOf("location" to location))
        safeRequest<List<ApiShop>>("GET", path)
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.fetchShop(shopId: Int): ApiShop? {
    return try {
        safeRequest<ApiShopResponse>("GET", "${ApiClient.Endpoints.SHOPS}?id=$shopId").shop
    } catch (_: Exception) {
        null
    }
}

suspend fun ApiClient.createShop(
    name: String,
    description: String,
    phone: String,
    location: String,
    category: String,
    imageUrl: String? = null
): ApiShop {
    val body = json.encodeToString(ApiCreateShopBody(name, description, phone, location, category, imageUrl))
    return safeRequest<ApiShopResponse>("POST", ApiClient.Endpoints.SHOPS, body).shop
}

suspend fun ApiClient.updateShop(
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
    put("${ApiClient.Endpoints.SHOPS}?id=$shopId", body)
}

suspend fun ApiClient.updateProduct(
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
    put("${ApiClient.Endpoints.PRODUCTS}?id=$productId", body)
}

suspend fun ApiClient.deleteProduct(productId: Int) {
    delete("${ApiClient.Endpoints.PRODUCTS}?id=$productId")
}

suspend fun ApiClient.fetchVendorProducts(shopId: Int): List<ApiProduct> {
    return fetchProducts(shopId = shopId, includeInactive = true)
}
