package com.tik_market.utils

import androidx.compose.runtime.Composable

@kotlinx.serialization.Serializable
data class GoogleUserData(
    val name: String?,
    val email: String?,
    val idToken: String?
)

expect class GoogleAuthManager {
    suspend fun signIn(): GoogleUserData?
}

@Composable
expect fun rememberGoogleAuthManager(): GoogleAuthManager
