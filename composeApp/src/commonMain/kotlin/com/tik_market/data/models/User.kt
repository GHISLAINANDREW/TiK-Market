package com.tik_market.data.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.BUYER,
    val shopId: String? = null,
    val location: String = "",
    val avatar: String = ""
)

enum class UserRole { BUYER, VENDOR }
