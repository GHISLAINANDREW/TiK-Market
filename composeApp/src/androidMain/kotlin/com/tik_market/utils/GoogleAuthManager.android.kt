package com.tik_market.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class GoogleAuthManager(private val context: Context) {
    // IMPORTANT: Remplacer par votre Client ID Web depuis la Console Google Cloud
    private val webClientId = "706844801362-57gso395t542iducvv1rdvp12rohsd3i.apps.googleusercontent.com"

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    actual suspend fun signIn(): GoogleUserData? = withContext(Dispatchers.Main) {
        val activity = findActivity(context)
        if (activity == null) {
            println("[GoogleAuth] Activity non trouvée")
            return@withContext null
        }

        try {
            val credentialManager = CredentialManager.create(activity)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false) // Désactivé pour forcer le choix du compte
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            val credential = result.credential
            println("[GoogleAuth] Credential type: ${credential::class.simpleName}")
            if (credential is GoogleIdTokenCredential) {
                return@withContext GoogleUserData(
                    name = credential.displayName,
                    email = credential.id,
                    idToken = credential.idToken
                )
            }
            println("[GoogleAuth] Credential is not GoogleIdTokenCredential: $credential")
            null
        } catch (e: Exception) {
            println("[GoogleAuth] Erreur lors de la connexion Google (${e::class.simpleName}): ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

@Composable
actual fun rememberGoogleAuthManager(): GoogleAuthManager {
    val context = LocalContext.current
    return remember(context) { GoogleAuthManager(context) }
}
