package com.dschangmarket.utils

/**
 * Native browser notification logic for WasmJs.
 */
@JsFun("""() => {
    if (!("Notification" in window)) return;
    if (Notification.permission !== "granted" && Notification.permission !== "denied") {
        Notification.requestPermission();
    }
}""")
private external fun jsRequestPermission()

@JsFun("""(title, body) => {
    if (!("Notification" in window)) return;
    if (Notification.permission === "granted") {
        new Notification(title, { body: body });
        // Play sound
        const audio = new Audio("https://cdn.pixabay.com/audio/2022/03/15/audio_783331b54f.mp3");
        audio.play().catch(e => console.log("Audio play failed:", e));
    }
}""")
private external fun jsShowNotification(title: String, body: String)

actual object NotificationUtils {
    actual fun requestPermission() {
        jsRequestPermission()
    }

    actual fun showNotification(title: String, message: String) {
        jsShowNotification(title, message)
    }
}
