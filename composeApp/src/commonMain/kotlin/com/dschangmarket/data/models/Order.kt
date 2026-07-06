package com.dschangmarket.data.models

data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentMethod: String = "Mobile Money",
    val shippingAddress: String = "",
    val phone: String = "",
    val createdAt: String = ""
)
