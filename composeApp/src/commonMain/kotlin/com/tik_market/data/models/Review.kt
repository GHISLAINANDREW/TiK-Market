package com.tik_market.data.models

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val date: String = ""
)
