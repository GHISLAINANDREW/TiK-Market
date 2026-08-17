package com.tik_market.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class GoogleAuthManager {
    actual suspend fun signIn(): GoogleUserData? = suspendCoroutine { cont ->
        val googleSignIn = window.asDynamic().googleSignIn
        if (googleSignIn != null) {
            googleSignIn("878241513233-mshm57r065798u1e7r6e3it68e219747.apps.googleusercontent.com") { result: dynamic ->
                if (result != null) {
                    cont.resume(GoogleUserData(
                        name = result.name as? String,
                        email = result.email as? String,
                        idToken = result.idToken as? String
                    ))
                } else {
                    cont.resume(null)
                }
            }
        } else {
            cont.resume(null)
        }
    }
}

@Composable
actual fun rememberGoogleAuthManager(): GoogleAuthManager {
    return remember { GoogleAuthManager() }
}
