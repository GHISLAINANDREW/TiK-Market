package com.tik_market.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.dto.ApiNotification
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Green
import com.tik_market.theme.Orange
import com.tik_market.theme.VioletSoft
import com.tik_market.ui.components.EmptyState
import com.tik_market.utils.safeApiCall
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onProductClick: (Int) -> Unit,
    onOrderClick: (Int) -> Unit
) {
    var notifications by remember { mutableStateOf<List<ApiNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    suspend fun loadNotifications() {
        val result = safeApiCall { ApiClient.fetchNotifications() }
        if (result.isSuccess) {
            notifications = result.getOrDefault(emptyList())
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    fun markAllAsRead() {
        scope.launch {
            safeApiCall { ApiClient.markNotificationAsRead() }
            loadNotifications()
        }
    }

    fun deleteNotification(id: Int) {
        scope.launch {
            safeApiCall { ApiClient.deleteNotification(id) }
            loadNotifications()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.notifications, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back, tint = Color.White)
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(onClick = { markAllAsRead() }) {
                            Text(s.markAllRead, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(VioletSoft.copy(alpha = 0.3f))) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Green)
            } else if (notifications.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Notifications,
                    title = s.noNotifications,
                    subtitle = s.noNotificationsHint
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications) { notif ->
                        NotificationItem(
                            notification = notif,
                            onClick = {
                                scope.launch { ApiClient.markNotificationAsRead(notif.id) }
                                when (notif.type) {
                                    "product" -> notif.relatedId?.let { onProductClick(it) }
                                    "order" -> notif.relatedId?.let { onOrderClick(it) }
                                }
                            },
                            onDelete = { deleteNotification(notif.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: ApiNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    val icon = when (notification.type) {
        "product" -> Icons.Default.Store
        "order" -> Icons.Default.ShoppingCart
        "system" -> Icons.Default.Settings
        "promo" -> Icons.Default.Sell
        else -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        "product" -> Green
        "order" -> Orange
        "system" -> Color.Gray
        "promo" -> Color.Red
        else -> Green
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (notification.isRead) Color.White else Color.White.copy(alpha = 0.9f),
        shadowElevation = if (notification.isRead) 1.dp else 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (notification.isRead) Color.DarkGray else Color.Black
                    )
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(8.dp).background(Orange, CircleShape))
                    }
                }
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Text(
                    text = notification.createdAt,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = s.delete, tint = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
