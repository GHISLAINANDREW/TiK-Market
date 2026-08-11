package com.tik_market.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.theme.*
import com.tik_market.ui.components.TiKCard
import com.tik_market.ui.components.TiKCardElevation
import com.tik_market.utils.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<ApiSuperAdminResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var isBroadcasting by remember { mutableStateOf(false) }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                data = ApiClient.fetchSuperAdminData()
            } catch (e: Exception) {
                errorMessage = e.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Super Admin Panel", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showBroadcastDialog = true }) {
                        Icon(Icons.Default.Campaign, null, tint = Color.White)
                    }
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = RedAccent)
                }
            } else {
                val d = data!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Configuration Système", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        ConfigCard(d.config)
                    }

                    item {
                        Text("Statistiques Globales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        SuperStatsGrid(d.stats)
                    }

                    item {
                        Text("Signalements (${d.reports.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    items(d.reports) { report ->
                        ReportItem(report, onAction = { loadData() })
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBroadcasting) showBroadcastDialog = false },
            title = { Text("Diffusion Globale") },
            text = {
                Column {
                    OutlinedTextField(value = broadcastTitle, onValueChange = { broadcastTitle = it }, label = { Text("Titre") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = broadcastMessage, onValueChange = { broadcastMessage = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isBroadcasting = true
                            ApiClient.broadcastSystemMessage(broadcastTitle, broadcastMessage)
                            isBroadcasting = false
                            showBroadcastDialog = false
                            broadcastTitle = ""
                            broadcastMessage = ""
                        }
                    },
                    enabled = !isBroadcasting && broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()
                ) {
                    if (isBroadcasting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                    else Text("Diffuser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }, enabled = !isBroadcasting) { Text("Annuler") }
            }
        )
    }
}

@Composable
fun ConfigCard(config: ApiSystemConfig) {
    TiKCard(elevation = TiKCardElevation.Low) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Version App", color = TextSecondary)
                Text(config.appVersion, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Version Min Requise", color = TextSecondary)
                Text(config.minVersion, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Taux Commission", color = TextSecondary)
                Text("${config.commissionRate}%", color = Green, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mode Maintenance", color = TextSecondary)
                Switch(checked = config.maintenanceMode, onCheckedChange = {})
            }
        }
    }
}

@Composable
fun SuperStatsGrid(stats: ApiSuperStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val totalUsers = stats.users.sumOf { it.count }
        val totalShops = stats.shops.sumOf { it.count }
        val activeProducts = stats.products.find { it.isActive == 1 }?.count ?: 0
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiBox("Utilisateurs", totalUsers.toString(), BlueAccent, Modifier.weight(1f))
            KpiBox("Boutiques", totalShops.toString(), Orange, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiBox("Produits Actifs", activeProducts.toString(), Green, Modifier.weight(1f))
            val totalRev = stats.revenue.sumOf { it.total }
            KpiBox("CA Global", FormatUtils.formatPrice(totalRev), Violet, Modifier.weight(1f))
        }
    }
}

@Composable
fun KpiBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
fun ReportItem(report: ApiReport, onAction: () -> Unit) {
    val scope = rememberCoroutineScope()
    TiKCard(elevation = TiKCardElevation.Low) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when(report.type) {
                        "product" -> Icons.Default.ShoppingBag
                        "user" -> Icons.Default.Person
                        else -> Icons.AutoMirrored.Filled.Message
                    },
                    null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Signalement ${report.type.uppercase()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                StatusBadge(report.status)
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Par: ${report.reporterName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("Raison: ${report.reason}", fontWeight = FontWeight.Medium)
            if (!report.comment.isNullOrBlank()) {
                Text(report.comment, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (report.status == "pending") {
                    Button(
                        onClick = {
                            scope.launch {
                                ApiClient.updateReportStatus(report.id, "resolved")
                                onAction()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Résoudre", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                ApiClient.updateReportStatus(report.id, "dismissed")
                                onAction()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ignorer", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when(status) {
        "pending" -> Orange
        "resolved" -> Green
        else -> TextTertiary
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            status.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
