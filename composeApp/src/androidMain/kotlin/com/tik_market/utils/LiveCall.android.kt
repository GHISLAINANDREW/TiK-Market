package com.tik_market.utils

// ── LiveKit room singleton (Stubbed / Commented out for now) ──
private var currentState = LiveCallState.IDLE

actual fun liveKitConnect(
    url: String,
    token: String,
    room: String,
    identity: String,
    listener: LiveCallListener
) {
    currentState = LiveCallState.FAILED
    listener.onStateChanged(LiveCallState.FAILED)
    listener.onError("LiveKit est désactivé temporairement")
}

actual fun liveKitPublishCamera(enabled: Boolean): Boolean = false

actual fun liveKitPublishMic(enabled: Boolean): Boolean = false

actual fun liveKitSetMicMuted(muted: Boolean) {}

actual fun liveKitSetCameraEnabled(enabled: Boolean) {}

actual fun liveKitSwitchCamera() {}

actual fun liveKitDisconnect() {
    currentState = LiveCallState.DISCONNECTED
}

actual fun liveKitCurrentState(): LiveCallState = currentState
