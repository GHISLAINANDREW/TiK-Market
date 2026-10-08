package com.tik_market.utils

// ── Web (JS) LiveKit implementation ──
//
// NOTE: The web build uses the `livekit-client` npm package. To enable it:
//   1. `npm install livekit-client`
//   2. Import it via a JS interop shim and wire the calls below.
//
// Until that shim is added, these are safe no-ops so the web build compiles
// and the app runs (calls simply won't connect on web yet).

private var jsState = LiveCallState.IDLE

actual fun liveKitConnect(
    url: String,
    token: String,
    room: String,
    identity: String,
    listener: LiveCallListener
) {
    jsState = LiveCallState.FAILED
    listener.onStateChanged(LiveCallState.FAILED)
    listener.onError("LiveKit web non configuré (livekit-client requis)")
}

actual fun liveKitPublishCamera(enabled: Boolean): Boolean = false
actual fun liveKitPublishMic(enabled: Boolean): Boolean = false
actual fun liveKitSetMicMuted(muted: Boolean) {}
actual fun liveKitSetCameraEnabled(enabled: Boolean) {}
actual fun liveKitSwitchCamera() {}
actual fun liveKitDisconnect() { jsState = LiveCallState.DISCONNECTED }
actual fun liveKitCurrentState(): LiveCallState = jsState