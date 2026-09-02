package com.tik_market.ui.live

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.data.models.Product
import com.tik_market.theme.*
import com.tik_market.ui.components.decodeDataUrlToImageBitmap
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.shareText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveShoppingScreen(
    streamId: Int,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit
) {
    var stream by remember { mutableStateOf<ApiLiveStream?>(null) }
    var pinnedProduct by remember { mutableStateOf<Product?>(null) }
    var comments by remember { mutableStateOf<List<ApiLiveComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var hearts by remember { mutableStateOf(0) }
    var frameBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var lastFrameAt by remember { mutableStateOf(0L) }
    var streamEnded by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Initial load: fetch the real stream from the backend.
    LaunchedEffect(streamId) {
        try {
            val streams = ApiClient.fetchLiveStreams()
            stream = streams.firstOrNull { it.id == streamId }
            // If the stream is no longer listed as live (orphaned/ended), show "ended".
            if (stream == null) {
                streamEnded = true
            }
        } catch (_: Exception) {}
        
        // Load pinned product if exists
        stream?.pinnedProductId?.let { id ->
            try {
                pinnedProduct = ApiClient.fetchProduct(id).toProduct()
            } catch (_: Exception) {}
        }
    }

    // Polling for comments
    LaunchedEffect(streamId) {
        while (true) {
            comments = ApiClient.fetchLiveComments(streamId)
            delay(3000)
        }
    }

    // Polling for broadcast frames (frame-based live stream).
    LaunchedEffect(streamId) {
        while (true) {
            try {
                val frameB64 = ApiClient.fetchLiveFrame(streamId)
                if (frameB64 != null) {
                    val bmp = decodeDataUrlToImageBitmap("data:image/jpeg;base64,$frameB64")
                    if (bmp != null) {
                        frameBitmap = bmp
                        lastFrameAt = Clock.System.now().toEpochMilliseconds()
                        streamEnded = false
                    }
                }
                // If we had frames but none for 15s, the streamer is gone.
                if (lastFrameAt > 0 && Clock.System.now().toEpochMilliseconds() - lastFrameAt > 15000) {
                    streamEnded = true
                }
            } catch (_: Exception) {}
            delay(1000)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ── 1. Live Frame Background (frame-based stream) ──
        frameBitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: run {
            // Fallback: loading placeholder, or "stream ended" if the streamer is gone.
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (streamEnded) {
                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Le direct est terminé", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    } else {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("En attente du flux...", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                }
            }
        }

        // ── 2. Overlay UI ──
        Column(Modifier.fillMaxSize()) {
            // Header: Shop info + Viewers + Close
            LiveHeader(stream, onBack)
            
            Spacer(Modifier.weight(1f))
            
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Left: Comments & Pinned Product
                Column(Modifier.weight(1f)) {
                    // Comments list (floating)
                    Box(Modifier.height(200.dp).fillMaxWidth()) {
                        LazyColumn(
                            reverseLayout = true,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comments.reversed()) { comment ->
                                CommentBubble(comment)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Pinned Product (Alibaba Style)
                    pinnedProduct?.let { product ->
                        PinnedProductCard(product, onProductClick)
                    }
                }
                
                // Right: Actions (Like, Share, etc.)
                LiveActions(
                    onLike = { hearts++ },
                    onShare = { com.tik_market.utils.shareText("Regardez ce direct sur TiK-Market !", "Partager le direct") }
                )
            }
            
            // Bottom Input
            LiveInputRow(
                value = commentText,
                onValueChange = { commentText = it },
                onSend = {
                    if (commentText.isNotBlank()) {
                        scope.launch {
                            if (ApiClient.postLiveComment(streamId, commentText)) {
                                commentText = ""
                                comments = ApiClient.fetchLiveComments(streamId)
                            }
                        }
                    }
                }
            )
        }
        
        // Floating Hearts Animation
        repeat(hearts % 10) {
            FloatingHeart()
        }
    }
}

@Composable
private fun LiveHeader(stream: ApiLiveStream?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shop Profile
        Surface(
            color = Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White)) {
                    // Load logo if available
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stream?.shopName ?: "Chargement...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${stream?.viewerCount ?: 0} spectateurs", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { /* Follow */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Suivre", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // Live Badge
        Surface(
            color = RedAccent,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        
        IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

@Composable
private fun CommentBubble(comment: ApiLiveComment) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(comment.userName, color = Amber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(comment.text, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PinnedProductCard(product: Product, onClick: (Product) -> Unit) {
    Surface(
        onClick = { onClick(product) },
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(220.dp).height(70.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Product Image
            Box(Modifier.size(70.dp).background(Color(0xFFF5F5F5))) {
                // Load image
            }
            Column(Modifier.padding(8.dp).weight(1f)) {
                Text(product.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${product.price.toInt()} FCFA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Orange)
            }
            Icon(Icons.Default.ShoppingCart, null, tint = Orange, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun LiveActions(onLike: () -> Unit, onShare: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton(Icons.Default.Favorite, Color.White, Color(0xFFE91E63), onLike)
        ActionButton(Icons.Default.Share, Color.White, Color.Black.copy(alpha = 0.4f), onShare)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, bg: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = bg,
        shape = CircleShape,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun LiveInputRow(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Dites quelque chose...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onSend, enabled = value.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (value.isNotBlank()) GreenAccent else Color.White.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun FloatingHeart() {
    // Basic heart animation (just a placeholder for now)
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2000)
        visible = false
    }
    if (visible) {
        Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.offset(x = 300.dp, y = 500.dp).size(30.dp))
    }
}
