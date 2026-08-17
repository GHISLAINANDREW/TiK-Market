package com.tik_market.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiOrder
import com.tik_market.data.models.OrderStatus
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.ui.components.EmptyState
import com.tik_market.ui.components.OrderProgressBar
import com.tik_market.theme.Green
import com.tik_market.theme.Orange
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onPay: (ApiOrder) -> Unit,
    onContactVendor: (productId: Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val s = LocalAppStrings.current
    var orders by remember { mutableStateOf<List<ApiOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var snackbarHostState by remember { mutableStateOf(SnackbarHostState()) }

    fun refreshOrders() {
        scope.launch {
            try {
                orders = ApiClient.fetchOrders()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(s.errorPrefix.format(e.message ?: s.unknownError))
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshOrders() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(s.myOrders, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            EmptyState(Icons.Default.Receipt, s.noOrders, s.noOrdersHint)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        onPay = { onPay(order) },
                        onContactVendor = onContactVendor,
                        onCancel = {
                            scope.launch {
                                try {
                                    ApiClient.deleteOrder(order.id)
                                    snackbarHostState.showSnackbar(s.orderCancelled)
                                    refreshOrders()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(s.errorPrefix.format(e.message ?: s.orderActionError))
                                }
                            }
                        },
                        onConfirmDelivery = {
                            scope.launch {
                                try {
                                    ApiClient.confirmOrderReceived(order.id)
                                    snackbarHostState.showSnackbar(s.receptionConfirmed)
                                    refreshOrders()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(s.errorPrefix.format(e.message ?: s.orderActionError))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: ApiOrder,
    onPay: () -> Unit,
    onContactVendor: (Int) -> Unit = {},
    onCancel: () -> Unit = {},
    onConfirmDelivery: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val isDirect = order.paymentType == "direct"
    val orderStatus = OrderStatus.fromCode(order.status)
    val canCancel = orderStatus == OrderStatus.PENDING
    val canConfirmDelivery = orderStatus == OrderStatus.DELIVERING
    val s = LocalAppStrings.current

    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(s.cancelOrderTitle) },
            text = { Text(s.cancelOrderConfirmText.format(order.orderNumber)) },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false; onCancel() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text(s.yes) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) { Text(s.no) }
            }
        )
    }

    // Delivery confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(s.confirmReceptionTitle) },
            text = { Text(s.confirmReceptionText.format(order.orderNumber)) },
            confirmButton = {
                Button(
                    onClick = { showConfirmDialog = false; onConfirmDelivery() },
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) { Text(s.confirmReception) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text(s.cancel) }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Commande ${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (isDirect) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF8E1)) {
                                Text("Virement", fontSize = 9.sp, color = Color(0xFFF57F17), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                Text(s.delivery, fontSize = 9.sp, color = Green, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.createdAt.take(10), fontSize = 11.sp, color = Color.Gray)
                        if (order.status == "delivering") {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE3F2FD)) {
                                Text(s.estimatedDelivery.format(addDays(order.createdAt.take(10), 3)), fontSize = 9.sp, color = Color(0xFF1565C0), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                Surface(
                    color = when {
                        orderStatus == OrderStatus.DELIVERED -> Color(0xFFE8F5E9)
                        orderStatus == OrderStatus.DELIVERING && order.clientConfirmed == 1 -> Color(0xFFFFF3E0) // Orange "Waiting"
                        orderStatus == OrderStatus.PENDING -> Color(0xFFFFF3E0)
                        orderStatus == OrderStatus.CANCELLED -> Color(0xFFFFEBEE)
                        else -> Color(0xFFE3F2FD)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when {
                            orderStatus == OrderStatus.DELIVERING && order.clientConfirmed == 1 -> s.waitingForVendor
                            else -> orderStatus.label
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            orderStatus == OrderStatus.DELIVERED -> Green
                            orderStatus == OrderStatus.DELIVERING && order.clientConfirmed == 1 -> Color(0xFFE65100)
                            orderStatus == OrderStatus.PENDING -> Color(0xFFE65100)
                            orderStatus == OrderStatus.CANCELLED -> Color(0xFFC62828)
                            else -> Color(0xFF1565C0)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OrderProgressBar(currentStatus = order.status, modifier = Modifier.padding(vertical = 8.dp))

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF5F5F5))
                Spacer(Modifier.height(12.dp))

                // ── Vendor info for direct payment ──
                if (isDirect && orderStatus == OrderStatus.PENDING && order.vendorInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF8E1),
                        border = BorderStroke(1.dp, Color(0xFFFFE082))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("📱 ${s.directToVendor}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF57F17))
                            Spacer(Modifier.height(8.dp))
                            Text(s.transferWait, fontSize = 12.sp, color = Color(0xFF5D4037), lineHeight = 18.sp)
                            Spacer(Modifier.height(8.dp))
                            order.vendorInfo!!.forEach { vendor ->
                                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(vendor.shopName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, null, Modifier.size(16.dp), tint = Green)
                                            Spacer(Modifier.width(6.dp))
                                            Text(vendor.vendorPhone.ifBlank { vendor.vendorPhoneUser }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF075E54))
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text("${s.amount} : ${order.totalAmount.toInt()} FCFA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandTopBarColor)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Timeline
                val timelineSteps = listOf(
                    Triple(OrderStatus.PENDING, s.orderCreated, order.createdAt),
                    Triple(OrderStatus.CONFIRMED, if (isDirect) s.paymentValidated else s.orderConfirmed, order.createdAt),
                    Triple(OrderStatus.PREPARING, s.orderPreparing, order.createdAt),
                    Triple(OrderStatus.DELIVERING, s.orderDelivering, order.createdAt),
                    Triple(OrderStatus.DELIVERED, s.orderDelivered, order.createdAt)
                )
                val currentStatusIndex = timelineSteps.indexOfFirst { it.first == orderStatus }

                Text(s.orderTracking, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                timelineSteps.forEachIndexed { index, (status, label, _) ->
                    val isDone = index <= currentStatusIndex
                    val isCurrent = index == currentStatusIndex

                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(24.dp).height(24.dp)) {
                            Box(
                                Modifier.size(20.dp)
                                    .background(if (isDone) Green else Color(0xFFE0E0E0), CircleShape)
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone && !isCurrent) {
                                    Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = Color.White)
                                } else if (isCurrent) {
                                    Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 13.sp, fontWeight = if (isDone) FontWeight.Medium else FontWeight.Normal, color = if (isDone) Color.DarkGray else Color.LightGray)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Delivery confirmation button for client
                if (canConfirmDelivery) {
                    if (order.clientConfirmed == 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Green.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.HourglassEmpty, null, Modifier.size(16.dp), tint = Green)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    s.awaitingSellerConfirmation,
                                    color = Green,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.confirmReception, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Cancel button for pending orders
                if (canCancel) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = BorderStroke(1.dp, Color(0xFFC62828))
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.cancelOrder, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Contact vendor button
                order.items?.firstOrNull()?.let { firstItem ->
                    Button(
                        onClick = { onContactVendor(firstItem.productId) },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Icon(Icons.Default.Chat, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.contactSeller, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))

            order.items?.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.title, fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("x${item.quantity}", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                    Text("${item.price.toInt()} F", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(s.total, fontSize = 11.sp, color = Color.Gray)
                    Text("${order.totalAmount.toInt()} FCFA", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Orange)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

private fun addDays(dateStr: String, days: Int): String {
    try {
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        val y = parts[0].toInt()
        val m = parts[1].toInt().coerceIn(1, 12)
        val d = parts[2].toInt().coerceIn(1, 31)
        var newDay = d + days
        var newMonth = m
        var newYear = y
        val daysInMonth = when (newMonth) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (newYear % 4 == 0 && (newYear % 100 != 0 || newYear % 400 == 0)) 29 else 28
            else -> 31
        }
        if (newDay > daysInMonth) {
            newDay -= daysInMonth
            newMonth++
            if (newMonth > 12) { newMonth = 1; newYear++ }
        }
        return "${newYear}-${newMonth.toString().padStart(2, '0')}-${newDay.toString().padStart(2, '0')}"
    } catch (_: Exception) { return dateStr }
}
