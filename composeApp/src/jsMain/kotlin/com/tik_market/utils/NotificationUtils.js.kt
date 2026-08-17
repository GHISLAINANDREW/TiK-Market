package com.tik_market.utils

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

actual object NotificationUtils {
    private val _navigationEvents = MutableSharedFlow<Unit>()
    actual val navigationEvents: SharedFlow<Unit> = _navigationEvents

    actual fun requestPermission() {
        js("""
        if ("Notification" in window) {
            Notification.requestPermission();
        }
        """)
    }

    actual fun showNotification(title: String, message: String) {
        js("""
        if ("Notification" in window && Notification.permission === "granted") {
            new Notification(title, { body: message, icon: "/favicon.svg" });
        }
        """)
    }

    actual fun onNotificationClicked() {
        // Not easily detectable globally on web, usually handled by notification options
    }
}
