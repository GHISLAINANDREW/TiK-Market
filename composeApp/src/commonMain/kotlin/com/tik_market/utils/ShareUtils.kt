package com.tik_market.utils

/**
 * Shares or copies text to clipboard.
 * On Android: opens system share sheet (Intent.ACTION_SEND).
 * On Web: uses navigator.share() if available, else copies to clipboard.
 */
expect fun shareText(text: String, title: String = "Partager")
