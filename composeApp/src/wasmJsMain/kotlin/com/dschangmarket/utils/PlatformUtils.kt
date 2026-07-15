package com.dschangmarket.utils

import kotlinx.browser.window

actual fun getStartupParameter(key: String): String? {
    val search = window.location.search
    if (search.isBlank()) return null
    val params = search.substring(1).split("&")
    for (p in params) {
        val kv = p.split("=")
        if (kv.size == 2 && kv[0] == key) return kv[1]
    }
    return null
}

actual fun copyToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}
