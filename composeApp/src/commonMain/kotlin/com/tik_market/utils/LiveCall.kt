package com.tik_market.utils

/**
 * LiveKit real-time call engine abstraction.
 *
 * This provides a platform-agnostic interface for real-time video/audio calls
 * and live streaming powered by LiveKit (WebRTC). The actual implementations
 * live in each platform source set:
 *   - Android: uses the official LiveKit Android SDK (io.livekit:livekit-android)
 *   - JS/Wasm: uses the LiveKit JS client (livekit-client)
 *
 * The flow is:
 *   1. The app requests a LiveKit access token from the backend
 *      (`POST /live/token.php`) for a given room.
 *   2. [connect] connects to the LiveKit server with that token.
 *   3. [publishCamera] / [publishMic] publish local tracks.
 *   4. Remote participants' tracks are surfaced via [onRemoteTrack].
 *   5. [disconnect] tears everything down.
 */

/** A remote participant's media track (video or audio). */
data class LiveRemoteTrack(
    val participantIdentity: String,
    val participantName: String,
    val isVideo: Boolean,
    /** Platform-specific handle to the track (e.g. a SurfaceView / MediaStream). */
    val handle: Any?
)

/** Connection state of the LiveKit room. */
enum class LiveCallState {
    IDLE, CONNECTING, CONNECTED, FAILED, DISCONNECTED
}

/** Callback interface for LiveKit events. */
interface LiveCallListener {
    fun onStateChanged(state: LiveCallState) {}
    fun onRemoteTrackAdded(track: LiveRemoteTrack) {}
    fun onRemoteTrackRemoved(track: LiveRemoteTrack) {}
    fun onParticipantJoined(identity: String, name: String) {}
    fun onParticipantLeft(identity: String) {}
    fun onError(message: String) {}
}

/**
 * Connects to a LiveKit room.
 *
 * @param url       LiveKit server WSS URL (e.g. wss://tik-market.duckdns.org)
 * @param token     LiveKit access token from the backend
 * @param room      Room name
 * @param identity  Local participant identity
 * @param listener  Callback listener
 */
expect fun liveKitConnect(
    url: String,
    token: String,
    room: String,
    identity: String,
    listener: LiveCallListener
)

/** Publishes the local camera (video) track. Returns true on success. */
expect fun liveKitPublishCamera(enabled: Boolean): Boolean

/** Publishes the local microphone (audio) track. Returns true on success. */
expect fun liveKitPublishMic(enabled: Boolean): Boolean

/** Mutes/unmutes the local microphone. */
expect fun liveKitSetMicMuted(muted: Boolean)

/** Mutes/unmutes the local camera (video). */
expect fun liveKitSetCameraEnabled(enabled: Boolean)

/** Switches between front and back camera. */
expect fun liveKitSwitchCamera()

/** Disconnects from the LiveKit room and releases all resources. */
expect fun liveKitDisconnect()

/** Returns the current connection state. */
expect fun liveKitCurrentState(): LiveCallState