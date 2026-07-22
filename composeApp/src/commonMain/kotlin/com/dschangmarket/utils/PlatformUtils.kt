package com.dschangmarket.utils

/**
 * Returns a startup parameter (URL query param on Web, Intent data on Android).
 */
expect fun getStartupParameter(key: String): String?

/**
 * Copies text to clipboard.
 */
expect fun copyToClipboard(text: String)

/**
 * Updates the page/tab title with unread badge count (no-op on Android).
 */
expect fun updateUnreadBadge(count: Int)

/**
 * Sets up a callback that fires when the browser tab regains focus (no-op on Android).
 */
expect fun setupTabFocusRefresh(callback: () -> Unit)
