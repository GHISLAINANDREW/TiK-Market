package com.tik_market.utils

/** Returns the saved language preference ("fr" or "en"). */
expect fun getSavedLanguage(): String

/** Saves the language preference. */
expect fun setSavedLanguage(lang: String)
