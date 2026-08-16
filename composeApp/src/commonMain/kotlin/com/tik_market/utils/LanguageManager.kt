package com.tik_market.utils

/** Returns the detected system language ("fr" or "en", fallback "fr"). */
expect fun getSystemLanguage(): String

/** Returns the saved language preference, or the system language if none saved. */
expect fun getSavedLanguage(): String

/** Saves the language preference. */
expect fun setSavedLanguage(lang: String)