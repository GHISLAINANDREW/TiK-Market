package com.tik_market.ui.live

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
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
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import com.tik_market.ui.components.CameraPreviewWithFrames
import com.tik_market.ui.components.switchCamera
import com.tik_market.utils.shareText
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamingScreen(
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var isStarting by remember { mutableStateOf(false) }
    var isLive by remember { mutableStateOf(false) }
    var streamTitle by remember { mutableStateOf("Mon Direct Shopping") }
    var streamId by remember { mutableStateOf(0) }
    var viewerCount by remember { mutableStateOf(0) }
    var comments by remember { mutableStateOf<List<ApiLiveComment>>(emptyList()) }
    var chatText by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }

    // Throttle frame uploads: only one upload in flight at a time. If a new
    // frame arrives while the previous upload is still running, it is dropped.
    // This prevents base64 frames from accumulating in memory (which caused OOM).
    var uploadInFlight by remember { mutableStateOf(false) }

    // Polling for stats and comments once live
    LaunchedEffect(isLive, streamId) {
        if (!isLive) return@LaunchedEffect
        while (true) {
            try {
                comments = ApiClient.fetchLiveComments(streamId)
                // Real viewer count from the backend (distinct spectators polling).
                val streams = ApiClient.fetchLiveStreams()
                viewerCount = streams.firstOrNull { it.id == streamId }?.viewerCount ?: viewerCount
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    // End the stream cleanly when leaving the screen (back gesture, etc.)
    // so it does not stay orphaned in the live list.
    // NOTE: keyed on Unit so the effect is only disposed when the screen is
    // actually left. Keying on (isLive, streamId) caused onDispose to fire
    // immediately when isLive flipped to true (it reads the CURRENT state),
    // which stopped the stream right after it started.
    val currentIsLive by rememberUpdatedState(isLive)
    val currentStreamId by rememberUpdatedState(streamId)
    DisposableEffect(Unit) {
        onDispose {
            if (currentIsLive && currentStreamId > 0) {
                scope.launch { ApiClient.stopLiveStream(currentStreamId) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            // 1. Camera Preview (with frame capture when live)
            CameraPreviewWithFrames(
                modifier = Modifier.fillMaxSize(),
                captureEnabled = isLive && streamId > 0,
                onFrame = { frameB64 ->
                    // Upload the captured frame to broadcast to spectators.
                    // Drop the frame if a previous upload is still in flight.
                    if (isLive && streamId > 0 && !uploadInFlight) {
                        uploadInFlight = true
                        scope.launch {
                            try {
                                ApiClient.uploadLiveFrame(streamId, frameB64)
                            } finally {
                                uploadInFlight = false
                            }
                        }
                    }
                }
            )

            // Camera switch button (back <-> front), always visible.
            IconButton(
                onClick = { switchCamera() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Cameraswitch, null, tint = Color.White)
            }

            if (!isLive) {
                // Setup UI
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Préparer votre Live", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        label = { Text("Titre du live") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isStarting = true
                                try {
                                    val resp = ApiClient.startLiveStream(streamTitle, null)
                                    if (resp.success) {
                                        streamId = resp.streamId
                                        isLive = true
                                    } else {
                                        snackbarHostState.showSnackbar("Erreur : ${resp.message}")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Impossible de lancer le direct : ${e.message}")
                                }
                                isStarting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        enabled = !isStarting && streamTitle.isNotBlank()
                    ) {
                        if (isStarting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("LANCER LE DIRECT", fontWeight = FontWeight.Bold)
                    }
                    
                    TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Annuler", color = Color.White)
                    }
                }
            } else {
                // Live UI
                Column(Modifier.fillMaxSize()) {
                    // Header
                    Row(Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = RedAccent, shape = RoundedCornerShape(4.dp)) {
                            Text("EN DIRECT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Surface(color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, null, Modifier.size(14.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("$viewerCount", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        // Mute / unmute toggle
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                null,
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Share the live
                        IconButton(
                            onClick = { com.tik_market.utils.shareText("Regardez mon direct sur TiK-Market !", "Partager le direct") },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                    
                    Spacer(Modifier.weight(1f))
                    
                    // Comments
                    Box(Modifier.height(250.dp).fillMaxWidth().padding(16.dp)) {
                        LazyColumn(reverseLayout = true, modifier = Modifier.fillMaxSize()) {
                            items(comments.reversed()) { comment ->
                                Surface(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(Modifier.padding(8.dp)) {
                                        Text(comment.userName, color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(comment.text, color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Footer
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                scope.launch {
                                    ApiClient.stopLiveStream(streamId)
                                    onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Text("Terminer", color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        // Chat input so the streamer can reply to spectators.
                        OutlinedTextField(
                            value = chatText,
                            onValueChange = { chatText = it },
                            placeholder = { Text("Répondre au chat...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Green,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val text = chatText.trim()
                                if (text.isNotEmpty()) {
                                    scope.launch {
                                        ApiClient.postLiveComment(streamId, text)
                                        chatText = ""
                                        comments = ApiClient.fetchLiveComments(streamId)
                                    }
                                }
                            },
                            enabled = chatText.isNotBlank(),
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Send, null, tint = if (chatText.isNotBlank()) GreenAccent else Color.White.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
