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
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ts = LocalAppStrings.current
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
                title = { Text(ts.superAdminPanel, color = Color.White) },
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
                        Text(ts.systemConfig, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        ConfigCard(d.config, ts)
                    }

                    item {
                        Text(ts.globalStats, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        SuperStatsGrid(d.stats, ts)
                    }

                    item {
                        Text(ts.reportsCount.replace("%d", "${d.reports.size}"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    items(d.reports) { report ->
                        ReportItem(report, onAction = { loadData() }, ts)
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBroadcasting) showBroadcastDialog = false },
            title = { Text(ts.globalBroadcast) },
            text = {
                Column {
                    OutlinedTextField(value = broadcastTitle, onValueChange = { broadcastTitle = it }, label = { Text(ts.notifTitle) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = broadcastMessage, onValueChange = { broadcastMessage = it }, label = { Text(ts.messageLabel) }, modifier = Modifier.fillMaxWidth().height(100.dp))
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
                    else Text(ts.broadcast)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }, enabled = !isBroadcasting) { Text(ts.cancel) }
            }
        )
    }
}

@Composable
fun ConfigCard(config: ApiSystemConfig, ts: com.tik_market.utils.AppStrings) {
    TiKCard(elevation = TiKCardElevation.Low) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ts.appVersionLabel, color = TextSecondary)
                Text(config.appVersion, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ts.minVersionRequired, color = TextSecondary)
                Text(config.minVersion, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ts.commissionRate, color = TextSecondary)
                Text("${config.commissionRate}%", color = Green, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(ts.maintenanceMode, color = TextSecondary)
                Switch(checked = config.maintenanceMode, onCheckedChange = {})
            }
        }
    }
}

@Composable
fun SuperStatsGrid(stats: ApiSuperStats, ts: com.tik_market.utils.AppStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val totalUsers = stats.users.sumOf { it.count }
        val totalShops = stats.shops.sumOf { it.count }
        val activeProducts = stats.products.find { it.isActive == 1 }?.count ?: 0
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiBox(ts.usersLabel, totalUsers.toString(), BlueAccent, Modifier.weight(1f))
            KpiBox(ts.shop, totalShops.toString(), Orange, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiBox(ts.activeProducts, activeProducts.toString(), Green, Modifier.weight(1f))
            val totalRev = stats.revenue.sumOf { it.total }
            KpiBox(ts.globalCA, FormatUtils.formatPrice(totalRev), Violet, Modifier.weight(1f))
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
fun ReportItem(report: ApiReport, onAction: () -> Unit, ts: com.tik_market.utils.AppStrings) {
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
                Text(ts.reportTypeLabel.replace("%s", report.type.uppercase()), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                StatusBadge(report.status, ts)
            }
            
            Spacer(Modifier.height(8.dp))
            Text(ts.byReporter.replace("%s", report.reporterName), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(ts.reasonPrefix.replace("%s", report.reason), fontWeight = FontWeight.Medium)
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
                        Text(ts.resolve, fontSize = 12.sp)
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
                        Text(ts.ignore, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, ts: com.tik_market.utils.AppStrings) {
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
