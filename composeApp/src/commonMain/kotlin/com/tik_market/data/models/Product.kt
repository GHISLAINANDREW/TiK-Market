package com.tik_market.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val shopLocation: String = "",
    val vendorId: String = "",
    val vendorPhone: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val comparePrice: Double? = null,
    val category: String = "",
    val images: List<String> = emptyList(),
    val stock: Int = 0,
    val unit: String = "pièce",
    val rating: Float = 0f,
    val totalReviews: Int = 0,
    val totalSales: Int = 0,
    val userPurchaseCount: Int = 0,
    val shopVerified: Boolean = false,
    val isStory: Boolean = false
) {
    val discountPercent: Int
        get() = if (comparePrice != null && comparePrice > 0)
            ((comparePrice - price) / comparePrice * 100).toInt() else 0

    val isInStock: Boolean get() = stock > 0
}
