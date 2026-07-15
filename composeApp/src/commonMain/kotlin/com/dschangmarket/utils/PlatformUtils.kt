package com.dschangmarket.utils

/**
 * Returns a startup parameter (URL query param on Web, Intent data on Android).
 */
expect fun getStartupParameter(key: String): String?

/**
 * Copies text to clipboard.
 */
expect fun copyToClipboard(text: String)
