package com.dschangmarket.api

// ── JS localStorage interop via @JsFun ──

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun jsSetItem(key: String, value: String)

@JsFun("(key) => { return window.localStorage.getItem(key); }")
private external fun jsGetItem(key: String): String?

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun jsRemoveItem(key: String)

actual object TokenStorage {
    private const val KEY = "dschang_market_token"

    actual fun save(token: String) {
        jsSetItem(KEY, token)
    }

    actual fun load(): String? {
        val value = jsGetItem(KEY)
        return if (value.isNullOrBlank()) null else value
    }

    actual fun clear() {
        jsRemoveItem(KEY)
    }
}
