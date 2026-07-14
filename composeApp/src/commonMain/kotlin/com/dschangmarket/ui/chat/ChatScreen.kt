package com.dschangmarket.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.rememberImagePickerLauncher
import com.dschangmarket.ui.components.rememberTakePhotoLauncher
import com.dschangmarket.ui.components.rememberPickFileLauncher
import com.dschangmarket.ui.components.loadImageFromUrl
import com.dschangmarket.utils.getCurrentLocationLatLng
import com.dschangmarket.utils.getCurrentLocationName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Int = 0,
    val senderId: Int = 0,
    val senderName: String = "",
    val text: String = "",
    val audioUrl: String? = null,
    val duration: Int = 0, // in seconds
    val timestamp: String = "",
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    vendorName: String = "Vendeur",
    vendorAvatar: String? = null,
    productTitle: String? = null,
    vendorId: Int = 0,
    vendorIsOnline: Boolean = false
) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentUserId = remember { ApiClient.getCurrentUser()?.id ?: 0 }

    var showPlusMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Load messages
    fun loadMessages() {
        scope.launch {
            try {
                val apiMessages = ApiClient.fetchMessages(vendorId)
                val newMessages = apiMessages.map { msg ->
                    ChatMessage(
                        id = msg.id,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        text = msg.text,
                        audioUrl = msg.audioUrl,
                        duration = msg.duration,
                        timestamp = msg.createdAt,
                        isRead = msg.isRead
                    )
                }
                
                if (newMessages.size != messages.size || newMessages.lastOrNull()?.id != messages.lastOrNull()?.id) {
                    if (messages.isNotEmpty() && newMessages.size > messages.size) {
                        val lastNew = newMessages.last()
                        if (lastNew.senderId != currentUserId) {
                            playChatSound()
                        }
                    }
                    messages = newMessages
                    if (messages.isNotEmpty()) {
                        scope.launch {
                            delay(100)
                            listState.animateScrollToItem(messages.size) 
                        }
                    }
                    if (newMessages.lastOrNull()?.senderId != currentUserId) {
                        ApiClient.markMessagesAsRead(vendorId)
                    }
                }
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Helper to send a message with optional audio/image/file data
    fun sendMessage(text: String, dataUrl: String? = null, duration: Int = 0) {
        scope.launch {
            try {
                // Optimistic UI: add to local list immediately for instant feedback
                if (dataUrl != null || text.isNotBlank()) {
                    val tempMsg = ChatMessage(
                        id = -System.currentTimeMillis().toInt(), // temporary negative ID
                        senderId = currentUserId,
                        senderName = ApiClient.getCurrentUser()?.name ?: "Moi",
                        text = text,
                        audioUrl = dataUrl,
                        duration = duration,
                        timestamp = "Maintenant",
                        isRead = true
                    )
                    messages = messages + tempMsg
                    // Scroll to bottom
                    scope.launch {
                        delay(100)
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
                
                // Send to API
                if (dataUrl != null) {
                    ApiClient.sendMessage(vendorId, text, dataUrl, duration)
                } else {
                    ApiClient.sendMessage(vendorId, text.trim())
                }
                messageText = ""
                // Refresh from server (replaces the optimistic message)
                loadMessages()
            } catch (_: Exception) {
                // If API fails, keep the optimistic message (the user can still play audio from data URL)
                messageText = ""
                loadMessages()
            }
        }
    }

    // Platform-specific picker launchers (declared after sendMessage for forward reference)
    val pickGallery = rememberImagePickerLauncher { result ->
        if (result != null) sendMessage("[Image]", result.dataUrl)
    }
    val pickCamera = rememberTakePhotoLauncher { dataUrl ->
        if (dataUrl != null) sendMessage("[Photo]", dataUrl)
    }
    val pickAnyFile = rememberPickFileLauncher { dataUrl ->
        if (dataUrl != null) sendMessage("[Fichier]", dataUrl)
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0) }

    // Location preview state
    var showLocationDialog by remember { mutableStateOf(false) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var locationName by remember { mutableStateOf("") }
    
    // Timer for recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        }
    }

    var prevMessageCount by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) { loadMessages() }

    // Auto-scroll to bottom only on initial load or when a NEW message arrives
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        val prev = prevMessageCount
        prevMessageCount = messages.size
        // Scroll on first load (prev == -1 or 0) or when count increased (new message)
        if (prev < messages.size) {
            delay(200)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            loadMessages()
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 1.dp) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(8.dp), color = GreenSurface) {
                                Box(contentAlignment = Alignment.Center) {
                                    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                                    LaunchedEffect(vendorAvatar) {
                                        vendorAvatar?.let {
                                            val cleanBase = ApiClient.baseUrl.trimEnd('/')
                                            val cleanPath = it.trimStart('/', '\\').replace("\\", "/")
                                            val finalUrl = if (it.startsWith("http")) it else "$cleanBase/$cleanPath"
                                            avatarBitmap = loadImageFromUrl(finalUrl)
                                        }
                                    }
                                    if (avatarBitmap != null) {
                                        Image(avatarBitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text(vendorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Green, fontSize = 16.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(vendorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(if (vendorIsOnline) Green else Color.Gray, CircleShape))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (vendorIsOnline) "En ligne" else "Hors ligne",
                                        fontSize = 10.sp,
                                        color = if (vendorIsOnline) Green else Color.Gray
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Outlined.Phone, null, tint = Color.White) }
                        IconButton(onClick = {}) { Icon(Icons.Outlined.Info, null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White)
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF4F4F4))) {
            Column(Modifier.fillMaxSize()) {
                // Product banner (Alibaba style)
                if (productTitle != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 1.dp
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(44.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFF9F9F9)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingBag, null, tint = Green, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(productTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Voir les détails du produit", fontSize = 11.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Green)
                            ) {
                                Text("Détails", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Messages
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    state = listState
                ) {
                    item { Spacer(Modifier.height(16.dp)) }
                    
                    messages.forEachIndexed { index, msg ->
                        val showDate = index == 0 || msg.timestamp.substringBefore(" ") != messages[index-1].timestamp.substringBefore(" ")
                        if (showDate) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                    Surface(color = Color(0xFFE0E0E0), shape = RoundedCornerShape(10.dp)) {
                                        Text(formatDateHeader(msg.timestamp), modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        item {
                            val isMe = msg.senderId == currentUserId
                            ChatBubble(msg, isMe, onImageClick = { url -> previewImageUrl = url })
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // Input bar (WhatsApp/Alibaba hybrid)
                Surface(
                    shadowElevation = 12.dp, 
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(Modifier.navigationBarsPadding()) {
                        // Emoji Picker (collapsible)
                        AnimatedVisibility(visible = showEmojiPicker) {
                            EmojiPicker(onEmojiSelect = { 
                                messageText += it
                            })
                        }

                        // Plus Menu (collapsible)
                        AnimatedVisibility(visible = showPlusMenu) {
                            PlusMenu(
                                onAction = { action ->
                                    showPlusMenu = false
                                    when(action) {
                                        "image" -> pickGallery()
                                        "camera" -> pickCamera()
                                        "file" -> pickAnyFile()
                                        "location" -> {
                                            getCurrentLocationLatLng { lat, lng ->
                                                if (lat != null && lng != null) {
                                                    locationLat = lat
                                                    locationLng = lng
                                                    getCurrentLocationName { name ->
                                                        locationName = name
                                                        showLocationDialog = true
                                                    }
                                                } else {
                                                    getCurrentLocationName { loc ->
                                                        sendMessage("📍 Ma position : $loc")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        
                        // Main input row
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecording) {
                                Row(
                                    Modifier.weight(1f).height(52.dp).background(Color(0xFFFDECEA), CircleShape).padding(horizontal = 16.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val alpha by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0.2f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Icon(Icons.Default.Mic, null, tint = Color.Red, modifier = Modifier.size(24.dp).scale(alpha))
                                    Spacer(Modifier.width(12.dp))
                                    Text(formatDuration(recordingTime), fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 18.sp)
                                    Spacer(Modifier.width(16.dp))
                                    Text("Enregistrement...", color = Color.Red.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.weight(1f))
                                    Text("Lâcher pour envoyer", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Plus button
                                InputToolIcon(Icons.Outlined.AddCircleOutline) { showPlusMenu = !showPlusMenu; showEmojiPicker = false }
                                
                                Spacer(Modifier.width(4.dp))
                                
                                // Emoji button (outside the +)
                                InputToolIcon(Icons.Outlined.SentimentSatisfied) { showEmojiPicker = !showEmojiPicker; showPlusMenu = false }
                                
                                Spacer(Modifier.width(4.dp))
                                
                                // Text field
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Tapez votre message...", color = Color.Gray, fontSize = 15.sp) },
                                    shape = RoundedCornerShape(28.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF2F2F2),
                                        unfocusedContainerColor = Color(0xFFF2F2F2),
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    maxLines = 5
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
                            // Action Button (Voice or Send)
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color.Red else Green)
                                    .pointerInput(messageText) {
                                        if (messageText.isBlank()) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { 
                                                    isRecording = true
                                                    startVoiceRecording()
                                                },
                                                onDrag = { _, _ -> },
                                                onDragEnd = {
                                                    if (isRecording) {
                                                        isRecording = false
                                                        stopVoiceRecording { dataUrl, duration ->
                                                            if (dataUrl != null) {
                                                                sendMessage("[Vocal]", dataUrl, duration)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDragCancel = { 
                                                    isRecording = false
                                                    stopVoiceRecording { _, _ -> }
                                                }
                                            )
                                        }
                                    }
                                    .clickable(enabled = messageText.isNotBlank()) {
                                        if (!isSending) {
                                            isSending = true
                                            sendMessage(messageText)
                                            isSending = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(targetState = messageText.isNotBlank() || isRecording) { isAction ->
                                    if (isAction && !isRecording) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
    
    // Full-screen image preview
    previewImageUrl?.let { url ->
        FullScreenImagePreview(
            url = url,
            onDismiss = { previewImageUrl = null }
        )
    }

    // Location preview dialog
    if (showLocationDialog && locationLat != null && locationLng != null) {
        LocationPreviewDialog(
            lat = locationLat!!,
            lng = locationLng!!,
            placeName = locationName,
            onConfirm = {
                sendMessage(
                    "📍 Ma position : $locationName\n" +
                    "https://www.google.com/maps?q=${locationLat},${locationLng}"
                )
                showLocationDialog = false
            },
            onOpenInMaps = {
                openUrl("https://www.google.com/maps?q=${locationLat},${locationLng}")
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }
}

@Composable
private fun LocationPreviewDialog(
    lat: Double,
    lng: Double,
    placeName: String,
    onConfirm: () -> Unit,
    onOpenInMaps: () -> Unit,
    onDismiss: () -> Unit
) {
    var mapBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loadingMap by remember { mutableStateOf(true) }

    // Load static map from OpenStreetMap
    LaunchedEffect(lat, lng) {
        val url = "https://staticmap.openstreetmap.de/staticmap.php?" +
            "center=$lat,$lng&zoom=15&size=600x400&markers=$lat,$lng"
        mapBitmap = try {
            loadImageFromUrl(url)
        } catch (_: Exception) {
            null
        }
        loadingMap = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📍 Partager ma position") },
        text = {
            Column(
                modifier = Modifier.width(300.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Static map image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1a1a2e)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadingMap) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF6C63FF)
                        )
                    } else {
                        mapBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp,
                                contentDescription = "Carte",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Place name
                Text(
                    text = placeName.ifEmpty { "Position actuelle" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // Coordinates
                Text(
                    text = "Lat: ${"%.6f".latLngFormat(lat)}  Lng: ${"%.6f".latLngFormat(lng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Open in Maps
                    OutlinedButton(
                        onClick = onOpenInMaps,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗺️ Carte")
                    }
                    // Send
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF)
                        )
                    ) {
                        Text("Envoyer")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun FullScreenImagePreview(url: String, onDismiss: () -> Unit) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(url) {
        if (url.startsWith("data:")) {
            bitmap = com.dschangmarket.ui.components.decodeDataUrlToImageBitmap(url)
        } else {
            try {
                val dataUrl = com.dschangmarket.ui.components.fetchImageAsDataUrl(url)
                if (dataUrl != null) {
                    bitmap = com.dschangmarket.ui.components.decodeDataUrlToImageBitmap(dataUrl)
                }
            } catch (_: Exception) {}
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
        
        // Image
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable(enabled = false) { /* prevent dismiss on image click — let background handle it */ },
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun EmojiPicker(onEmojiSelect: (String) -> Unit) {
    val categories = listOf(
        listOf("\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE0E", "\uD83D\uDE18", "\uD83D\uDE1C", "\uD83D\uDE0A", "\uD83D\uDE42"),
        listOf("\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDCAF", "\uD83D\uDCE6", "\uD83D\uDCB0", "\uD83D\uDCCD", "\uD83D\uDD25"),
        listOf("\uD83D\uDE97", "\uD83D\uDEEB", "\uD83C\uDFE0", "\uD83C\uDF4E", "\uD83C\uDF55", "\u2615", "\u26BD", "\uD83C\uDFA7")
    )
    Column(
        Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF8F8F8)).padding(8.dp)
    ) {
        categories.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { emoji ->
                    Text(
                        emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.clickable { onEmojiSelect(emoji) }.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlusMenu(onAction: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PlusMenuItem(Icons.Default.Image, "Galerie", Color(0xFF4CAF50)) { onAction("image") }
        PlusMenuItem(Icons.Default.CameraAlt, "Caméra", Color(0xFF2196F3)) { onAction("camera") }
        PlusMenuItem(Icons.Default.AttachFile, "Fichier", Color(0xFF9C27B0)) { onAction("file") }
        PlusMenuItem(Icons.Default.LocationOn, "Localisation", Color(0xFFFF5722)) { onAction("location") }
    }
}

@Composable
private fun PlusMenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(50.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun InputToolIcon(icon: ImageVector, onClick: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, isMe: Boolean, onImageClick: (String) -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(8.dp), color = GreenSurface) {
                Box(contentAlignment = Alignment.Center) {
                    Text(msg.senderName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 16.dp
            ),
            color = if (isMe) Green else Color.White,
            shadowElevation = 0.5.dp,
            border = if (!isMe) BorderStroke(0.5.dp, Color(0xFFE0E0E0)) else null
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).widthIn(max = 250.dp)) {
                if (msg.audioUrl != null) {
                    val url = msg.audioUrl
                    val urlLower = url.lowercase()
                    val isImage = urlLower.let {
                        it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".gif") || it.endsWith(".webp") || it.endsWith(".bmp") || msg.text == "[Image]" || msg.text == "[Photo]"
                    }
                    val isAudio = urlLower.let {
                        it.endsWith(".mp4") || it.endsWith(".webm") || it.endsWith(".wav") || it.endsWith(".mp3") || it.endsWith(".ogg") || msg.duration > 0
                    }
                    when {
                        isImage -> ChatMessageImage(url, onClick = { onImageClick(url) })
                        isAudio -> VoiceMessageLayout(url, msg.duration, isMe)
                        else -> FileMessageLayout(url, isMe)
                    }
                } else {
                    if (msg.text.startsWith("📍")) {
                        // Location message — show with map button
                        Column {
                            // First line: the location description
                            val lines = msg.text.split("\n")
                            Text(lines[0], fontSize = 15.sp, color = if (isMe) Color.White else Color.Black, lineHeight = 21.sp)
                            Spacer(Modifier.height(6.dp))
                            // If there's a Maps URL, show as a button
                            val mapsUrl = lines.find { it.startsWith("https://www.google.com/maps") }
                            if (mapsUrl != null) {
                                OutlinedButton(
                                    onClick = { openUrl(mapsUrl) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isMe) Color.White else Color(0xFF4285F4)),
                                    border = BorderStroke(1.dp, if (isMe) Color.White.copy(alpha = 0.5f) else Color(0xFF4285F4))
                                ) {
                                    Icon(Icons.Default.Map, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ouvrir dans Maps", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text(msg.text, fontSize = 15.sp, color = if (isMe) Color.White else Color.Black, lineHeight = 21.sp)
                    }
                }
                
                Row(modifier = Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatOnlyTime(msg.timestamp), fontSize = 10.sp, color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray)
                    if (isMe) {
                        Spacer(Modifier.width(4.dp))
                        Icon(if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done, null, Modifier.size(14.dp), tint = if (msg.isRead) Amber else Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageImage(url: String, onClick: () -> Unit = {}) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(url) {
        if (url.startsWith("data:")) {
            bitmap = com.dschangmarket.ui.components.decodeDataUrlToImageBitmap(url)
        } else {
            try {
                val dataUrl = com.dschangmarket.ui.components.fetchImageAsDataUrl(url)
                if (dataUrl != null) {
                    bitmap = com.dschangmarket.ui.components.decodeDataUrlToImageBitmap(dataUrl)
                }
            } catch (_: Exception) {}
        }
    }
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)).clickable { onClick() },
            contentScale = ContentScale.Crop
        )
    } else {
        Box(Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray, RoundedCornerShape(8.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Green)
        }
    }
}

@Composable
private fun VoiceMessageLayout(audioUrl: String, duration: Int, isMe: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
            null, 
            tint = if (isMe) Color.White else Green,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable {
                    if (isPlaying) {
                        isPlaying = false
                    } else {
                        isPlaying = true
                        playAudio(audioUrl)
                        // Auto-reset after estimated duration
                        if (duration > 0) {
                            scope.launch {
                                delay((duration * 1000L))
                                isPlaying = false
                            }
                        }
                    }
                }
        )
        Spacer(Modifier.width(8.dp))
        // Simulated Waveform
        Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val heights = listOf(10, 15, 8, 20, 12, 18, 10, 22, 14, 16, 8, 12)
            heights.forEach { h ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(if (isPlaying) (h * 1.3f).dp else h.dp)
                        .background(if (isMe) Color.White.copy(alpha = 0.7f) else Color.LightGray, RoundedCornerShape(1.dp))
                        .animateContentSize(animationSpec = tween(300))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(formatDuration(duration), fontSize = 12.sp, color = if (isMe) Color.White else Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FileMessageLayout(url: String, isMe: Boolean) {
    val urlLower = url.lowercase()
    val fileName = url.substringAfterLast("/")
    val ext = fileName.substringAfterLast(".").ifBlank { "bin" }
    
    // Determine file icon and label
    val (icon, iconColor, label) = when (ext) {
        "pdf" -> Triple(Icons.Default.PictureAsPdf, Color(0xFFE53935), "PDF")
        "doc", "docx" -> Triple(Icons.Default.Description, Color(0xFF1565C0), "Document Word")
        "xls", "xlsx" -> Triple(Icons.Default.TableChart, Color(0xFF2E7D32), "Tableur")
        "ppt", "pptx" -> Triple(Icons.Default.Slideshow, Color(0xFFE65100), "Présentation")
        "zip", "rar", "gz" -> Triple(Icons.Default.Folder, Color(0xFFF9A825), "Archive")
        "txt" -> Triple(Icons.AutoMirrored.Filled.Article, Color(0xFF607D8B), "Texte")
        "vcf" -> Triple(Icons.Default.Contacts, Color(0xFF00ACC1), "Contact")
        "csv" -> Triple(Icons.Default.TableChart, Color(0xFF43A047), "CSV")
        "json" -> Triple(Icons.Default.Code, Color(0xFF5C6BC0), "JSON")
        else -> Triple(Icons.AutoMirrored.Filled.InsertDriveFile, Color.Gray, "Fichier $ext")
    }
    
    Surface(
        onClick = { openUrl(url) },
        shape = RoundedCornerShape(8.dp),
        color = if (isMe) Color.White.copy(alpha = 0.15f) else Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(10.dp).widthIn(max = 220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(36.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isMe) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(fileName, fontSize = 10.sp, color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Download, null, tint = if (isMe) Color.White else Color(0xFF4285F4), modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}

private fun formatDateHeader(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    val date = dateStr.substringBefore(" ")
    return date
}

private fun formatOnlyTime(dateStr: String): String {
    return if (dateStr.contains(" ")) dateStr.substringAfter(" ").take(5) else if (dateStr == "Maintenant") "Maintenant" else dateStr
}

/** Simple double formatter for KMP (String.format not available in common). */
private fun String.latLngFormat(value: Double): String {
    val decimals = if (contains(".")) length - indexOf('.') - 1 else 0
    val factor = when (decimals) { 0 -> 1; 1 -> 10; 2 -> 100; 3 -> 1000; 4 -> 10000; 5 -> 100000; 6 -> 1000000; else -> 1000000 }
    val rounded = kotlin.math.round(value * factor) / factor
    val str = rounded.toString()
    val dot = str.indexOf('.')
    return if (dot >= 0) {
        val have = str.length - dot - 1
        if (have >= decimals) str else str + "0".repeat(decimals - have)
    } else {
        str + "." + "0".repeat(decimals)
    }
}
