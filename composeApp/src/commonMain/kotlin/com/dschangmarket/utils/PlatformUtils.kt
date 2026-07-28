package com.dschangmarket.utils

/**
 * Returns a startup parameter (URL query param on Web, Intent data on Android).
 */
expect fun getStartupParameter(key: String): String?

/**
 * Sets a startup parameter (Internal use for navigation).
 */
expect fun setStartupParameter(key: String, value: String?)

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

/**
 * Observes online/offline connectivity status.
 * Calls onChange(true) when online, onChange(false) when offline.
 * Returns a cancellation function (call to stop observing).
 */
expect fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit

/**
 * Triggers a file download in the browser (no-op on Android).
 */
expect fun downloadFile(url: String, filename: String)
