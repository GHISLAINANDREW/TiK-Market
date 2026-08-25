package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiCartActionBody(
    @SerialName("product_id") val productId: Int,
    val quantity: Int = 1
)

@Serializable
data class ApiReportBody(
    val type: String,
    @SerialName("target_id") val targetId: Int,
    val reason: String,
    val comment: String = ""
)

@Serializable
data class ApiCreateShopBody(
    val name: String,
    val description: String,
    val phone: String,
    val location: String,
    val category: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class ApiUploadBody(
    val image: String, // base64 data
    val filename: String
)

@Serializable
data class ApiUploadResponse(
    val success: Boolean = true,
    @SerialName("image_url") val imageUrl: String = "",
    val filename: String = ""
)

@Serializable
data class ApiSuccessResponse(
    val success: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class ApiError(val error: String)
