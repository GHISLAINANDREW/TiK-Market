package com.dschangmarket.api

/**
 * Platform-specific HTTP engine.
 * - WasmJs : uses fetch() via JS interop
 * - Android : uses ktor / HttpURLConnection
 */
expect object HttpEngine {
    suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): String
}
