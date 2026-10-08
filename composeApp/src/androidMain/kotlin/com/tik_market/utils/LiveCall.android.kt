package com.tik_market.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.tik_market.AndroidChatContext
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// ── LiveKit room singleton ──
private val roomScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private var room: Room? = null
private var currentListener: LiveCallListener? = null
private var currentState = LiveCallState.IDLE

// ── Permission helpers ──
private fun ensurePermissions(): Boolean {
    val activity = AndroidChatContext.currentActivity ?: return false
    val cam = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
    val mic = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
    if (cam == PackageManager.PERMISSION_GRANTED && mic == PackageManager.PERMISSION_GRANTED) {
        return true
    }
    val missing = mutableListOf<String>()
    if (cam != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CAMERA)
    if (mic != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO)
    ActivityCompat.requestPermissions(activity, missing.toTypedArray(), 600)
    return false
}

actual fun liveKitConnect(
    url: String,
    token: String,
    roomName: String,
    identity: String,
    listener: LiveCallListener
) {
    currentListener = listener
    if (!ensurePermissions()) {
        currentState = LiveCallState.FAILED
        listener.onStateChanged(LiveCallState.FAILED)
        listener.onError("Permissions caméra/micro requises")
        return
    }

    // Disconnect any previous room.
    liveKitDisconnect()

    currentState = LiveCallState.CONNECTING
    listener.onStateChanged(LiveCallState.CONNECTING)

    val activity = AndroidChatContext.currentActivity ?: run {
        currentState = LiveCallState.FAILED
        listener.onStateChanged(LiveCallState.FAILED)
        listener.onError("Contexte Android indisponible")
        return
    }

    val newRoom = LiveKit.create(activity.applicationContext)
    room = newRoom

    roomScope.launch {
        // ── Event handling (LiveKit v2 Flow API) ──
        launch {
            newRoom.events.collect { event ->
                when (event) {
                    is RoomEvent.Connected -> {
                        currentState = LiveCallState.CONNECTED
                        listener.onStateChanged(LiveCallState.CONNECTED)
                    }
                    is RoomEvent.Disconnected -> {
                        currentState = LiveCallState.DISCONNECTED
                        listener.onStateChanged(LiveCallState.DISCONNECTED)
                    }
                    is RoomEvent.FailedToConnect -> {
                        currentState = LiveCallState.FAILED
                        listener.onStateChanged(LiveCallState.FAILED)
                        listener.onError("Connexion LiveKit échouée : ${event.error.message}")
                    }
                    is RoomEvent.ParticipantConnected -> {
                        listener.onParticipantJoined(
                            event.participant.identity,
                            event.participant.name ?: event.participant.identity
                        )
                    }
                    is RoomEvent.ParticipantDisconnected -> {
                        listener.onParticipantLeft(event.participant.identity)
                    }
                    is RoomEvent.TrackSubscribed -> {
                        val pub = event.publication
                        val participant = event.participant
                        val isVideo = pub.kind == Track.Kind.VIDEO
                        listener.onRemoteTrackAdded(
                            LiveRemoteTrack(
                                participantIdentity = participant.identity,
                                participantName = participant.name ?: participant.identity,
                                isVideo = isVideo,
                                handle = event.track
                            )
                        )
                    }
                    is RoomEvent.TrackUnsubscribed -> {
                        val participant = event.participant
                        val isVideo = event.publications.any { it.kind == Track.Kind.VIDEO }
                        listener.onRemoteTrackRemoved(
                            LiveRemoteTrack(
                                participantIdentity = participant.identity,
                                participantName = participant.name ?: participant.identity,
                                isVideo = isVideo,
                                handle = null
                            )
                        )
                    }
                    else -> {}
                }
            }
        }

        // ── Connect ──
        try {
            newRoom.connect(url, token)
        } catch (e: Exception) {
            currentState = LiveCallState.FAILED
            listener.onStateChanged(LiveCallState.FAILED)
            listener.onError("Erreur de connexion : ${e.message}")
        }
    }
}

actual fun liveKitPublishCamera(enabled: Boolean): Boolean {
    val lp = room?.localParticipant ?: return false
    return try {
        lp.setCameraEnabled(enabled)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun liveKitPublishMic(enabled: Boolean): Boolean {
    val lp = room?.localParticipant ?: return false
    return try {
        lp.setMicrophoneEnabled(enabled)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun liveKitSetMicMuted(muted: Boolean) {
    try { room?.localParticipant?.setMicrophoneEnabled(!muted) } catch (_: Exception) {}
}

actual fun liveKitSetCameraEnabled(enabled: Boolean) {
    try { room?.localParticipant?.setCameraEnabled(enabled) } catch (_: Exception) {}
}

actual fun liveKitSwitchCamera() {
    try { room?.localParticipant?.switchCamera() } catch (_: Exception) {}
}

actual fun liveKitDisconnect() {
    try { room?.disconnect() } catch (_: Exception) {}
    room = null
    currentState = LiveCallState.DISCONNECTED
    currentListener?.onStateChanged(LiveCallState.DISCONNECTED)
}

actual fun liveKitCurrentState(): LiveCallState = currentState