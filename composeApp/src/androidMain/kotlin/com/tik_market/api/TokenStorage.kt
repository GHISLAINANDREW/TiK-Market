package com.tik_market.api

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation using SharedPreferences.
 * Requires init(context) to be called from MainActivity.
 */
actual object TokenStorage {
    private const val PREFS_NAME = "tik_market_prefs"
    private const val KEY_TOKEN = "auth_token"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun save(token: String) {
        prefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
    }

    actual fun load(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    actual fun clear() {
        prefs?.edit()?.remove(KEY_TOKEN)?.apply()
    }
}
