package com.tik_market.utils

import android.content.Context
import com.tik_market.AndroidChatContext

actual fun getSavedLanguage(): String {
    val prefs = AndroidChatContext.currentActivity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    return prefs?.getString("app_lang", "fr") ?: "fr"
}

actual fun setSavedLanguage(lang: String) {
    val prefs = AndroidChatContext.currentActivity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs?.edit()?.putString("app_lang", lang)?.apply()
}
