package com.tik_market.data.models

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val shopName: String = ""
) {
    val subtotal: Double get() = product.price * quantity
}
