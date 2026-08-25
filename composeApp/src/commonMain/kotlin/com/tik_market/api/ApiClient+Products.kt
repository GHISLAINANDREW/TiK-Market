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

suspend fun ApiClient.fetchProduct(id: Int): ApiProduct {
    return safeRequest<ApiProduct>("GET", "${Endpoints.PRODUCTS}?id=$id")
}

suspend fun ApiClient.submitReview(productId: Int, rating: Int, comment: String, imageUrl: String = "") {
    val body = buildJsonObject {
        put("product_id", productId)
        put("rating", rating)
        put("comment", comment)
        if (imageUrl.isNotBlank()) put("image_url", imageUrl)
    }.toString()
    post(Endpoints.REVIEWS, body)
}

suspend fun ApiClient.fetchProductReviews(productId: Int): ApiReviewResponse? {
    return try {
        safeRequest<ApiReviewResponse>("GET", "${Endpoints.REVIEWS}?product_id=$productId")
    } catch (_: Exception) { null }
}

suspend fun ApiClient.markReviewUseful(reviewId: Int) {
    try { post("${Endpoints.REVIEWS}?useful=$reviewId", "") } catch (_: Exception) { }
}

suspend fun ApiClient.replyToReview(reviewId: Int, reply: String) {
    try {
        val body = buildJsonObject { put("reply", reply) }.toString()
        put("${Endpoints.REVIEWS}?reply=$reviewId", body)
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
    return safeRequest<ApiProduct>("POST", Endpoints.PRODUCTS, body)
}

suspend fun ApiClient.fetchCategories(): List<String> {
    return listOf(
        "Alimentation", "Mode", "Électronique", "Artisanat",
        "Boutique", "Services", "Agriculture", "Autres"
    )
}

suspend fun ApiClient.fetchWishlist(): List<ApiWishlistItem> {
    return safeRequest<ApiWishlistResponse>("GET", Endpoints.WISHLIST).items
}

suspend fun ApiClient.addToWishlist(productId: Int) {
    post(Endpoints.WISHLIST, """{"product_id":$productId}""")
}

suspend fun ApiClient.removeFromWishlist(productId: Int) {
    delete("${Endpoints.WISHLIST}?product_id=$productId")
}

suspend fun ApiClient.fetchFavoriteShops(): List<ApiFavoriteShop> {
    return try {
        safeRequest<ApiFavoriteShopsResponse>("GET", Endpoints.FAVORITE_SHOPS).favorites
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.addFavoriteShop(shopId: Int) {
    val body = buildJsonObject { put("shop_id", shopId) }.toString()
    post(Endpoints.FAVORITE_SHOPS, body)
}

suspend fun ApiClient.removeFavoriteShop(shopId: Int) {
    delete("${Endpoints.FAVORITE_SHOPS}?shop_id=$shopId")
}

suspend fun ApiClient.validatePromoCode(code: String, amount: Double): ApiPromoValidationResponse {
    return try {
        val path = buildUrl(Endpoints.PROMOTIONS, mapOf("code" to code, "amount" to amount))
        safeRequest<ApiPromoValidationResponse>("GET", path)
    } catch (_: Exception) {
        ApiPromoValidationResponse(valid = false, error = "Erreur de validation")
    }
}

suspend fun ApiClient.fetchShopPromotions(shopId: Int): List<ApiPromotion> {
    return try {
        safeRequest<ApiPromotionsResponse>("GET", "${Endpoints.PROMOTIONS}?shop_id=$shopId").promotions
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.createPromotion(body: ApiPromoCreateBody) {
    post(Endpoints.PROMOTIONS, json.encodeToString(body))
}

suspend fun ApiClient.deletePromotion(id: Int) {
    delete("${Endpoints.PROMOTIONS}?id=$id")
}

suspend fun ApiClient.fetchShops(location: String? = null): List<ApiShop> {
    return try {
        val path = buildUrl(Endpoints.SHOPS, mapOf("location" to location))
        safeRequest<List<ApiShop>>("GET", path)
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.fetchShop(shopId: Int): ApiShop? {
    return try {
        safeRequest<ApiShopResponse>("GET", "${Endpoints.SHOPS}?id=$shopId").shop
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
    return safeRequest<ApiShopResponse>("POST", Endpoints.SHOPS, body).shop
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
    put("${Endpoints.PRODUCTS}?id=$productId", body)
}

suspend fun ApiClient.deleteProduct(productId: Int) {
    delete("${Endpoints.PRODUCTS}?id=$productId")
}

suspend fun ApiClient.fetchVendorProducts(shopId: Int): List<ApiProduct> {
    return fetchProducts(shopId = shopId, includeInactive = true)
}
