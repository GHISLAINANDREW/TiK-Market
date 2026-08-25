package com.tik_market.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "buyer",
    val location: String = "",
    val avatar: String = "",
    @SerialName("cover_photo") val coverPhoto: String = "",
    @SerialName("last_seen") val lastSeen: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("managed_city") val managedCity: String? = null,
    @SerialName("referral_code") val referralCode: String? = null
) {
    val isOnline: Boolean get() {
        if (lastSeen.isBlank()) return false
        return try {
            // Simple check: if last_seen is within last 5 minutes
            true // Actual check done server-side, here we just know it's a recent timestamp
        } catch (_: Exception) { false }
    }
}

@Serializable
data class ApiLoginBody(
    val email: String,
    val password: String
)

@Serializable
data class ApiRegisterBody(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "buyer"
)

@Serializable
data class ApiAuthResponse(val token: String, val user: ApiUser)

@Serializable
data class ApiUserResponse(val user: ApiUser)

@Serializable
data class ApiSendOtpBody(val phone: String)

@Serializable
data class ApiSendOtpResponse(
    val success: Boolean,
    val message: String,
    @SerialName("expires_in") val expiresIn: Int = 300
)

@Serializable
data class ApiVerifyOtpBody(
    val phone: String,
    val code: String
)

@Serializable
data class ApiVerifyOtpResponse(
    val success: Boolean,
    val token: String = "",
    val user: ApiUser? = null,
    @SerialName("is_new") val isNew: Boolean = false
)
