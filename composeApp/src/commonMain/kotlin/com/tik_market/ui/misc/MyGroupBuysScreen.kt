package com.tik_market.ui.misc

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiGroupBuy
import com.tik_market.api.ApiGroupBuyDetail
import com.tik_market.theme.*
import com.tik_market.utils.FormatUtils
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

private val statusColors = mapOf(
    "open" to Green,
    "filled" to Orange,
    "completed" to Color(0xFF1565C0),
    "cancelled" to Color.Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupBuysScreen(onBack: () -> Unit, onProductClick: (Int) -> Unit = {}) {
    var groupBuys by remember { mutableStateOf<List<ApiGroupBuy>>(emptyList()) }
    var details by remember { mutableStateOf<Map<Int, ApiGroupBuyDetail>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val ts = LocalAppStrings.current

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            groupBuys = ApiClient.fetchMyGroupBuys()
            // Load details for active ones
            groupBuys.filter { it.status in listOf("open", "filled") }.forEach { gb ->
                try {
                    val d = ApiClient.fetchGroupBuyDetails(gb.id)
                    if (d.groupBuy != null) {
                        details = details + (gb.id to d.groupBuy)
                    }
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    val filtered = remember(groupBuys, selectedFilter) {
        when (selectedFilter) {
            "active" -> groupBuys.filter { it.status == "open" }
            "completed" -> groupBuys.filter { it.status == "completed" }
            "cancelled" -> groupBuys.filter { it.status == "cancelled" }
            else -> groupBuys
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.myGroupBuys, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stats rapides
                item {
                    val stats = remember(groupBuys) {
                        ts.participantsStats.format(groupBuys.count { it.status == "open" }, groupBuys.count { it.status == "completed" })
                    }
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, null, Modifier.size(32.dp), tint = Green)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ts.myParticipations, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(stats, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Filtres
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to ts.allFilter, "active" to ts.activePlural, "completed" to ts.completedPlural, "cancelled" to ts.cancelledPlural).forEach { (key, label) ->
                            Surface(
                                onClick = { selectedFilter = key },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedFilter == key) Green else Color(0xFFE0E0E0)
                            ) {
                                Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = if (selectedFilter == key) FontWeight.Bold else FontWeight.Normal, color = if (selectedFilter == key) Color.White else Color.DarkGray)
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.GroupOff, null, Modifier.size(48.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text(ts.noParticipation, color = Color.Gray, fontSize = 14.sp)
                                Text(ts.joinGroupHint, fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                items(filtered, key = { it.id }) { gb ->
                    MyGroupBuyCard(gb, details[gb.id], onProductClick)
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun MyGroupBuyCard(gb: ApiGroupBuy, detail: ApiGroupBuyDetail?, onProductClick: (Int) -> Unit) {
    val ts = LocalAppStrings.current
    var expanded by remember { mutableStateOf(false) }
    val progress = if (gb.minQuantity > 0) (gb.currentQty.toFloat() / gb.minQuantity).coerceAtMost(1f) else 0f
    val statusColor = statusColors[gb.status] ?: Color.Gray
    val statusLabel = when (gb.status) {
        "open" -> ts.active
        "filled" -> ts.filled
        "completed" -> ts.completed
        "cancelled" -> ts.cancelled
        else -> gb.status
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(gb.productTitle.ifBlank { "Produit #${gb.productId}" }, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(gb.shopName.ifBlank { "" }, fontSize = 12.sp, color = Color.Gray)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(ts.originalPrice, fontSize = 11.sp, color = Color.Gray)
                    Text(FormatUtils.formatPrice(gb.originalPrice), fontSize = 12.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${gb.discountPct.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Green)
                    Text(ts.reduction, fontSize = 9.sp, color = Green, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(ts.myPrice, fontSize = 11.sp, color = Color.Gray)
                    Text(FormatUtils.formatPrice(gb.targetPrice), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenDark)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Progression
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ts.groupLabel, fontSize = 11.sp, color = Color.Gray)
                    Text("${gb.currentQty}/${gb.minQuantity}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (progress >= 1f) Green else Orange)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (progress >= 1f) Green else Orange, trackColor = Color(0xFFE0E0E0), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            // Participants
            if (expanded && detail != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))
                Text(ts.participantsCount.format(detail.participants.size), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                detail.participants.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Text(p.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Green)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(p.name.ifBlank { ts.anonymous }, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("x${p.quantity}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(20.dp).align(Alignment.CenterHorizontally), tint = Color.Gray)
        }
    }
}
