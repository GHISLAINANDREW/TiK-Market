package com.dschangmarket.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiOrder
import com.dschangmarket.theme.BrandTopBarColor
import com.dschangmarket.ui.components.OrderProgressBar
import com.dschangmarket.theme.Green
import com.dschangmarket.theme.Orange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onPay: (ApiOrder) -> Unit,
    onContactVendor: (productId: Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var orders by remember { mutableStateOf<List<ApiOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            orders = ApiClient.fetchOrders()
        } catch (_: Exception) { }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes commandes", fontWeight = FontWeight.SemiBold, color = Color.White) },
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
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucune commande", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
                items(orders) { order ->
                    OrderCard(order = order, onPay = { onPay(order) }, onContactVendor = onContactVendor)
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: ApiOrder, onPay: () -> Unit, onContactVendor: (Int) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Commande ${order.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.createdAt.take(10), fontSize = 11.sp, color = Color.Gray)
                        if (order.status == "delivering") {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE3F2FD)) {
                                Text("Livraison estimée : ${addDays(order.createdAt.take(10), 3)}", fontSize = 9.sp, color = Color(0xFF1565C0), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                Surface(
                    color = when (order.status) {
                        "pending" -> Color(0xFFFFF3E0)
                        "delivered" -> Color(0xFFE8F5E9)
                        "cancelled" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFE3F2FD)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        when (order.status) {
                            "pending" -> "À payer"
                            "confirmed" -> "Confirmée"
                            "preparing" -> "Préparation"
                            "delivering" -> "En livraison"
                            "delivered" -> "Livrée"
                            "cancelled" -> "Annulée"
                            else -> order.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            "pending" -> Color(0xFFE65100)
                            "delivered" -> Green
                            "cancelled" -> Color(0xFFC62828)
                            else -> Color(0xFF1565C0)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Visual Tracking Bar - now shown for all statuses
            OrderProgressBar(currentStatus = order.status, modifier = Modifier.padding(vertical = 8.dp))

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF5F5F5))
                Spacer(Modifier.height(12.dp))

                // Timeline détaillée
                val timelineSteps = listOf(
                    Triple("pending", "Commande créée", order.createdAt),
                    Triple("confirmed", "Paiement confirmé", order.createdAt),
                    Triple("preparing", "En préparation", order.createdAt),
                    Triple("delivering", "En cours de livraison", order.createdAt),
                    Triple("delivered", "Livrée", order.createdAt)
                )
                val currentStatusIndex = timelineSteps.indexOfFirst { it.first == order.status }

                Text("Suivi de commande", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                // Bouton contacter le vendeur
                order.items?.firstOrNull()?.let { firstItem ->
                    Button(
                        onClick = { onContactVendor(firstItem.productId) },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Icon(Icons.Default.Chat, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contacter le vendeur", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                    Text("Total", fontSize = 11.sp, color = Color.Gray)
                    Text("${order.totalAmount.toInt()} FCFA", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Orange)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Paiement désactivé pour le moment
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

/** Simple date manipulation: adds days to a "2026-01-15" string */
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
