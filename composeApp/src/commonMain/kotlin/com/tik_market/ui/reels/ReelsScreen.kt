package com.tik_market.ui.reels

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
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
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.cache.PersistentMediaCache
import com.tik_market.theme.*
import com.tik_market.ui.components.VideoPlayer
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsScreen(
    onBack: () -> Unit,
    onShopClick: (Int) -> Unit
) {
    var reels by remember { mutableStateOf<List<ApiReel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(pageCount = { reels.size })

    LaunchedEffect(Unit) {
        isLoading = true
        reels = ApiClient.fetchReels()
        // Mocking if empty
        if (reels.isEmpty()) {
            reels = listOf(
                ApiReel(1, 1, "Boutique de Will", null, "https://www.w3schools.com/html/mov_bbb.mp4", "Découvrez nos produits frais ! 🍗"),
                ApiReel(2, 2, "Mode & Élégance", null, "https://www.w3schools.com/html/mov_bbb.mp4", "Nouvel arrivage Pagne Wax ✨")
            )
        }
        isLoading = false
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        } else {
            // ── PRE-FETCHING ──
            LaunchedEffect(pagerState.currentPage) {
                for (i in 1..2) {
                    val nextIndex = pagerState.currentPage + i
                    if (nextIndex < reels.size) {
                        PersistentMediaCache.cacheMedia(reels[nextIndex].videoUrl)
                    }
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                ReelItem(
                    reel = reels[index],
                    isCurrent = pagerState.currentPage == index,
                    onShopClick = onShopClick
                )
            }
        }
        
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 48.dp, start = 16.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
        }
    }
}

@Composable
private fun ReelItem(
    reel: ApiReel,
    isCurrent: Boolean,
    onShopClick: (Int) -> Unit
) {
    var isLiked by remember { mutableStateOf(reel.isLiked) }
    var likes by remember { mutableStateOf(reel.likeCount) }
    var showComments by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<ApiReelComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        // 1. Video Player
        VideoPlayer(
            url = reel.videoUrl,
            modifier = Modifier.fillMaxSize(),
            isPlaying = isCurrent,
            onEnded = { }
        )

        // 2. Gradient Overlay for readability
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 500f)))

        // 3. Side Actions
        Column(
            Modifier.align(Alignment.CenterEnd).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    isLiked = !isLiked
                    likes += if (isLiked) 1 else -1
                    scope.launch { ApiClient.likeReel(reel.id) }
                }) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text("$likes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    showComments = true
                    scope.launch { comments = ApiClient.fetchReelComments(reel.id) }
                }) {
                    Icon(Icons.Default.Comment, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("${comments.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            // Share
            IconButton(onClick = { com.tik_market.utils.shareText("Regardez ce reel sur TiK-Market !", "Partager le reel") }) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // Delete (only for the shop owner — checked via a lightweight call)
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
            }
        }

        // 4. Bottom Info
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp).padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shop Logo
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onShopClick(reel.shopId) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(reel.shopName.take(1), color = Orange, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(reel.shopName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(12.dp))
                // Follow Button
                Surface(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.White)
                ) {
                    Text("Suivre", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                reel.description,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }

    // ── Comments dialog ──
    if (showComments) {
        AlertDialog(
            onDismissRequest = { showComments = false },
            title = { Text("Commentaires", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(comments) { c ->
                            Row(Modifier.padding(vertical = 6.dp)) {
                                Text(c.userName, color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(c.text, fontSize = 13.sp)
                            }
                        }
                        if (comments.isEmpty()) {
                            item { Text("Aucun commentaire. Soyez le premier !", color = Color.Gray, fontSize = 13.sp) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Ajouter un commentaire...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val text = commentText.trim()
                                if (text.isNotEmpty()) {
                                    scope.launch {
                                        if (ApiClient.postReelComment(reel.id, text)) {
                                            commentText = ""
                                            comments = ApiClient.fetchReelComments(reel.id)
                                        }
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Send, null, tint = if (commentText.isNotBlank()) Green else Color.Gray)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showComments = false }) { Text("Fermer") } }
        )
    }

    // ── Delete confirmation ──
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce reel ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ApiClient.deleteReel(reel.id)
                        showDeleteConfirm = false
                    }
                }) { Text("Supprimer", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") } }
        )
    }
}
