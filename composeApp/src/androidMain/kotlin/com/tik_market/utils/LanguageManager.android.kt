package com.tik_market.utils

import android.content.Context
import com.tik_market.AndroidChatContext
import java.util.Locale

fun normalizeSystemLang(lang: String?): String =
    when (lang) {
        "fr" -> "fr"
        "en" -> "en"
        else -> "fr"
    }

actual fun getSystemLanguage(): String =
    normalizeSystemLang(Locale.getDefault().language.lowercase())

actual fun getSavedLanguage(): String {
    val prefs = AndroidChatContext.currentActivity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val saved = prefs?.getString("app_lang", null)
    return if (saved == "fr" || saved == "en") saved else getSystemLanguage()
}

actual fun setSavedLanguage(lang: String) {
    val prefs = AndroidChatContext.currentActivity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs?.edit()?.putString("app_lang", if (lang == "en") "en" else "fr")?.apply()
}