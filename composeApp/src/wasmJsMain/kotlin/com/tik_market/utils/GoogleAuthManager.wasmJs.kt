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

            callGoogleSignIn(webClientId) { data ->
                if (data != null) {
                    continuation.resume(GoogleUserData(
                        name = data.name,
                        email = data.email,
                        idToken = data.idToken
                    ))
                } else {
                    println("[GoogleAuth] Data null from JS")
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            println("[GoogleAuth] Error during sign-in: ${e.message}")
            continuation.resume(null)
        }
    }
}

@JsFun("() => typeof window.google !== 'undefined'")
private external fun isGoogleLoaded(): Boolean

@JsFun("(clientId, callback) => { if(window.googleSignIn) { window.googleSignIn(clientId, callback); } else { callback(null); } }")
private external fun callGoogleSignIn(clientId: String, callback: (JsGoogleUserData?) -> Unit)

private external interface JsGoogleUserData : JsAny {
    val name: String
    val email: String
    val idToken: String
}

@Composable
actual fun rememberGoogleAuthManager(): GoogleAuthManager {
    return remember { GoogleAuthManager() }
}
