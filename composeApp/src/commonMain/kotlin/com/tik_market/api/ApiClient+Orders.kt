package com.tik_market.api

import com.tik_market.api.dto.*
import com.tik_market.currentTimeMillis
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Extension functions for ApiClient related to Cart, Orders, Payments and Wallet.
 */

// ── Cart ──

suspend fun ApiClient.fetchCart(): List<ApiCartItem> {
    return safeRequest("GET", ApiClient.Endpoints.CART)
}

suspend fun ApiClient.addToCart(productId: Int, quantity: Int = 1) {
    val body = json.encodeToString(ApiCartActionBody(productId, quantity))
    post(ApiClient.Endpoints.CART, body)
}

suspend fun ApiClient.updateCart(productId: Int, quantity: Int) {
    val body = json.encodeToString(ApiCartActionBody(productId, quantity))
    put(ApiClient.Endpoints.CART, body)
}

suspend fun ApiClient.removeFromCart(productId: Int) {
    delete("${ApiClient.Endpoints.CART}?product_id=$productId")
}

// ── Orders ──

suspend fun ApiClient.fetchOrders(): List<ApiOrder> {
    val resp = safeRequest<ApiOrdersResponse>("GET", ApiClient.Endpoints.ORDERS)
    return resp.orders
}

/**
 * Fetch a single order by ID.
 */
suspend fun ApiClient.fetchOrder(orderId: Int): ApiOrder {
    return safeRequest<ApiOrderResponse>("GET", "${ApiClient.Endpoints.ORDERS}?id=$orderId").order
}

suspend fun ApiClient.createOrder(
    shippingAddress: String,
    phone: String,
    notes: String? = null,
    paymentMethod: String = "Mobile Money",
    paymentType: String = "delivery",
    items: List<ApiCartItemBody> = emptyList(),
    useWallet: Boolean = false
): ApiOrder {
    val body = json.encodeToString(ApiCreateOrderBody(
        shippingAddress = shippingAddress,
        phone = phone,
        notes = notes ?: "",
        paymentMethod = paymentMethod,
        paymentType = paymentType,
        items = items,
        useWallet = if (useWallet) 1 else 0
    ))
    return safeRequest<ApiOrderResponse>("POST", ApiClient.Endpoints.ORDERS, body).order
}

suspend fun ApiClient.deleteOrder(orderId: Int) {
    delete("${ApiClient.Endpoints.ORDERS}?id=$orderId")
}

suspend fun ApiClient.updateOrderStatus(orderId: Int, status: String) {
    put("${ApiClient.Endpoints.ORDERS_VENDOR}?id=$orderId&status=$status", "")
}

// ── Vendor Orders ──

suspend fun ApiClient.fetchVendorOrders(): List<ApiOrder> {
    val resp = safeRequest<ApiOrdersResponse>("GET", ApiClient.Endpoints.ORDERS_VENDOR)
    return resp.orders
}

suspend fun ApiClient.updateVendorOrderStatus(orderId: Int, status: String) {
    updateOrderStatus(orderId, status)
}

suspend fun ApiClient.confirmVendorDelivery(orderId: Int) {
    updateVendorOrderStatus(orderId, "delivered")
}

suspend fun ApiClient.confirmClientDelivery(orderId: Int) {
    confirmOrderReceived(orderId)
}

suspend fun ApiClient.confirmOrderReceived(orderId: Int) {
    put("${ApiClient.Endpoints.ORDERS_VENDOR}?id=$orderId&action=confirm_received", "")
}

// ── Payments ──

suspend fun ApiClient.initiatePayment(orderId: Int, provider: String, phone: String): ApiPayment {
    val body = json.encodeToString(ApiInitiatePaymentBody(orderId, provider, phone))
    return safeRequest<ApiPaymentResponse>("POST", ApiClient.Endpoints.PAYMENTS, body).payment
}

suspend fun ApiClient.getPaymentStatus(orderId: Int): ApiPayment? {
    return try {
        safeRequest<ApiPayment>("GET", "${ApiClient.Endpoints.PAYMENTS}?order_id=$orderId")
    } catch (_: Exception) { null }
}

// ── Wallet / Fidélité ──

suspend fun ApiClient.fetchWallet(): ApiWallet? {
    return try {
        val ts = currentTimeMillis()
        val resp = safeRequest<ApiWalletResponse>("GET", "${ApiClient.Endpoints.WALLET}?t=$ts")
        resp.wallet
    } catch (_: Exception) {
        null
    }
}

suspend fun ApiClient.fetchWalletTransactions(): List<ApiWalletTransaction> {
    return try {
        val resp = safeRequest<ApiWalletTransactionsResponse>("GET", ApiClient.Endpoints.WALLET_TRANSACTIONS)
        resp.transactions
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.earnPoints(amount: Double, orderId: Int): ApiEarnResponse {
    val body = buildJsonObject {
        put("amount", amount)
        put("order_id", orderId)
    }.toString()
    return safeRequest("POST", ApiClient.Endpoints.WALLET_EARN, body)
}

suspend fun ApiClient.redeemPoints(points: Int): ApiRedeemResponse {
    val body = buildJsonObject {
        put("points", points)
    }.toString()
    return safeRequest("POST", ApiClient.Endpoints.WALLET_REDEEM, body)
}

suspend fun ApiClient.rechargeWallet(amount: Double, method: String = "other"): ApiRechargeResponse {
    val body = buildJsonObject {
        put("amount", amount)
        put("method", method)
    }.toString()
    return safeRequest("POST", ApiClient.Endpoints.WALLET_RECHARGE, body)
}

suspend fun ApiClient.fetchCoupons(): List<ApiCoupon> {
    return try {
        val resp = safeRequest<ApiCouponsResponse>("GET", ApiClient.Endpoints.COUPONS)
        resp.coupons
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.useCoupon(code: String): ApiCoupon? {
    return try {
        val body = buildJsonObject { put("code", code) }.toString()
        val resp = safeRequest<ApiCouponsResponse>("POST", ApiClient.Endpoints.COUPONS_USE, body)
        resp.coupons.firstOrNull()
    } catch (_: Exception) {
        null
    }
}
