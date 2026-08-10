package com.tik_market.data.models

data class Shop(
    val id: String = "",
    val vendorId: String = "",
    val name: String = "",
    val description: String = "",
    val logo: String = "",
    val phone: String = "",
    val location: String = "",
    val category: String = "",
    val rating: Float = 0f,
    val totalProducts: Int = 0,
    val isVerified: Boolean = false
)
