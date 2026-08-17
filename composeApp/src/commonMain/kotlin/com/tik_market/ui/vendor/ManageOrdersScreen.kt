package com.tik_market.ui.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiOrder
import com.tik_market.data.models.OrderStatus
import com.tik_market.ui.components.OrderProgressBar
import com.tik_market.theme.*
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOrdersScreen(onBack: () -> Unit) {
    var orders by remember { mutableStateOf<List<ApiOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(value = true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }
    val ts = LocalAppStrings.current

    suspend fun loadVendorOrders() {
        try {
            orders = ApiClient.fetchVendorOrders()
            isLoading = false
            errorMessage = null
        } catch (e: Exception) {
            isLoading = false
            errorMessage = e.message ?: ts.unknownError
        }
    }

    suspend fun updateOrderStatus(orderId: Int, status: String) {
        try {
            ApiClient.updateOrderStatus(orderId, status)
            loadVendorOrders()
        } catch (e: Exception) {
            errorMessage = e.message ?: ts.updateError
        }
    }

    LaunchedEffect(Unit) { loadVendorOrders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.manageOrders, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
                errorMessage != null -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(errorMessage ?: "", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { isLoading = true; errorMessage = null; scope.launch { loadVendorOrders() } }) {
                        Text(ts.retry)
                    }
                }
                orders.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ts.noOrdersNow, fontSize = 16.sp, color = Color.Gray)
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(orders) { order ->
                        VendorOrderCard(order = order) { status ->
                            scope.launch { updateOrderStatus(order.id, status) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorOrderCard(order: ApiOrder, onUpdateStatus: (String) -> Unit) {
    val ts = LocalAppStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(order.createdAt, fontSize = 11.sp, color = Color.Gray)
                }
                StatusBadgeV(order.status)
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))

            // Progress Bar for Vendor
            val orderStatus = OrderStatus.fromCode(order.status)
            if (orderStatus != OrderStatus.CANCELLED && orderStatus != OrderStatus.PENDING) {
                OrderProgressBar(currentStatus = order.status, modifier = Modifier.padding(vertical = 8.dp))
                Spacer(Modifier.height(12.dp))
            }

            Text(ts.customerInfo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Green)
            Text(ts.telPrefix.format(order.phone), fontSize = 13.sp)
            Text(ts.deliveryPrefix.format(order.shippingAddress), fontSize = 13.sp, color = Color.Gray)
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))

            order.items?.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${item.title} x${item.quantity}", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("${item.price.toInt()} F", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (order.shopTotal != null) ts.shopShare else ts.total, fontWeight = FontWeight.Bold)
                val displayTotal = (order.shopTotal ?: order.totalAmount).toInt()
                Text("$displayTotal FCFA", fontWeight = FontWeight.ExtraBold, color = Green, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentStatus = OrderStatus.fromCode(order.status)
                when (currentStatus) {
                    OrderStatus.PENDING -> ActionButton(ts.confirmOrder, Orange, Modifier.weight(1f)) { onUpdateStatus("confirmed") }
                    OrderStatus.CONFIRMED -> ActionButton(ts.startPrep, GreenAccent, Modifier.weight(1f)) { onUpdateStatus("preparing") }
                    OrderStatus.PREPARING -> ActionButton(ts.readyForDelivery, Color(0xFF1565C0), Modifier.weight(1f)) { onUpdateStatus("delivering") }
                    OrderStatus.DELIVERING -> {
                        if (order.clientConfirmed == 1) {
                            ActionButton(ts.confirmFinalDelivery, Green, Modifier.weight(1f)) { 
                                onUpdateStatus("delivered")
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Gray.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text(
                                    ts.awaitingReceipt,
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusBadgeV(status: String) {
    val orderStatus = OrderStatus.fromCode(status)
    val (bg, fg) = when (orderStatus) {
        OrderStatus.PENDING -> Color(0xFF9E9E9E) to Color.White
        OrderStatus.CONFIRMED -> Color(0xFF2196F3) to Color.White
        OrderStatus.PREPARING -> GreenAccent to Color.White
        OrderStatus.DELIVERING -> Color(0xFF9C27B0) to Color.White
        OrderStatus.DELIVERED -> Green to Color.White
        OrderStatus.CANCELLED -> Color(0xFFF44336) to Color.White
    }
    Surface(shape = RoundedCornerShape(16.dp), color = bg) {
        Text(orderStatus.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
