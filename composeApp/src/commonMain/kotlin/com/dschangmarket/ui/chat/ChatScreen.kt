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
import androidx.compose.foundation.text.ClickableText
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
import com.dschangmarket.api.ApiMessageReaction
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
    val duration: Int = 0,
    val timestamp: String = "",
    val isRead: Boolean = false,
    val productId: Int? = null,
    val productTitle: String? = null,
    val productImageUrl: String? = null,
    val repliedToId: Int? = null,
    val repliedText: String? = null,
    val reactions: List<ApiMessageReaction> = emptyList()
)

data class ProductShare(
    val title: String,
    val imageUrl: String? = null,
    val price: String = "",
    val shopName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    vendorName: String = "Vendeur",
    vendorAvatar: String? = null,
    productTitle: String? = null,
    productImage: String? = null,
    productPrice: String? = null,
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var replyToMsg by remember { mutableStateOf<ChatMessage?>(null) }

    // Product share message (WhatsApp style — appears as first message)
    val sharedProduct = remember(productTitle) {
        if (productTitle != null) ProductShare(
            title = productTitle,
            imageUrl = productImage,
            price = productPrice ?: "",
            shopName = vendorName
        ) else null
    }
    var productSentToApi by remember { mutableStateOf(false) }

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
                        isRead = msg.isRead,
                        productId = msg.productId,
                        productTitle = msg.productTitle,
                        productImageUrl = msg.productImageUrl,
                        repliedToId = msg.repliedToId,
                        repliedText = msg.repliedText,
                        reactions = msg.reactions
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

    fun sendMessage(text: String, dataUrl: String? = null, duration: Int = 0) {
        scope.launch {
            try {
                if (dataUrl != null || text.isNotBlank()) {
                    val tempMsg = ChatMessage(
                        id = -(kotlin.random.Random.nextLong()).toInt(),
                        senderId = currentUserId,
                        senderName = ApiClient.getCurrentUser()?.name ?: "Moi",
                        text = text,
                        audioUrl = dataUrl,
                        duration = duration,
                        timestamp = "Maintenant",
                        isRead = true,
                        repliedToId = replyToMsg?.id,
                        repliedText = replyToMsg?.text
                    )
                    messages = messages + tempMsg
                    scope.launch {
                        delay(100)
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
                if (dataUrl != null) {
                    ApiClient.sendMessage(vendorId, text, dataUrl, duration, repliedToId = replyToMsg?.id)
                } else {
                    ApiClient.sendMessage(vendorId, text.trim(), repliedToId = replyToMsg?.id)
                }
                messageText = ""
                replyToMsg = null
                loadMessages()
            } catch (_: Exception) {
                messageText = ""
                replyToMsg = null
                loadMessages()
            }
        }
    }

    // Send product as message on first load
    LaunchedEffect(sharedProduct, vendorId) {
        if (sharedProduct != null && !productSentToApi) {
            val msg = "🛍️ ${sharedProduct.title}\n💰 ${sharedProduct.price}\n🏪 ${sharedProduct.shopName}"
            try {
                ApiClient.sendMessage(
                    receiverId = vendorId,
                    text = msg.trim(),
                    productId = null,
                    productImageUrl = sharedProduct.imageUrl
                )
                productSentToApi = true
                loadMessages()
            } catch (_: Exception) {}
        }
    }

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

    var showLocationDialog by remember { mutableStateOf(false) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var locationName by remember { mutableStateOf("") }

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

    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        val prev = prevMessageCount
        prevMessageCount = messages.size
        if (prev < messages.size) {
            delay(200)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            loadMessages()
        }
    }

    // ── WhatsApp-style top bar ──
    Scaffold(
        topBar = {
            Surface(shadowElevation = 1.dp) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar (circle like WhatsApp)
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = GreenSurface
                            ) {
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
                                        Text(vendorName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Green, fontSize = 18.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(vendorName, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(7.dp).background(if (vendorIsOnline) Color(0xFF25D366) else Color.Gray, CircleShape))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        if (vendorIsOnline) "En ligne" else "Hors ligne",
                                        fontSize = 12.sp,
                                        color = if (vendorIsOnline) Color(0xFF25D366) else Color.Gray
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Outlined.MoreVert, null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF075E54),
                        titleContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(Color(0xFFECE5DD))
        ) {
            Column(Modifier.fillMaxSize()) {
                // ── Messages list ──
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                    state = listState
                ) {
                    item { Spacer(Modifier.height(12.dp)) }

                    messages.forEachIndexed { index, msg ->
                        val showDate = index == 0 ||
                            msg.timestamp.substringBefore(" ") != messages[index - 1].timestamp.substringBefore(" ")
                        if (showDate) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        color = Color(0xFFE1F3FB).copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = 0.dp
                                    ) {
                                        Text(
                                            formatDateHeader(msg.timestamp),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            color = Color(0xFF1C1C1C).copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            val isMe = msg.senderId == currentUserId
                            ChatBubble(
                                msg, isMe,
                                onImageClick = { url -> previewImageUrl = url },
                                onDeleteRequest = { m -> deleteTargetMsg = m; showDeleteConfirm = true },
                                onReply = { m -> replyToMsg = m },
                                onReact = { messageId, emoji ->
                                    scope.launch {
                                        try {
                                            ApiClient.addReaction(messageId, emoji)
                                            loadMessages()
                                        } catch (_: Exception) {}
                                    }
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Reply preview bar ──
                AnimatedVisibility(visible = replyToMsg != null) {
                    Surface(shadowElevation = 2.dp, color = Color(0xFFE8E8E8)) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(3.dp).height(28.dp).background(Color(0xFF075E54), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (replyToMsg?.senderId == currentUserId) "Vous" else replyToMsg?.senderName ?: "",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075E54)
                                )
                                Text(
                                    replyToMsg?.text?.let { if (it.length > 60) it.take(60) + "..." else it } ?: "Audio",
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray
                                )
                            }
                            IconButton(onClick = { replyToMsg = null }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // ── Input bar (WhatsApp-style) ──
                Surface(
                    shadowElevation = 4.dp,
                    color = Color(0xFFF0F0F0)
                ) {
                    Column(Modifier.navigationBarsPadding()) {
                        // Emoji Picker
                        AnimatedVisibility(visible = showEmojiPicker) {
                            EmojiPicker(onEmojiSelect = { messageText += it })
                        }

                        // Plus Menu
                        AnimatedVisibility(visible = showPlusMenu) {
                            PlusMenu(
                                onAction = { action ->
                                    showPlusMenu = false
                                    when (action) {
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

                        // ── Main input row ──
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecording) {
                                // ── WhatsApp-style recording bar ──
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .background(Color(0xFFFEFEFE), RoundedCornerShape(24.dp))
                                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pulsing red dot
                                    val infiniteTransition = rememberInfiniteTransition()
                                    val dotAlpha by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0.3f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(600),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = dotAlpha))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        formatDuration(recordingTime),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red,
                                        fontSize = 16.sp
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    // Live waveform bars
                                    Row(
                                        Modifier.weight(1f).height(24.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        val barHeights = listOf(6, 12, 8, 18, 10, 22, 14, 20, 8, 16, 10, 14)
                                        barHeights.forEachIndexed { i, h ->
                                            val barAlpha by animateFloatAsState(
                                                targetValue = if ((recordingTime * 3 + i) % 12 < 6) 0.4f else 1f,
                                                animationSpec = tween(300)
                                            )
                                            Box(
                                                Modifier
                                                    .width(3.dp)
                                                    .height((h * (0.8f + kotlin.math.sin((recordingTime.toFloat() + i) * 0.5f).coerceAtLeast(0f) * 0.4f)).dp)
                                                    .background(Color.Red.copy(alpha = barAlpha), RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "⬆️",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                // Plus button
                                InputToolIcon(Icons.Outlined.AddCircle) { showPlusMenu = !showPlusMenu; showEmojiPicker = false }
                                Spacer(Modifier.width(4.dp))
                                // Emoji button
                                InputToolIcon(Icons.Outlined.EmojiEmotions) { showEmojiPicker = !showEmojiPicker; showPlusMenu = false }
                                Spacer(Modifier.width(4.dp))
                                // Text field (WhatsApp-style rounded)
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Message", color = Color.Gray, fontSize = 16.sp) },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    maxLines = 5,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                                )
                            }

                            Spacer(Modifier.width(6.dp))

                            // ── Send / Voice button (WhatsApp style) ──
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color(0xFFE53935) else Color(0xFF075E54))
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
                                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    } else if (isRecording) {
                                        Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    } else {
                                        Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-screen image preview
    previewImageUrl?.let { url ->
        FullScreenImagePreview(url = url, onDismiss = { previewImageUrl = null })
    }

    // Delete message confirmation
    if (showDeleteConfirm && deleteTargetMsg != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; deleteTargetMsg = null },
            title = { Text("Supprimer le message") },
            text = { Text("Voulez-vous vraiment supprimer ce message ?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    onClick = {
                        scope.launch {
                            try {
                                ApiClient.deleteMessage(deleteTargetMsg!!.id)
                            } catch (_: Exception) {}
                            showDeleteConfirm = false
                            deleteTargetMsg = null
                            loadMessages()
                        }
                    }
                ) {
                    Text("Supprimer", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false; deleteTargetMsg = null }) {
                    Text("Annuler")
                }
            }
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
                    "📍 Ma position : $locationName\nhttps://www.google.com/maps?q=${locationLat},${locationLng}"
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

// ── WhatsApp-style Chat Bubble ──
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChatBubble(
    msg: ChatMessage, isMe: Boolean,
    onImageClick: (String) -> Unit = {},
    onDeleteRequest: (ChatMessage) -> Unit = {},
    onReply: (ChatMessage) -> Unit = {},
    onReact: (Int, String) -> Unit = { _, _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    val quickEmojis = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            // Sender avatar (small, like WhatsApp)
            Surface(
                modifier = Modifier.size(30.dp),
                shape = CircleShape,
                color = GreenSurface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(msg.senderName.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green)
                }
            }
            Spacer(Modifier.width(6.dp))
        }

        // Bubble container
        val bubbleColor = if (isMe) Color(0xFFDCF8C6) else Color.White
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isMe) 12.dp else 4.dp,
                    topEnd = if (isMe) 4.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                ),
                color = bubbleColor,
                shadowElevation = 0.5.dp
                ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showContextMenu = true }
                        )
                ) {
                    // ── Replied-to message preview ──
                    if (msg.repliedToId != null && !msg.repliedText.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (isMe) Color(0xFF075E54).copy(alpha = 0.1f) else Color(0xFFE8E8E8)
                        ) {
                            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(3.dp).height(24.dp).background(Color(0xFF25D366), RoundedCornerShape(2.dp)))
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text("Vous" /*will be fixed*/, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
                                    Text(msg.repliedText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isMe) Color(0xFF1C1C1C).copy(alpha = 0.7f) else Color.Gray)
                                }
                            }
                        }
                    }
                    // Story/Product Reference (Reply context)
                    if (msg.productId != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = Color.Black.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerticalDivider(color = Orange, thickness = 3.dp, modifier = Modifier.height(30.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Story", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Orange)
                                    Text(msg.productTitle ?: "Produit", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                // Product image thumbnail in chat bubble
                                if (!msg.productImageUrl.isNullOrBlank()) {
                                    var thumbBitmap by remember(msg.productImageUrl) {
                                        mutableStateOf<ImageBitmap?>(null)
                                    }
                                    LaunchedEffect(msg.productImageUrl) {
                                        val cleanUrl = if (msg.productImageUrl.startsWith("http"))
                                            msg.productImageUrl
                                        else
                                            "${ApiClient.baseUrl.trimEnd('/')}/${msg.productImageUrl.trimStart('/')}"
                                        thumbBitmap = try {
                                            loadImageFromUrl(cleanUrl)
                                        } catch (_: Exception) { null }
                                    }
                                    if (thumbBitmap != null) {
                                        Image(
                                            bitmap = thumbBitmap!!,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            Modifier.size(36.dp)
                                                .background(Color.LightGray, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, null, Modifier.size(18.dp), tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Content
                    if (msg.audioUrl != null) {
                        val url = msg.audioUrl
                        val urlLower = url.lowercase()
                        val isImage = urlLower.let {
                            it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png")
                                || it.endsWith(".gif") || it.endsWith(".webp") || it.endsWith(".bmp")
                                || msg.text == "[Image]" || msg.text == "[Photo]"
                        }
                        val isAudio = urlLower.let {
                            it.endsWith(".mp4") || it.endsWith(".webm") || it.endsWith(".wav")
                                || it.endsWith(".mp3") || it.endsWith(".ogg") || msg.duration > 0
                        }
                        when {
                            isImage -> ChatMessageImage(url, onClick = { onImageClick(url) })
                            isAudio -> VoiceMessageLayout(url, msg.duration, isMe)
                            else -> FileMessageLayout(url, isMe)
                        }
                    } else {
                        if (msg.text.startsWith("📍")) {
                            // Location message
                            Column {
                                val lines = msg.text.split("\n")
                                Text(lines[0], fontSize = 15.sp, color = if (isMe) Color(0xFF1C1C1C) else Color.Black, lineHeight = 21.sp)
                                Spacer(Modifier.height(4.dp))
                                val mapsUrl = lines.find { it.startsWith("https://www.google.com/maps") }
                                if (mapsUrl != null) {
                                    OutlinedButton(
                                        onClick = { openUrl(mapsUrl) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34B7F1)),
                                        border = BorderStroke(1.dp, Color(0xFF34B7F1).copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Map, null, Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Ouvrir dans Maps", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            Text(msg.text, fontSize = 15.sp, color = if (isMe) Color(0xFF1C1C1C) else Color.Black, lineHeight = 21.sp)
                        }
                    }

                    // Timestamp + Read status row
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatOnlyTime(msg.timestamp),
                            fontSize = 11.sp,
                            color = if (isMe) Color(0xFF1C1C1C).copy(alpha = 0.55f) else Color.Gray
                        )
                        Spacer(Modifier.width(3.dp))
                        if (isMe) {
                            Icon(
                                if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                null,
                                Modifier.size(16.dp),
                                tint = if (msg.isRead) Color(0xFF53BDEB) else Color(0xFF1C1C1C).copy(alpha = 0.45f)
                            )
                        }
                    }

                    // ── Reactions row ──
                    if (msg.reactions.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.align(if (isMe) Alignment.End else Alignment.Start),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            msg.reactions.forEach { reaction ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFF3E0),
                                    shadowElevation = 0.5.dp,
                                    border = BorderStroke(0.5.dp, Color(0xFFFFCC80).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(reaction.emoji, fontSize = 12.sp)
                                        if (reaction.count > 1) {
                                            Spacer(Modifier.width(2.dp))
                                            Text(reaction.count.toString(), fontSize = 10.sp, color = Color(0xFF795548))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Context menu (Reply / React / Delete) ──
                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Répondre") },
                        onClick = { showContextMenu = false; onReply(msg) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Réagir") },
                        onClick = { showContextMenu = false; showReactionPicker = true },
                        leadingIcon = { Icon(Icons.Outlined.EmojiEmotions, null, Modifier.size(18.dp)) }
                    )
                    if (isMe) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Supprimer", color = Color(0xFFE53935)) },
                            onClick = { showContextMenu = false; onDeleteRequest(msg) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color(0xFFE53935)) }
                        )
                    }
                }

                // ── Quick Reaction Picker (popup) ──
                if (showReactionPicker) {
                    AlertDialog(
                        onDismissRequest = { showReactionPicker = false },
                        title = { Text("Réagir au message") },
                        text = {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                quickEmojis.forEach { emoji ->
                                    Text(
                                        emoji, fontSize = 28.sp,
                                        modifier = Modifier
                                            .clickable {
                                                onReact(msg.id, emoji)
                                                showReactionPicker = false
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showReactionPicker = false }) { Text("Annuler") }
                        }
                    )
                }
            }
        }
    }
}

// ── Voice Message Layout (WhatsApp style) ──
@Composable
private fun VoiceMessageLayout(audioUrl: String, duration: Int, isMe: Boolean) {
    var isPlaying by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val fgColor = if (isMe) Color(0xFF1C1C1C) else Color(0xFF075E54)
    val accentColor = Color(0xFF075E54)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(220.dp).padding(vertical = 2.dp)
    ) {
        // Play/Pause button (WhatsApp green circle)
        Surface(
            onClick = {
                if (isPlaying) {
                    isPlaying = false
                    // We don't have a stopAudio yet, but we could add one.
                    // For now, re-playing another one stops the previous.
                } else {
                    scope.launch {
                        stopAudio() // Stop any previous playback
                        if (!isMe) {
                            isDownloading = true
                            delay(800) // Visual feedback
                            isDownloading = false
                        }
                        isPlaying = true
                        playAudio(
                            url = audioUrl,
                            onProgress = { progress = it },
                            onCompletion = { 
                                isPlaying = false
                                progress = 0f 
                            }
                        )
                    }
                }
            },
            shape = CircleShape,
            color = if (isPlaying) accentColor.copy(alpha = 0.15f) else accentColor,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = if (isPlaying) accentColor else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Waveform + progress
        Column(Modifier.weight(1f)) {
            // Waveform bars
            Row(
                Modifier.fillMaxWidth().height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Generate realistic-looking voice waveform
                val bars = listOf(
                    6, 10, 14, 18, 22, 26, 30, 24, 20, 16, 12, 8,
                    12, 18, 24, 28, 32, 28, 22, 16, 10, 14, 20, 26,
                    30, 34, 30, 24, 18, 12, 8, 14, 20, 26, 32, 28,
                    22, 16, 10, 6, 10, 16, 22, 28, 24, 18, 12, 8
                )
                val totalBars = bars.size
                bars.forEachIndexed { i, h ->
                    val barProgress = i.toFloat() / totalBars
                    val isPlayed = barProgress <= progress
                    Bar(
                        height = h,
                        color = if (isPlayed) accentColor else if (isMe) fgColor.copy(alpha = 0.25f) else Color.LightGray,
                        animated = isPlaying && barProgress <= progress + 0.05f
                    )
                }
            }

            // Duration text below waveform
            Text(
                formatDuration(duration),
                fontSize = 11.sp,
                color = if (isMe) fgColor.copy(alpha = 0.6f) else Color.Gray
            )
        }

        Spacer(Modifier.width(8.dp))

        // Duration on far right (like WhatsApp)
        Text(
            formatDuration(duration),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isMe) fgColor.copy(alpha = 0.7f) else Color.Gray
        )
    }
}

@Composable
private fun Bar(height: Int, color: Color, animated: Boolean) {
    val h by animateDpAsState(
        targetValue = if (animated) (height + 8).dp else height.dp,
        animationSpec = tween(150)
    )
    Box(
        Modifier
            .width(3.dp)
            .height(h)
            .background(color, RoundedCornerShape(2.dp))
    )
}

// ── Image Message ──
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
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 260.dp)
                .heightIn(max = 200.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() },
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 260.dp)
                .height(150.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF075E54))
        }
    }
}

// ── File Message ──
@Composable
private fun FileMessageLayout(url: String, isMe: Boolean) {
    val urlLower = url.lowercase()
    val fileName = url.substringAfterLast("/")
    val ext = fileName.substringAfterLast(".").ifBlank { "bin" }
    val fgColor = if (isMe) Color(0xFF1C1C1C) else Color.Black

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
        color = if (isMe) Color(0xFFDCF8C6).copy(alpha = 0.5f) else Color(0xFFF5F5F5),
        border = BorderStroke(0.5.dp, if (isMe) Color.Transparent else Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(8.dp).widthIn(max = 220.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fgColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(fileName, fontSize = 10.sp, color = fgColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.Download, null, tint = Color(0xFF34B7F1), modifier = Modifier.size(18.dp))
        }
    }
}

// ── Full-screen Image Preview ──
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onDismiss() }
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

// ── Emoji Picker ──
@Composable
private fun EmojiPicker(onEmojiSelect: (String) -> Unit) {
    val categories = listOf(
        listOf("\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83D\uDE0E", "\uD83D\uDE18", "\uD83D\uDE1C", "\uD83D\uDE0A", "\uD83D\uDE42"),
        listOf("\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDCAF", "\uD83D\uDCE6", "\uD83D\uDCB0", "\uD83D\uDCCD", "\uD83D\uDD25"),
        listOf("\uD83D\uDE97", "\uD83D\uDEEB", "\uD83C\uDFE0", "\uD83C\uDF4E", "\uD83C\uDF55", "\u2615", "\u26BD", "\uD83C\uDFA7")
    )
    Column(
        Modifier.fillMaxWidth().height(140.dp).background(Color(0xFFF0F0F0)).padding(8.dp)
    ) {
        categories.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { emoji ->
                    Text(
                        emoji, fontSize = 28.sp,
                        modifier = Modifier.clickable { onEmojiSelect(emoji) }.padding(4.dp)
                    )
                }
            }
        }
    }
}

// ── Plus Menu (WhatsApp attachment style) ──
@Composable
private fun PlusMenu(onAction: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
        Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun InputToolIcon(icon: ImageVector, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(icon, null, tint = Color(0xFF1C1C1C).copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
    }
}

// ── Location Preview Dialog ──
@Composable
private fun LocationPreviewDialog(
    lat: Double, lng: Double, placeName: String,
    onConfirm: () -> Unit, onOpenInMaps: () -> Unit, onDismiss: () -> Unit
) {
    var mapBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var loadingMap by remember { mutableStateOf(true) }

    LaunchedEffect(lat, lng) {
        val url = "https://staticmap.openstreetmap.de/staticmap.php?" +
            "center=$lat,$lng&zoom=15&size=600x400&markers=$lat,$lng"
        mapBitmap = try { loadImageFromUrl(url) } catch (_: Exception) { null }
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
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF1a1a2e)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loadingMap) {
                        CircularProgressIndicator(Modifier.size(32.dp), color = Color(0xFF6C63FF))
                    } else {
                        mapBitmap?.let { bmp ->
                            Image(bitmap = bmp, contentDescription = "Carte",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(placeName.ifEmpty { "Position actuelle" },
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Lat: ${"%.6f".latLngFormat(lat)}  Lng: ${"%.6f".latLngFormat(lng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenInMaps, modifier = Modifier.weight(1f)) {
                        Text("🗺️ Carte")
                    }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))) {
                        Text("Envoyer")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

// ── Utility functions ──
private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "${mins}:${secs.toString().padStart(2, '0')}"
}

private fun formatDateHeader(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return dateStr.substringBefore(" ")
}

private fun formatOnlyTime(dateStr: String): String {
    return if (dateStr.contains(" ")) dateStr.substringAfter(" ").take(5)
    else if (dateStr == "Maintenant") "Maintenant" else dateStr
}

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
