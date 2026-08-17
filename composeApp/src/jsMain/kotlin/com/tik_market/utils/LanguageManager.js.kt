package com.tik_market.utils

import kotlinx.browser.localStorage
import kotlinx.browser.window

actual fun getSystemLanguage(): String {
    val raw = (window.navigator.language).lowercase()
    val lang = raw.substringBefore("-").substringBefore("_").trim()
    return if (lang == "fr" || lang == "en") lang else "fr"
}

actual fun getSavedLanguage(): String {
    val saved = localStorage.getItem("app_lang") ?: ""
    return if (saved == "fr" || saved == "en") saved else getSystemLanguage()
}

actual fun setSavedLanguage(lang: String) {
    localStorage.setItem("app_lang", if (lang == "en") "en" else "fr")
}
