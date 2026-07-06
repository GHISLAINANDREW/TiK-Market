package com.dschangmarket.utils

/**
 * Simple wrapper for API call results.
 * Instead of silent catch blocks, this preserves the error message.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }
}

/**
 * Maps common exception messages to user-friendly French messages.
 */
fun userFriendlyError(e: Exception?): String {
    val msg = e?.message ?: "Une erreur est survenue"
    return when {
        msg.contains("timeout", ignoreCase = true) || msg.contains("Timed out", ignoreCase = true) ->
            "Le serveur ne répond pas. Vérifie ta connexion."
        msg.contains("Non authentifié", ignoreCase = true) ->
            "Session expirée. Connecte-toi à nouveau."
        msg.contains("HTTP 404", ignoreCase = true) ->
            "Élément introuvable."
        msg.contains("HTTP 403", ignoreCase = true) ->
            "Tu n'as pas les droits pour cette action."
        msg.contains("HTTP 401", ignoreCase = true) ->
            "Email ou mot de passe incorrect."
        msg.contains("HTTP 500", ignoreCase = true) || msg.contains("Erreur serveur", ignoreCase = true) ->
            "Erreur serveur. Réessaie plus tard."
        msg.contains("fetch", ignoreCase = true) || msg.contains("NetworkError", ignoreCase = true) ->
            "Impossible de contacter le serveur. Vérifie ta connexion."
        msg.contains("Empty", ignoreCase = true) || msg.contains("null", ignoreCase = true) ->
            "Données invalides reçues du serveur."
        else -> msg
    }
}

/**
 * Wraps a suspend API call into ApiResult.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(call())
    } catch (e: Exception) {
        ApiResult.Error(userFriendlyError(e))
    }
}
