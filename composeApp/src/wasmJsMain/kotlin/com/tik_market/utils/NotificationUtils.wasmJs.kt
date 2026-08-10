package com.tik_market.utils

import kotlinx.browser.document
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Native browser notification logic for WasmJs.
 */
@JsFun("""() => {
    if (!("Notification" in window)) return;
    if (Notification.permission !== "granted" && Notification.permission !== "denied") {
        Notification.requestPermission();
    }
    return Notification.permission === "granted";
}""")
private external fun jsRequestPermission(): Boolean

@JsFun("""(title, body) => {
    if (!("Notification" in window)) return;
    if (Notification.permission === "granted") {
        new Notification(title, { body: body, icon: "/favicon.svg" });
    }
}""")
private external fun jsShowNotification(title: String, body: String)

@JsFun("""(count) => {
    // Update page title with badge
    if (count > 0) {
        document.title = "(" + count + ") Dschang Market";
    } else {
        document.title = "Dschang Market Place";
    }
}""")
private external fun jsUpdateBadge(count: Int)

actual object NotificationUtils {
    private var permissionRequested = false

    actual fun requestPermission() {
        if (!permissionRequested) {
            permissionRequested = true
            jsRequestPermission()
        }
    }

    private val _navigationEvents = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    actual val navigationEvents = _navigationEvents.asSharedFlow()

    actual fun onNotificationClicked() {
        _navigationEvents.tryEmit(Unit)
    }

    actual fun showNotification(title: String, message: String) {
        jsShowNotification(title, message)
    }

    fun updateUnreadBadge(count: Int) {
        jsUpdateBadge(count)
    }
}
