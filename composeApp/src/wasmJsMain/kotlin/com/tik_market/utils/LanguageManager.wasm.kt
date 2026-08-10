package com.tik_market.utils

actual fun getSavedLanguage(): String = getLanguageJs()
actual fun setSavedLanguage(lang: String) = setLanguageJs(lang)

@JsFun("""() => localStorage.getItem('app_lang') || 'fr'""")
private external fun getLanguageJs(): String

@JsFun("""(lang) => localStorage.setItem('app_lang', lang)""")
private external fun setLanguageJs(lang: String)
