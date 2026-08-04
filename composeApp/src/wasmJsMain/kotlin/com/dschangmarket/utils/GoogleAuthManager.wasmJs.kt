package com.dschangmarket.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class GoogleAuthManager {
    actual suspend fun signIn(): GoogleUserData? {
        // TODO: Implémenter Google Sign-In pour Web (GSI)
        return null
    }
}

@Composable
actual fun rememberGoogleAuthManager(): GoogleAuthManager {
    return remember { GoogleAuthManager() }
}
