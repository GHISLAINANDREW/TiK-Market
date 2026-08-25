package com.tik_market.ui.vendor

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
import com.tik_market.api.*
import com.tik_market.api.dto.ApiGroupBuy
import com.tik_market.api.dto.ApiGroupBuyParticipant
import com.tik_market.api.dto.ApiProduct
import com.tik_market.theme.*
import com.tik_market.utils.FormatUtils
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

private val statusColors = mapOf(
    "open" to Green,
    "filled" to Orange,
    "completed" to Color(0xFF1565C0),
    "cancelled" to Color.Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorGroupBuysScreen(
    onBack: () -> Unit,
    shopName: String = "",
    refreshSignal: Int = 0,
    onNavigateToChat: (userId: Int, userName: String) -> Unit = { _, _ -> }
) {
    var groupBuys by remember { mutableStateOf<List<ApiGroupBuy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") }
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var participants by remember { mutableStateOf<Map<Int, List<ApiGroupBuyParticipant>>>(emptyMap()) }
    var shopId by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showNotifyDialog by remember { mutableStateOf<ApiGroupBuy?>(null) }
    var vendorProducts by remember { mutableStateOf<List<ApiProduct>>(emptyList()) }
    var isProductsLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val ts = LocalAppStrings.current

    // Charger shopId + group-buys + produits
    LaunchedEffect(refreshSignal) {
        isLoading = true
        try {
            val shop = ApiClient.fetchShopByVendor()
            if (shop != null) {
                shopId = shop.id
                groupBuys = ApiClient.fetchShopGroupBuys(shop.id)
                
                // Charger les produits du vendeur pour la création
                isProductsLoading = true
                vendorProducts = ApiClient.fetchProducts(shopId = shop.id)
                isProductsLoading = false
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    // Stats dérivées
    val stats = remember(groupBuys) {
        val active = groupBuys.count { it.status == "open" }
        val filled = groupBuys.count { it.status == "filled" }
        val completed = groupBuys.count { it.status == "completed" }
        val cancelled = groupBuys.count { it.status == "cancelled" }
        val totalParticipants = groupBuys.sumOf { it.participantsCount }
        GroupBuyStats(active, filled, completed, cancelled, totalParticipants)
    }

    val filteredGroupBuys = remember(groupBuys, selectedFilter) {
        when (selectedFilter) {
            "active" -> groupBuys.filter { it.status == "open" }
            "filled" -> groupBuys.filter { it.status == "filled" }
            "completed" -> groupBuys.filter { it.status == "completed" || it.status == "cancelled" }
            else -> groupBuys
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.groupBuysTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Green,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(ts.launchGroup) }
            )
        }
    ) { padding ->
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
                // ── Carte résumé ──
                item {
                    StatsSummaryCard(stats, ts)
                }

                // ── Filtres ──
                item {
                    FilterRow(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it },
                        counts = GroupBuyCounts(stats.active, stats.filled, stats.completed + stats.cancelled),
                        ts = ts
                    )
                }

                // ── Message vide ──
                if (filteredGroupBuys.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Group, null, Modifier.size(48.dp), tint = Color.LightGray)
                                Spacer(Modifier.height(12.dp))
                                Text(ts.noGroupBuys, style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                                Text(
                                    ts.noGroupBuysHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── Liste des group-buys ──
                items(filteredGroupBuys, key = { it.id }) { gb ->
                    GroupBuyVendorCard(
                        groupBuy = gb,
                        isExpanded = expandedId == gb.id,
                        participants = participants[gb.id] ?: emptyList(),
                        ts = ts,
                        onToggleExpand = {
                            expandedId = if (expandedId == gb.id) null else gb.id
                            if (expandedId == gb.id && participants[gb.id] == null) {
                                scope.launch {
                                    try {
                                        val detail = ApiClient.fetchGroupBuyDetails(gb.id)
                                        if (detail.groupBuy != null) {
                                            participants = participants + (gb.id to detail.groupBuy.participants)
                                        }
                                    } catch (_: Exception) { }
                                }
                            }
                        },
                        onCancel = {
                            scope.launch {
                                try {
                                    val resp = ApiClient.cancelGroupBuy(gb.id)
                                    if (resp.success) {
                                        snackbarHostState.showSnackbar(ts.groupBuyCancelled)
                                        groupBuys = ApiClient.fetchShopGroupBuys(shopId)
                                    } else {
                                        snackbarHostState.showSnackbar(resp.error ?: ts.error)
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.message ?: ts.error)
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    val resp = ApiClient.deleteGroupBuy(gb.id)
                                    if (resp.success) {
                                        snackbarHostState.showSnackbar(ts.groupBuyDeleted)
                                        groupBuys = ApiClient.fetchShopGroupBuys(shopId)
                                    } else {
                                        snackbarHostState.showSnackbar(resp.error ?: ts.error)
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(e.message ?: ts.error)
                                }
                            }
                        },
                        onNotifyAll = { showNotifyDialog = gb },
                        onMessageParticipant = { id, name -> onNavigateToChat(id, name) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateDialog) {
        VendorCreateGroupBuyDialog(
            products = vendorProducts,
            onDismiss = { showCreateDialog = false },
            ts = ts,
            onCreate = { productId, minQty, discountPct, hours ->
                scope.launch {
                    try {
                        val resp = ApiClient.createGroupBuy(productId, minQty, discountPct, hours)
                        if (resp.groupBuy != null) {
                            snackbarHostState.showSnackbar(ts.groupBuyLaunched)
                            groupBuys = ApiClient.fetchShopGroupBuys(shopId)
                            showCreateDialog = false
                        } else {
                            snackbarHostState.showSnackbar(resp.error ?: ts.errorCreatingGroup)
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: ts.error)
                    }
                }
            }
        )
    }

    if (showNotifyDialog != null) {
        VendorNotifyGroupDialog(
            groupBuy = showNotifyDialog!!,
            onDismiss = { showNotifyDialog = null },
            ts = ts,
            onSend = { title, msg ->
                scope.launch {
                    try {
                        val resp = ApiClient.notifyGroupParticipants(showNotifyDialog!!.id, title, msg)
                        if (resp.success) {
                            snackbarHostState.showSnackbar(ts.notificationSentParticipants)
                            showNotifyDialog = null
                        } else {
                            snackbarHostState.showSnackbar(resp.error ?: ts.error)
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(e.message ?: ts.error)
                    }
                }
            }
        )
    }
}

// ── Données stats internes ──
private data class GroupBuyStats(
    val active: Int,
    val filled: Int,
    val completed: Int,
    val cancelled: Int,
    val totalParticipants: Int
)
private data class GroupBuyCounts(val active: Int, val filled: Int, val completed: Int)

// ── Carte résumé stats ──
@Composable
private fun StatsSummaryCard(stats: GroupBuyStats, ts: com.tik_market.utils.AppStrings) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(ts.summary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(Icons.Default.Timer, "${stats.active}", ts.activePlural, Green)
                StatItem(Icons.Default.CheckCircle, "${stats.filled}", ts.filledPlural, Orange)
                StatItem(Icons.Default.TaskAlt, "${stats.completed}", ts.completedPlural, Color(0xFF1565C0))
                StatItem(Icons.Default.People, "${stats.totalParticipants}", ts.participantsLabel, GreenAccent)
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(22.dp), tint = color)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

// ── Rangée de filtres ──
@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit, counts: GroupBuyCounts, ts: com.tik_market.utils.AppStrings) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(ts.allFilter, "all", selected, onSelect, groupBuysCount = -1)
        FilterChip("${ts.activePlural} (${counts.active})", "active", selected, onSelect)
        FilterChip("${ts.filledPlural} (${counts.filled})", "filled", selected, onSelect)
        FilterChip("${ts.completedPlural} (${counts.completed})", "completed", selected, onSelect)
    }
}

@Composable
private fun FilterChip(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
    groupBuysCount: Int = 0
) {
    val isSelected = selected == value
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Green else Color(0xFFE0E0E0),
        contentColor = if (isSelected) Color.White else Color.DarkGray
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── Carte d'un group-buy (vue vendeur) ──
@Composable
private fun GroupBuyVendorCard(
    groupBuy: ApiGroupBuy,
    isExpanded: Boolean,
    participants: List<ApiGroupBuyParticipant>,
    onToggleExpand: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit = {},
    onNotifyAll: () -> Unit = {},
    onMessageParticipant: (Int, String) -> Unit = { _, _ -> },
    ts: com.tik_market.utils.AppStrings
) {
    val canCancel = groupBuy.status == "open" || groupBuy.status == "filled"
    val canNotify = groupBuy.participantsCount > 0
    val progress = if (groupBuy.minQuantity > 0) (groupBuy.currentQty.toFloat() / groupBuy.minQuantity).coerceAtMost(1f) else 0f
    val statusColor = statusColors[groupBuy.status] ?: Color.Gray
    val statusLabel = when (groupBuy.status) {
        "open" -> ts.active
        "filled" -> ts.filled
        "completed" -> ts.completed
        "cancelled" -> ts.cancelled
        else -> groupBuy.status
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onToggleExpand
    ) {
        Column(Modifier.padding(14.dp)) {
            // ── En-tête : produit + statut ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(groupBuy.productTitle.ifBlank { ts.productFallback.replace("%s", "${groupBuy.productId}") }, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ts.byCreator.replace("%s", groupBuy.creatorName.ifBlank { ts.creatorAnonymous }), fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("·", color = Color.LightGray)
                        Spacer(Modifier.width(8.dp))
                        Text(ts.participantCountFmt.replace("%d", "${groupBuy.participantsCount}"), fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Badge statut
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Infos prix et réduction ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(ts.originalPrice, fontSize = 11.sp, color = Color.Gray)
                    Text(FormatUtils.formatPrice(groupBuy.originalPrice), fontSize = 13.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${groupBuy.discountPct.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Green)
                    Text(ts.reduction, fontSize = 9.sp, color = Green, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(ts.groupPrice, fontSize = 11.sp, color = Color.Gray)
                    Text(FormatUtils.formatPrice(groupBuy.targetPrice), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenDark)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Barre de progression ──
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ts.progress, fontSize = 11.sp, color = Color.Gray)
                    Text("${groupBuy.currentQty}/${groupBuy.minQuantity}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = if (progress >= 1f) Green else Orange)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (progress >= 1f) Green else Orange,
                    trackColor = Color(0xFFE0E0E0),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            // ── Expiration ──
            if (groupBuy.expiresAt.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(ts.groupExpiry.replace("%s", groupBuy.expiresAt.take(10)), fontSize = 11.sp, color = Color.Gray)
                }
            }

            // ── Boutons d'action ──
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(ts.cancelGroup, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Bouton Supprimer définitivement (hard delete) — toujours visible
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(ts.delete, fontSize = 12.sp)
                }
            }

            // ── Participants (expand) ──
            if (isExpanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ts.participantsLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (canNotify) {
                        TextButton(
                            onClick = onNotifyAll,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, Modifier.size(14.dp), tint = Green)
                            Spacer(Modifier.width(4.dp))
                            Text(ts.notifyAll, fontSize = 11.sp, color = Green)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (participants.isEmpty()) {
                    Text(ts.loading, fontSize = 12.sp, color = Color.Gray)
                } else {
                    participants.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(32.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Text(p.name.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Green)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name.ifBlank { ts.anonymous }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("x${p.quantity} • ${p.joinedAt.take(10)}", fontSize = 11.sp, color = Color.Gray)
                            }
                            IconButton(
                                onClick = { onMessageParticipant(p.id, p.name) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Chat, null, Modifier.size(18.dp), tint = Green)
                            }
                        }
                    }
                }
                // Indicateur de fin
                Spacer(Modifier.height(4.dp))
                Icon(
                    Icons.Default.ExpandLess, null,
                    Modifier.size(20.dp).align(Alignment.CenterHorizontally),
                    tint = Color.Gray
                )
            } else {
                Spacer(Modifier.height(2.dp))
                Icon(
                    Icons.Default.ExpandMore, null,
                    Modifier.size(20.dp).align(Alignment.CenterHorizontally),
                    tint = Color.Gray
                )
            }
        }
    }
}

// ── Dialog de création (vendeur) ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorCreateGroupBuyDialog(
    products: List<ApiProduct>,
    onDismiss: () -> Unit,
    onCreate: (productId: Int, minQty: Int, discountPct: Double, hours: Int) -> Unit,
    ts: com.tik_market.utils.AppStrings
) {
    var selectedProduct by remember { mutableStateOf<ApiProduct?>(products.firstOrNull()) }
    var minQty by remember { mutableStateOf("5") }
    var discountPct by remember { mutableStateOf("10") }
    var hours by remember { mutableStateOf("48") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ts.newGroupBuy, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(ts.chooseProductOffer, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                // Sélection du produit
                Text(ts.productLabel, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.title ?: ts.selectProduct,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(prod.title, fontWeight = FontWeight.Medium)
                                        Text("${prod.price} FCFA", fontSize = 12.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedProduct = prod
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Participants
                    OutlinedTextField(
                        value = minQty,
                        onValueChange = { minQty = it.filter { c -> c.isDigit() } },
                        label = { Text(ts.minParticipants) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    // Réduction
                    OutlinedTextField(
                        value = discountPct,
                        onValueChange = { discountPct = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(ts.discountPercent) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Durée
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter { c -> c.isDigit() } },
                    label = { Text(ts.offerDuration) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                if (selectedProduct != null) {
                    val disc = discountPct.toDoubleOrNull() ?: 0.0
                    val targetPrice = selectedProduct!!.price * (1 - disc / 100)
                    Spacer(Modifier.height(12.dp))
                    Surface(color = Green.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp)) {
                        Text(ts.finalClientPrice.replace("%s", FormatUtils.formatPrice(targetPrice)),
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            color = GreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prodId = selectedProduct?.id ?: return@Button
                    val qty = minQty.toIntOrNull() ?: 5
                    val disc = discountPct.toDoubleOrNull() ?: 10.0
                    val h = hours.toIntOrNull() ?: 48
                    onCreate(prodId, qty, disc, h)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedProduct != null
            ) {
                Text(ts.launchOffer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(ts.cancel) }
        }
    )
}

// ── Dialog de notification (vendeur) ──
@Composable
private fun VendorNotifyGroupDialog(
    groupBuy: ApiGroupBuy,
    onDismiss: () -> Unit,
    onSend: (title: String, message: String) -> Unit,
    ts: com.tik_market.utils.AppStrings
) {
    var title by remember { mutableStateOf(ts.groupBuyNotifTitle.replace("%s", groupBuy.productTitle)) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ts.notifyParticipantsTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(ts.sendNotificationParticipants.replace("%d", "${groupBuy.participantsCount}"), fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(ts.notifTitle) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(ts.messageLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, message) },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                shape = RoundedCornerShape(12.dp),
                enabled = message.isNotBlank()
            ) {
                Text(ts.send)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(ts.cancel) }
        }
    )
}
