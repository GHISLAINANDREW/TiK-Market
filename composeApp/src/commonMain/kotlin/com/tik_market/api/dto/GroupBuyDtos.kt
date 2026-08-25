package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class ApiGroupBuyDetailResponse(
    val success: Boolean = false,
    @SerialName("group_buy") val groupBuy: ApiGroupBuyDetail? = null
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
