package com.tik_market.ui.chat

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.dto.ApiConversation
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Conversation(
    val id: String,
    val name: String,
    val lastMessage: String,
    val isLastMessageFromMe: Boolean = false,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val productTitle: String? = null,
    val vendorUserId: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onBack: () -> Unit,
    onConversationClick: (vendorName: String, productTitle: String?, vendorUserId: Int, isOnline: Boolean) -> Unit,
    showBack: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var conversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    // Delete conversation state
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetConv by remember { mutableStateOf<Conversation?>(null) }

    val filteredConversations = conversations.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    // Load conversations + periodic refresh
    suspend fun refreshConversations() {
        try {
            val apiConvs = ApiClient.fetchConversations()
            val sorted = apiConvs
                // Sort by timestamp descending (most recent first)
                .sortedByDescending { it.lastMessageAt }
                .map { apiConv ->
                    Conversation(
                        id = apiConv.userId.toString(),
                        name = apiConv.userName,
                        lastMessage = apiConv.lastMessage,
                        isLastMessageFromMe = apiConv.lastSenderId == ApiClient.getCurrentUser()?.id,
                        timestamp = apiConv.lastMessageAt,
                        unreadCount = apiConv.unreadCount,
                        isOnline = apiConv.isOnline, 
                        productTitle = null, 
                        vendorUserId = apiConv.userId
                    )
                }
            conversations = sorted
            errorMessage = null
        } catch (e: Exception) {
            if (conversations.isEmpty()) {
                errorMessage = e.message ?: s.error
            }
        }
        isLoading = false
    }

    // Initial load
    LaunchedEffect(Unit) {
        refreshConversations()
    }

    // Auto-refresh every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            refreshConversations()
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val cityColors = LocalCityColors.current

    Scaffold(
        topBar = {
            Box(Modifier.background(cityColors.gradient).shadow(2.dp)) {
                TopAppBar(
                    title = { Text(s.messageCenter, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                    navigationIcon = { if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Default.DoneAll, null, tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF7F8FA))) {
            // Search bar
            SearchBar(value = searchQuery, onValueChange = { searchQuery = it })

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), tint = RedAccent)
                        Spacer(Modifier.height(16.dp))
                        Text(errorMessage!!, fontSize = 16.sp, color = RedAccent, textAlign = TextAlign.Center)
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                refreshConversations()
                            }
                        }) { Text(s.retry) }
                    }
                }
            } else if (filteredConversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text(if (searchQuery.isEmpty()) s.noMessages else s.noResultsFor.format(searchQuery), fontSize = 16.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredConversations) { conv ->
                        ConversationItem(
                            conv,
                            onClick = { onConversationClick(conv.name, conv.productTitle, conv.vendorUserId, conv.isOnline) },
                            onDelete = {
                                deleteTargetConv = conv
                                showDeleteConfirm = true
                            }
                        )
                        HorizontalDivider(Modifier.padding(start = 80.dp), color = Color(0xFFF0F0F0), thickness = 0.5.dp)
                    }
                }
            }

            // ── Delete conversation confirmation ──
            if (showDeleteConfirm && deleteTargetConv != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false; deleteTargetConv = null },
                    title = { Text(s.deleteConversation) },
                    text = { Text(s.deleteConversationConfirm.format(deleteTargetConv!!.name)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val contactId = deleteTargetConv!!.vendorUserId
                                        ApiClient.deleteConversation(contactId)
                                        conversations = conversations.filter { it.id != deleteTargetConv!!.id }
                                    } catch (_: Exception) { }
                                    showDeleteConfirm = false
                                    deleteTargetConv = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text(s.deleteConfirm, color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteConfirm = false; deleteTargetConv = null }) {
                            Text(s.cancel)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    val s = LocalAppStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, Modifier.size(20.dp), tint = Color.Gray)
            Spacer(Modifier.width(8.dp))
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(s.searchContacts, color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp)
            )
        }
    }
}

@Composable
private fun ConversationItem(conv: Conversation, onClick: () -> Unit, onDelete: () -> Unit = {}) {
    val s = LocalAppStrings.current
    Surface(onClick = onClick, color = Color.White) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with verified badge potential
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = GreenSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(conv.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (conv.isOnline) {
                    Box(
                        Modifier.size(12.dp).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Box(Modifier.size(8.dp).align(Alignment.Center).background(MaterialTheme.colorScheme.primary, CircleShape))
                    }
                }
            }
            
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conv.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatShortDate(conv.timestamp), fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val prefix = if (conv.isLastMessageFromMe) "Moi: " else ""
                    Text(
                        "$prefix${conv.lastMessage}",
                        fontSize = 13.sp,
                        color = if (conv.unreadCount > 0) Color.Black else Color.Gray,
                        fontWeight = if (conv.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (conv.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color.Red, shape = CircleShape) {
                            Text(
                                "${conv.unreadCount}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = s.deleteConversation,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatShortDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    // Expect "YYYY-MM-DD HH:MM:SS"
    return if (dateStr.contains(" ")) dateStr.substringAfter(" ").take(5) else dateStr
}
