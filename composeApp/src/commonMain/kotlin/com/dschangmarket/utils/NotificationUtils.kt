package com.dschangmarket.utils

/**
 * Expected interface for handling local/push-like notifications across platforms.
 */
expect object NotificationUtils {
    /**
     * Requests permission to show notifications.
     */
    fun requestPermission()

    /**
     * Shows a local notification with a title and message.
     * Also plays a notification sound.
     */
    fun showNotification(title: String, message: String)
}
