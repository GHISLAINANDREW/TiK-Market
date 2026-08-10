package com.tik_market.api

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
