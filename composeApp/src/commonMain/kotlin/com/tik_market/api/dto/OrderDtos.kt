package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiVendorInfo(
    @SerialName("shop_name") val shopName: String = "",
    @SerialName("vendor_phone") val vendorPhone: String = "",
    @SerialName("vendor_phone_user") val vendorPhoneUser: String = ""
)

@Serializable
data class ApiOrderItem(
    @SerialName("product_id") val productId: Int = 0,
    val title: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0
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
    @SerialName("shop_total") val shopTotal: Double? = null,
    @SerialName("vendor_confirmed") val vendorConfirmed: Int = 0,
    @SerialName("client_confirmed") val clientConfirmed: Int = 0
)

@Serializable
data class ApiOrderResponse(val order: ApiOrder)

@Serializable
data class ApiOrdersResponse(
    val orders: List<ApiOrder> = emptyList(),
    val shop: ApiShop? = null
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
data class ApiInitiatePaymentBody(
    @SerialName("order_id") val orderId: Int,
    val provider: String,
    val phone: String
)

@Serializable
data class ApiPaymentResponse(val payment: ApiPayment)

@Serializable
data class ApiNextTier(
    val name: String = "",
    @SerialName("points_needed") val pointsNeeded: Int = 0
)

@Serializable
data class ApiWallet(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    val balance: Double = 0.0,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("current_points") val currentPoints: Int = 0,
    val tier: String = "bronze",
    @SerialName("tier_name") val tierName: String? = null,
    @SerialName("tier_color") val tierColor: String? = null,
    @SerialName("cashback_pct") val cashbackPct: Double = 1.0,
    @SerialName("bonus_pct") val bonusPct: Double = 0.0,
    @SerialName("lifetime_spent") val lifetimeSpent: Double = 0.0,
    @SerialName("next_tier") val nextTier: ApiNextTier? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
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
data class ApiRedeemResponse(
    val success: Boolean = false,
    val coupon: ApiCoupon? = null
)

@Serializable
data class ApiRechargeResponse(
    val success: Boolean = false,
    @SerialName("new_balance") val newBalance: Double = 0.0
)

@Serializable
data class ApiCouponsResponse(
    val success: Boolean = false,
    val coupons: List<ApiCoupon> = emptyList()
)
