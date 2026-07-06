package com.dschangmarket.api

/**
 * Platform-specific persistent storage for the auth token.
 * - WasmJs  → localStorage
 * - Android → SharedPreferences
 */
expect object TokenStorage {
    fun save(token: String)
    fun load(): String?
    fun clear()
}
