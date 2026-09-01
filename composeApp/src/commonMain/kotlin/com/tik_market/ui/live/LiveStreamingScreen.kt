package com.tik_market.ui.live

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStreamingScreen(
    onBack: () -> Unit
) {
    var isStarting by remember { mutableStateOf(false) }
    var isLive by remember { mutableStateOf(false) }
    var streamTitle by remember { mutableStateOf("Mon Direct Shopping") }
    var streamId by remember { mutableStateOf(0) }
    var viewerCount by remember { mutableStateOf(0) }
    var comments by remember { mutableStateOf<List<ApiLiveComment>>(emptyList()) }
    
    val scope = rememberCoroutineScope()

    // Polling for stats and comments once live
    LaunchedEffect(isLive, streamId) {
        if (!isLive) return@LaunchedEffect
        while (true) {
            try {
                // In real app, fetch viewer count and new comments
                comments = ApiClient.fetchLiveComments(streamId)
                viewerCount = (viewerCount + (1..5).random()).coerceAtMost(1000)
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Camera Preview (Placeholder)
        Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Videocam, null, Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Text("Aperçu Caméra", color = Color.White.copy(alpha = 0.5f))
            }
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
                                }
                            } catch (_: Exception) {}
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
                    Surface(
                        modifier = Modifier.weight(1f).height(44.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text("Le chat est actif", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
