package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Extension functions for ApiClient related to Group Buying.
 */

suspend fun ApiClient.createGroupBuy(productId: Int, minQty: Int = 5, discountPct: Double = 5.0, hours: Int = 48): ApiGroupBuyCreateResponse {
    val body = buildJsonObject {
        put("product_id", productId)
        put("min_quantity", minQty)
        put("discount_pct", discountPct)
        put("expires_in_hours", hours)
    }.toString()
    return safeRequest<ApiGroupBuyCreateResponse>("POST", "/group-buy/create.php", body)
}

suspend fun ApiClient.joinGroupBuy(groupBuyId: Int, quantity: Int = 1): ApiGroupBuyJoinResponse {
    val body = buildJsonObject {
        put("group_buy_id", groupBuyId)
        put("quantity", quantity)
    }.toString()
    return safeRequest<ApiGroupBuyJoinResponse>("POST", "/group-buy/join.php", body)
}

suspend fun ApiClient.cancelGroupBuy(groupBuyId: Int): ApiSuccessResponse {
    val body = buildJsonObject {
        put("group_buy_id", groupBuyId)
    }.toString()
    return try {
        safeRequest<ApiSuccessResponse>("POST", "/group-buy/cancel.php", body)
    } catch (e: Exception) {
        ApiSuccessResponse(success = false, error = e.message)
    }
}

suspend fun ApiClient.deleteGroupBuy(groupBuyId: Int): ApiSuccessResponse {
    val body = buildJsonObject {
        put("group_buy_id", groupBuyId)
    }.toString()
    return try {
        safeRequest<ApiSuccessResponse>("POST", "/group-buy/delete.php", body)
    } catch (e: Exception) {
        ApiSuccessResponse(success = false, error = e.message)
    }
}

suspend fun ApiClient.notifyGroupParticipants(groupBuyId: Int, title: String, message: String): ApiSuccessResponse {
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

suspend fun ApiClient.fetchGroupBuys(productId: Int): List<ApiGroupBuy> {
    return try {
        val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?product_id=$productId")
        resp.groupBuys
    } catch (_: Exception) { emptyList() }
}

suspend fun ApiClient.fetchMyGroupBuys(): List<ApiGroupBuy> {
    return try {
        val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?my=1")
        resp.groupBuys
    } catch (_: Exception) { emptyList() }
}

suspend fun ApiClient.fetchShopGroupBuys(shopId: Int): List<ApiGroupBuy> {
    return try {
        val resp = safeRequest<ApiGroupBuyListResponse>("GET", "/group-buy/list.php?shop_id=$shopId")
        resp.groupBuys
    } catch (_: Exception) { emptyList() }
}

suspend fun ApiClient.fetchGroupBuyDetails(groupBuyId: Int): ApiGroupBuyDetailResponse {
    return try {
        safeRequest<ApiGroupBuyDetailResponse>("GET", "/group-buy/details.php?id=$groupBuyId")
    } catch (_: Exception) { ApiGroupBuyDetailResponse() }
}
