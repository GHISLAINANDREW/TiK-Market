package com.tik_market.api

import kotlinx.browser.localStorage

actual object TokenStorage {
    private const val KEY = "tik_market_token"

    actual fun save(token: String) {
        localStorage.setItem(KEY, token)
    }

    actual fun load(): String? {
        return localStorage.getItem(KEY)
    }

    actual fun clear() {
        localStorage.removeItem(KEY)
    }
}
