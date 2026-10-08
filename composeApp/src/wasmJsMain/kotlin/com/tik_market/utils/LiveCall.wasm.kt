package com.tik_market.utils

// ── Wasm/JS LiveKit implementation ──
//
// Same as the JS target: requires the `livekit-client` npm package + a JS
// interop shim. Until then, safe no-ops so the Wasm build compiles.

private var wasmState = LiveCallState.IDLE

actual fun liveKitConnect(
    url: String,
    token: String,
    room: String,
    identity: String,
    listener: LiveCallListener
) {
    wasmState = LiveCallState.FAILED
    listener.onStateChanged(LiveCallState.FAILED)
    listener.onError("LiveKit wasm non configuré (livekit-client requis)")
}

actual fun liveKitPublishCamera(enabled: Boolean): Boolean = false
actual fun liveKitPublishMic(enabled: Boolean): Boolean = false
actual fun liveKitSetMicMuted(muted: Boolean) {}
actual fun liveKitSetCameraEnabled(enabled: Boolean) {}
actual fun liveKitSwitchCamera() {}
actual fun liveKitDisconnect() { wasmState = LiveCallState.DISCONNECTED }
actual fun liveKitCurrentState(): LiveCallState = wasmState