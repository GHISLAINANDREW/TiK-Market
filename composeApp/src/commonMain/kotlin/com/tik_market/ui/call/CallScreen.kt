package com.tik_market.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.getLiveKitToken
import com.tik_market.theme.Green
import com.tik_market.theme.RedAccent
import com.tik_market.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen LiveKit call UI.
 *
 * Handles both outgoing calls (caller) and incoming calls (callee). The caller
 * requests a token from the backend and connects to a room; the callee connects
 * to the same room.
 *
 * @param roomName   The LiveKit room name (e.g. "call_<id1>_<id2>")
 * @param peerName   Display name of the other participant
 * @param isOutgoing True if this device initiated the call
 * @param onEnd      Called when the call ends (user hangs up or remote leaves)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    roomName: String,
    peerName: String,
    isOutgoing: Boolean,
    onEnd: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var callState by remember { mutableStateOf(LiveCallState.IDLE) }
    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var remoteJoined by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var callDuration by remember { mutableStateOf(0) }

    // ── Connect to LiveKit on first composition ──
    LaunchedEffect(Unit) {
        try {
            val identity = "user_${ApiClient.getCurrentUserId()}"
            val resp = ApiClient.getLiveKitToken(roomName, identity)
            if (resp != null) {
                liveKitConnect(
                    url = resp.url,
                    token = resp.token,
                    room = resp.room,
                    identity = resp.identity,
                    listener = object : LiveCallListener {
                        override fun onStateChanged(state: LiveCallState) {
                            callState = state
                            if (state == LiveCallState.FAILED) {
                                errorMsg = "Connexion impossible"
                            }
                        }
                        override fun onParticipantJoined(identity: String, name: String) {
                            remoteJoined = true
                        }
                        override fun onParticipantLeft(identity: String) {
                            // Remote hung up → end the call.
                            scope.launch {
                                delay(500)
                                onEnd()
                            }
                        }
                        override fun onError(message: String) {
                            errorMsg = message
                        }
                    }
                )
                // Publish camera + mic once connected.
                liveKitPublishCamera(true)
                liveKitPublishMic(true)
            } else {
                errorMsg = "Impossible d'obtenir le token LiveKit"
            }
        } catch (e: Exception) {
            errorMsg = "Erreur : ${e.message}"
        }
    }

    // ── Call duration timer once connected ──
    LaunchedEffect(callState) {
        if (callState == LiveCallState.CONNECTED) {
            while (callState == LiveCallState.CONNECTED) {
                delay(1000)
                callDuration++
            }
        }
    }

    // ── Cleanup on dispose ──
    DisposableEffect(Unit) {
        onDispose { liveKitDisconnect() }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F0F1A))) {
        Column(Modifier.fillMaxSize()) {
            // ── Header: peer name + status ──
            Column(
                Modifier.fillMaxWidth().padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar placeholder
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = Green.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            peerName.take(1).uppercase(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(peerName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (callState) {
                        LiveCallState.CONNECTING -> "Connexion..."
                        LiveCallState.CONNECTED -> if (remoteJoined) formatCallDuration(callDuration) else "En attente de l'autre personne..."
                        LiveCallState.FAILED -> "Échec de l'appel"
                        LiveCallState.DISCONNECTED -> "Appel terminé"
                        else -> "Préparation..."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Error message ──
            errorMsg?.let { msg ->
                Surface(
                    color = RedAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Text(
                        msg,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Call controls ──
            Row(
                Modifier.fillMaxWidth().padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute toggle
                CallControlButton(
                    icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMicMuted) "Micro coupé" else "Micro",
                    onClick = {
                        isMicMuted = !isMicMuted
                        liveKitSetMicMuted(isMicMuted)
                    }
                )
                // Camera toggle
                CallControlButton(
                    icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label = if (isCameraOff) "Caméra coupée" else "Caméra",
                    onClick = {
                        isCameraOff = !isCameraOff
                        liveKitSetCameraEnabled(!isCameraOff)
                    }
                )
                // Switch camera
                CallControlButton(
                    icon = Icons.Default.Cameraswitch,
                    label = "Inverser",
                    onClick = { liveKitSwitchCamera() }
                )
                // End call
                Surface(
                    onClick = { onEnd() },
                    color = RedAccent,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            color = Color.White.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

private fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}