package com.tik_market.ui.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.data.models.Product
import com.tik_market.theme.*
import com.tik_market.ui.components.VideoPlayer
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced StoryItem supporting both legacy Product-based stories
 * and new standalone stories from the stories API.
 */
data class StoryItem(
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val product: Product? = null,
    // New fields for standalone stories
    val storyId: Int = 0,
    val shopId: Int = 0,
    val vendorId: Int = 0,
    val mediaType: String = "image", // "image" or "video"
    val caption: String? = null,
    val replyCount: Int = 0,
    val userId: Int = 0,
    val userAvatar: String? = null,
    val shopLogo: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    stories: List<StoryItem>,
    initialIndex: Int = 0,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onReply: ((String, Product) -> Unit)? = null,
    onDeleteStory: ((Product) -> Unit)? = null,
    currentUserId: Int = 0,
    onRefreshStories: () -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val ts = LocalAppStrings.current
    val storyDurationMs = 5000L // 5 seconds per story

    // Auto-advance timer using delay.
    // NOTE: les stories vidéo avancent via onEnded du lecteur vidéo (pas ce timer),
    // sinon une vidéo serait coupée après 5 secondes fixes.
    val currentStoryForTimer = stories.getOrNull(currentIndex)
    LaunchedEffect(currentIndex, isPaused) {
        if (isPaused || stories.isEmpty()) return@LaunchedEffect
        if (currentStoryForTimer?.mediaType == "video") return@LaunchedEffect
        val totalTicks = 100
        val tickMs = storyDurationMs / totalTicks
        for (step in 1..totalTicks) {
            delay(tickMs)
            if (isPaused) return@LaunchedEffect
            progress = step.toFloat() / totalTicks
        }
        if (currentIndex < stories.lastIndex) {
            currentIndex++
            progress = 0f
        } else {
            onBack()
        }
    }

    if (stories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(ts.noStory, color = Color.White, fontSize = 18.sp)
        }
        return
    }

    val currentStory = stories.getOrNull(currentIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Story image content
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (currentStory.mediaType == "text") {
                val bgColor = when (currentStory.imageUrl) {
                    "#4CAF50" -> Green
                    "#FF9800" -> Orange
                    "#2196F3" -> BlueAccent
                    "#F44336" -> RedAccent
                    else -> Color(0xFF333333)
                }
                Box(
                    Modifier.fillMaxSize().background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentStory.caption ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                        lineHeight = 36.sp
                    )
                }
            } else if (currentStory.mediaType == "video") {
                // Use native video player for video stories
                VideoPlayer(
                    url = currentStory.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    isPlaying = !isPaused,
                    onEnded = {
                        if (currentIndex < stories.lastIndex) {
                            currentIndex++
                            progress = 0f
                        } else {
                            onBack()
                        }
                    }
                )
            } else if (currentStory.imageUrl.isNotBlank()) {
                var bitmap by remember(currentIndex, currentStory.imageUrl) {
                    mutableStateOf<ImageBitmap?>(null)
                }
                LaunchedEffect(currentStory.imageUrl) {
                    bitmap = loadImageFromUrl(currentStory.imageUrl)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!,
                        contentDescription = currentStory.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Green),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentStory.title.take(2).uppercase(),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize().background(GreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            currentStory.title.take(2).uppercase(),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            currentStory.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Touch areas for navigation (left=prev, right=next)
        Box(Modifier.fillMaxSize().clickable { isPaused = !isPaused })

        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .clickable(enabled = currentIndex > 0) {
                        currentIndex--
                        progress = 0f
                    }
            )
            Spacer(Modifier.weight(0.4f))
            Box(
                Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .clickable(enabled = currentIndex < stories.lastIndex) {
                        currentIndex++
                        progress = 0f
                    }
            )
        }

        Column(Modifier.fillMaxSize()) {
            // ── Progress bars ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        if (index == currentIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        } else if (index < currentIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // ── Header (avatar + shop name + caption info + close) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar or initial
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        currentStory.title.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        currentStory.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (currentStory.subtitle.isNotBlank()) {
                        Text(
                            currentStory.subtitle,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Delete button (owner only)
                val isOwner = currentUserId > 0 && (
                    currentStory.userId == currentUserId ||
                    (currentStory.product?.vendorId?.toIntOrNull() == currentUserId)
                )
                if (isOwner) {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                if (currentStory.storyId > 0) {
                                    ApiClient.deleteStory(currentStory.storyId)
                                } else if (currentStory.product != null) {
                                    onDeleteStory?.invoke(currentStory.product)
                                }
                                snackbarHostState.showSnackbar(ts.storyDeleted)
                                onRefreshStories()
                                onBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(ts.storyError.format(e.message ?: ""))
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, ts.delete, tint = Color.White)
                    }
                }

                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }

            // ── Caption / Note (if present) ──
            if (!currentStory.caption.isNullOrBlank() && currentStory.mediaType != "text") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        currentStory.caption,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Reply input ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text(ts.replyToSeller, color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.15f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.3f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                val msgText = replyText.trim()
                                replyText = ""
                                scope.launch {
                                    try {
                                        // For new standalone stories → use API
                                        if (currentStory.storyId > 0 && currentStory.shopId > 0) {
                                            // Send private message to the vendor (not story reply)
                                            val vendorId = if (currentStory.vendorId > 0) currentStory.vendorId
                                                else ApiClient.fetchShopById(currentStory.shopId)?.vendorId ?: 0
                                            if (vendorId > 0) {
                                                val msg = "📲 Story: ${currentStory.title}\n$msgText"
                                                ApiClient.sendMessage(
                                                    receiverId = vendorId,
                                                    text = msg,
                                                    productImageUrl = currentStory.imageUrl,
                                                    productTitle = "Story: ${currentStory.title}"
                                                )
                                                snackbarHostState.showSnackbar(ts.msgSentToSeller)
                                            } else {
                                                snackbarHostState.showSnackbar(ts.vendorNotFound)
                                            }
                                        } else if (currentStory.product != null) {
                                            // Legacy: use onReply callback
                                            val msg = "📲 Story: ${currentStory.product.title}\n$msgText"
                                            onReply?.invoke(msg, currentStory.product)
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(ts.storyError.format(e.message ?: ""))
                                    }
                                }
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            null,
                            tint = if (replyText.isNotBlank()) Orange else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── Bottom product card (for legacy Product stories) ──
            if (currentStory.product != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(currentStory.product) },
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                currentStory.product.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${currentStory.product.price.toInt()} FCFA",
                                color = Orange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            tint = Orange,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Pause indicator
            AnimatedVisibility(
                visible = isPaused,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    modifier = Modifier.padding(bottom = 80.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.8f)
                ) {
                    Icon(
                        Icons.Default.Pause,
                        null,
                        modifier = Modifier.padding(12.dp).size(24.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        // Snackbar host for toasts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )
    }
}
