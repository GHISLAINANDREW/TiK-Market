package com.tik_market.ui.vendor

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.ApiClient
import com.tik_market.data.models.Product
import com.tik_market.data.models.OrderStatus
import com.tik_market.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DashboardStat(val label: String, val value: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDashboardScreen(
    onBack: () -> Unit,
    shopName: String = "Ma Boutique",
    onManageShop: () -> Unit,
    onAddProduct: () -> Unit,
    onViewOrders: () -> Unit,
    onGroupBuys: () -> Unit = {},
    onSubscribers: () -> Unit = {},
    refreshSignal: Int = 0
) {
    var internalRefresh by remember { mutableStateOf(0) }
    var stats by remember { mutableStateOf<ApiVendorStatsResponse?>(null) }
    var myProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshSignal, internalRefresh) {
        isLoading = true
        try {
            val response = ApiClient.fetchVendorStats()
            if (response.success) {
                stats = response
            } else {
                stats = null
            }
            // Fetch all products of the shop
            val shop = ApiClient.fetchShopByVendor()
            if (shop != null) {
                val products = ApiClient.fetchProducts(shopId = shop.id, includeInactive = true)
                myProducts = products.map { it.toProduct() }
            }
        } catch (_: Exception) { 
            stats = null
        }
        isLoading = false
    }

    // Derive display stats
    val overview = stats?.overview
    val displayStats = remember(overview) {
        if (overview == null) emptyList() else listOf(
            DashboardStat("Produits", "${overview.productCount}", Icons.Default.Store, Green),
            DashboardStat("Commandes", "${overview.totalOrders}", Icons.Default.ShoppingCart, GreenAccent),
            DashboardStat("Vendus", "${overview.totalItemsSold}", Icons.Default.TrendingUp, Color(0xFF1565C0)),
            DashboardStat("Revenu", "${overview.totalRevenue.toInt().let { if (it >= 1000) "${it / 1000}k" else "$it" }} FCFA", Icons.Default.AccountBalance, GreenDark)
        )
    }

    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

        Scaffold(topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tableau de bord", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(stats?.shopName ?: shopName, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { internalRefresh++ }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                } else {
                    // ── Stats cards ──
                    item {
                        if (isLoading) {
                             Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Green)
                             }
                        } else if (displayStats.isNotEmpty()) {
                            if (isCompact) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    displayStats.chunked(2).forEach { row ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            row.forEach { stat -> StatCard(stat, Modifier.weight(1f)) }
                                            if (row.size < 2) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    displayStats.forEach { stat -> StatCard(stat, Modifier.weight(1f)) }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("Aucune statistique disponible", color = Color.Gray)
                            }
                        }
                    }

                    // ── Stock alerts ──
                    overview?.let { ov ->
                        if (ov.lowStockCount > 0 || ov.outOfStockCount > 0) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(40.dp).background(Color(0xFFFFE0B2), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Warning, null, Modifier.size(24.dp), tint = Color(0xFFE65100))
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text("Alertes de stock", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE65100))
                                                if (ov.lowStockCount > 0)
                                                    Text("${ov.lowStockCount} produit(s) bientôt épuisé(s)", fontSize = 13.sp, color = Color(0xFFE65100).copy(alpha = 0.8f))
                                                if (ov.outOfStockCount > 0)
                                                    Text("${ov.outOfStockCount} produit(s) en rupture de stock", fontSize = 13.sp, color = Color.Red.copy(alpha = 0.8f))
                                            }
                                        }
                                        
                                        Spacer(Modifier.height(12.dp))
                                        
                                        Button(
                                            onClick = onManageShop,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Mettre à jour le stock", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Revenue chart ──
                    val dailyRev = stats?.dailyRevenue ?: emptyList()
                    if (dailyRev.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Revenus (7 jours)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        val totalWeek = dailyRev.sumOf { it.revenue }
                                        Text("${totalWeek.toInt()} FCFA", fontSize = 14.sp, color = Green, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    BarChart(
                                        data = dailyRev.map { it.revenue },
                                        labels = dailyRev.map {
                                            val parts = it.day.split("-")
                                            if (parts.size >= 3) "${parts[2]}/${parts[1]}" else it.day
                                        },
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        barColor = Green
                                    )
                                }
                            }
                        }
                    }

                    // ── Monthly revenue ──
                    val monthlyRev = stats?.monthlyRevenue ?: emptyList()
                    if (monthlyRev.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Revenus mensuels", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        val total6m = monthlyRev.sumOf { it.revenue }
                                        Text("${total6m.toInt()} FCFA", fontSize = 14.sp, color = GreenAccent, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    BarChart(
                                        data = monthlyRev.map { it.revenue },
                                        labels = monthlyRev.map {
                                            val parts = it.month.split("-")
                                            if (parts.size >= 2) {
                                                val months = listOf("", "Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Aoû", "Sep", "Oct", "Nov", "Déc")
                                                "${months[parts[1].toIntOrNull() ?: 0]}"
                                            } else it.month
                                        },
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        barColor = GreenAccent
                                    )
                                }
                            }
                        }
                    }

                    // ── Orders by status ──
                    val ordersByStatus = stats?.ordersByStatus ?: emptyMap()
                    if (ordersByStatus.isNotEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Commandes par statut", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.height(12.dp))
                                    val statusColors = mapOf(
                                        OrderStatus.PENDING to Color(0xFFFFA000),
                                        OrderStatus.CONFIRMED to GreenAccent,
                                        OrderStatus.PREPARING to Color(0xFF1565C0),
                                        OrderStatus.DELIVERING to Color(0xFF7B1FA2),
                                        OrderStatus.DELIVERED to Green,
                                        OrderStatus.CANCELLED to Color.Gray
                                    )
                                    ordersByStatus.entries.forEach { (statusStr, count) ->
                                        val status = OrderStatus.fromCode(statusStr)
                                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(10.dp).background(statusColors[status] ?: Color.Gray, RoundedCornerShape(2.dp)))
                                            Spacer(Modifier.width(8.dp))
                                            Text(status.label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                            Text("$count", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Actions rapides ──
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Actions rapides", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ActionButton(Icons.Default.AddCircle, "Ajouter\nproduit", Green, onAddProduct, Modifier.weight(1f))
                                    ActionButton(Icons.Default.Store, "Gérer\nboutique", GreenAccent, onManageShop, Modifier.weight(1f))
                                    ActionButton(Icons.Default.ShoppingBag, "Voir\ncommandes", Color(0xFF1565C0), onViewOrders, Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ActionButton(Icons.Default.Group, "Achats\ngroupés", Orange, onGroupBuys, Modifier.weight(1f))
                                    ActionButton(Icons.Default.People, "Voir\nabonnés", GreenAccent, onSubscribers, Modifier.weight(1f))
                                    ActionButton(Icons.Default.Download, "Export\nCSV", Color(0xFF1565C0), {
                                        com.tik_market.ui.chat.openUrl("${ApiClient.baseUrl}/vendor/export.php?type=products")
                                    }, Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ActionButton(Icons.Default.Receipt, "CSV\nCommandes", Color(0xFF1565C0), {
                                        com.tik_market.ui.chat.openUrl("${ApiClient.baseUrl}/vendor/export.php?type=orders")
                                    }, Modifier.weight(1f))
                                    ActionButton(Icons.Default.TrendingUp, "CSV\nRevenus", GreenDark, {
                                        com.tik_market.ui.chat.openUrl("${ApiClient.baseUrl}/vendor/export.php?type=revenue")
                                    }, Modifier.weight(1f))
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // ── Top products ──
                    val topProducts = stats?.topProducts ?: emptyList()
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Top produits", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    TextButton(onClick = onAddProduct) { Text("+ Ajouter", color = Green) }
                                }
                                Spacer(Modifier.height(8.dp))
                                if (topProducts.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Inventory, null, tint = Color.LightGray)
                                            Text("Aucun produit pour le moment", color = Color.Gray, fontSize = 12.sp)
                                            Button(onClick = onAddProduct, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                                Text("Ajouter mon premier produit", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    topProducts.forEachIndexed { index, prod ->
                                        if (index > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            // Rank badge
                                            val rankColors = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32))
                                            val rankColor = if (index < 3) rankColors[index] else Color(0xFFE0E0E0)
                                            Box(Modifier.size(36.dp).background(rankColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                Text("${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = rankColor)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(prod.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                                Row {
                                                    Text("${prod.totalSold} vendu(s)", fontSize = 12.sp, color = Color.Gray)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("${prod.totalGenerated.toInt()} FCFA", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Tous mes produits ──
                    item {
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Tous mes produits", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${myProducts.size}", fontSize = 14.sp, color = Green, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(12.dp))
                                if (myProducts.isEmpty()) {
                                    Text("Aucun produit répertorié.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                                } else {
                                    myProducts.forEachIndexed { index, prod ->
                                        if (index > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(40.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                if (prod.images.isNotEmpty()) {
                                                    // On pourrait charger l'image ici si on avait un composant de chargement d'image
                                                    Text("📦", fontSize = 20.sp)
                                                } else {
                                                    Text("📦", fontSize = 20.sp)
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(prod.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${prod.price.toInt()} FCFA", fontSize = 12.sp, color = Green, fontWeight = FontWeight.SemiBold)
                                                    if (prod.isStory) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Surface(color = Orange.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                                            Text("Story", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontSize = 9.sp, color = Orange, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            IconButton(onClick = { /* TODO: Editer */ }) {
                                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp), tint = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BarChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = Green
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(modifier) {
        // Bars row
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { value ->
                    val fraction = (value / maxVal).toFloat().coerceIn(0.01f, 1f)
                    Column(
                        Modifier.weight(1f).padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Value label
                        Text(
                            "${value.toInt()}",
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(2.dp))
                        // Bar
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Labels row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    label,
                    fontSize = 9.sp,
                    color = textColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(stat: DashboardStat, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(12.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(stat.icon, null, Modifier.size(20.dp), tint = stat.color)
            Spacer(Modifier.height(4.dp))
            Text(stat.value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = stat.color)
            Text(stat.label, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(80.dp), shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color, textAlign = TextAlign.Center)
        }
    }
}
