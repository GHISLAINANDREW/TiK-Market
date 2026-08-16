package com.tik_market.utils

actual fun getSystemLanguage(): String {
    val raw = getSystemLanguageJs().lowercase()
    val lang = raw.substringBefore("-").substringBefore("_").trim()
    return if (lang == "fr" || lang == "en") lang else "fr"
}

actual fun getSavedLanguage(): String {
    val saved = getLanguageJs()
    return if (saved == "fr" || saved == "en") saved else getSystemLanguage()
}

actual fun setSavedLanguage(lang: String) = setLanguageJs(if (lang == "en") "en" else "fr")

@JsFun("""() => navigator.language || navigator.userLanguage || 'fr'""")
private external fun getSystemLanguageJs(): String

@JsFun("""() => localStorage.getItem('app_lang') || ''""")
private external fun getLanguageJs(): String

@JsFun("""(lang) => localStorage.setItem('app_lang', lang)""")
private external fun setLanguageJs(lang: String)