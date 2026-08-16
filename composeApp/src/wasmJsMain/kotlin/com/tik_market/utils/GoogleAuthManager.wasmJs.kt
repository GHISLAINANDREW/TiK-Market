package com.tik_market.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class GoogleAuthManager {
    private val webClientId = "706844801362-57gso395t542iducvv1rdvp12rohsd3i.apps.googleusercontent.com"

    actual suspend fun signIn(): GoogleUserData? = suspendCancellableCoroutine { continuation ->
        try {
            if (!isGoogleLoaded()) {
                println("[GoogleAuth] Google script not loaded")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            if (!isGoogleSignInHelperLoaded()) {
                println("[GoogleAuth] JS helper not found")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            callGoogleSignIn(webClientId) { data ->
                if (data != null) {
                    continuation.resume(GoogleUserData(
                        name = data.name,
                        email = data.email,
                        idToken = data.idToken
                    ))
                } else {
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            println("[GoogleAuth] Error: ${e.message}")
            continuation.resume(null)
        }
    }
}

private fun isGoogleLoaded(): Boolean = js("typeof window.google !== 'undefined'")
private fun isGoogleSignInHelperLoaded(): Boolean = js("typeof window.googleSignIn !== 'undefined'")

private fun callGoogleSignIn(clientId: String, callback: (JsGoogleUserData?) -> Unit): Unit = 
    js("window.googleSignIn(clientId, callback)")

private external interface JsGoogleUserData : JsAny {
    val name: String
    val email: String
    val idToken: String
}

@Composable
actual fun rememberGoogleAuthManager(): GoogleAuthManager {
    return remember { GoogleAuthManager() }
}
