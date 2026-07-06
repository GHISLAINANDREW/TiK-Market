package com.dschangmarket.ui.loyalty

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.*
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.*
import com.dschangmarket.utils.FormatUtils
import kotlinx.coroutines.launch

private val tierColors = mapOf(
    "bronze" to Color(0xFF8D6E63),
    "argent" to Color(0xFF9E9E9E),
    "or" to Color(0xFFFFD700)
)

private val tierGradients = mapOf(
    "bronze" to listOf(Color(0xFF8D6E63), Color(0xFFA1887F)),
    "argent" to listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD)),
    "or" to listOf(Color(0xFFFFD700), Color(0xFFFFF176))
)

private val tierNames = mapOf("bronze" to "Bronze", "argent" to "Argent", "or" to "Or")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyScreen(
    onBack: () -> Unit,
    onCouponClick: (String) -> Unit = {}
) {
    var wallet by remember { mutableStateOf<ApiWallet?>(null) }
    var transactions by remember { mutableStateOf<List<ApiWalletTransaction>>(emptyList()) }
    var coupons by remember { mutableStateOf<List<ApiCoupon>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRedeemDialog by remember { mutableStateOf(false) }
    var showRechargeDialog by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            wallet = ApiClient.fetchWallet()
            transactions = ApiClient.fetchWalletTransactions()
            coupons = ApiClient.fetchCoupons()
        } catch (_: Exception) { }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Programme fidélité", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
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
                // ── Carte de fidélité ──
                item { LoyaltyCard(wallet) }

                // ── Niveau suivant ──
                wallet?.nextTier?.let { next ->
                    if (next.name.isNotBlank()) {
                        item { NextTierCard(wallet!!, next) }
                    }
                }

                // ── Actions rapides ──
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionChip(
                            icon = Icons.Default.CardGiftcard,
                            text = "Échanger points",
                            color = Orange,
                            onClick = { showRedeemDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        ActionChip(
                            icon = Icons.Default.AccountBalanceWallet,
                            text = "Recharger",
                            color = Green,
                            onClick = { showRechargeDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Tabs: Transactions / Coupons ──
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TabButton("Historique", tab == 0, onClick = { tab = 0 }, Modifier.weight(1f))
                                TabButton("Mes coupons (${coupons.size})", tab == 1, onClick = { tab = 1 }, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))

                            if (tab == 0) {
                                if (transactions.isEmpty()) {
                                    EmptyStateIcon(Icons.Default.Receipt, "Aucune transaction")
                                } else {
                                    transactions.forEachIndexed { i, tx ->
                                        if (i > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                                        TransactionRow(tx)
                                    }
                                }
                            } else {
                                if (coupons.isEmpty()) {
                                    EmptyStateIcon(Icons.Default.CardGiftcard, "Aucun coupon")
                                } else {
                                    coupons.forEach { c -> CouponCard(c, onClick = { onCouponClick(c.code) }) }
                                }
                            }
                        }
                    }
                }

                // ── Grille des avantages ──
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Avantages par niveau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            TierAdvantageRow("Bronze", "0 pts", "1% cashback", Color(0xFF8D6E63), isActive = wallet?.tier == "bronze")
                            TierAdvantageRow("Argent", "500 pts", "2% cashback + 5% bonus", Color(0xFF9E9E9E), isActive = wallet?.tier == "argent")
                            TierAdvantageRow("Or", "2000 pts", "3.5% cashback + 10% bonus", Color(0xFFFFD700), isActive = wallet?.tier == "or")
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ─── Dialog échange points ───
    if (showRedeemDialog) {
        RedeemPointsDialog(
            currentPoints = wallet?.currentPoints ?: 0,
            onDismiss = { showRedeemDialog = false },
            onRedeem = { points ->
                scope.launch {
                    try {
                        val resp = ApiClient.redeemPoints(points)
                        if (resp.success && resp.coupon != null) {
                            snackbar.showSnackbar("Coupon ${resp.coupon.code} généré !")
                            showRedeemDialog = false
                            wallet = ApiClient.fetchWallet()
                            coupons = ApiClient.fetchCoupons()
                        } else {
                            snackbar.showSnackbar("Erreur lors de l'échange")
                        }
                    } catch (e: Exception) {
                        snackbar.showSnackbar(e.message ?: "Erreur")
                    }
                }
            }
        )
    }

    // ─── Dialog recharge ───
    if (showRechargeDialog) {
        RechargeWalletDialog(
            onDismiss = { showRechargeDialog = false },
            onRecharge = { amount, method ->
                scope.launch {
                    try {
                        val resp = ApiClient.rechargeWallet(amount, method)
                        if (resp.success) {
                            snackbar.showSnackbar("Recharge de ${amount.toInt()} FCFA effectuée")
                            showRechargeDialog = false
                            wallet = ApiClient.fetchWallet()
                            transactions = ApiClient.fetchWalletTransactions()
                        } else {
                            snackbar.showSnackbar("Erreur de recharge")
                        }
                    } catch (e: Exception) {
                        snackbar.showSnackbar(e.message ?: "Erreur")
                    }
                }
            }
        )
    }
}

// ── Carte de fidélité principale ──
@Composable
private fun LoyaltyCard(wallet: ApiWallet?) {
    val w = wallet ?: return
    val tierColor = tierColors[w.tier] ?: Color(0xFF8D6E63)
    val gradient = tierGradients[w.tier] ?: listOf(Color(0xFF8D6E63), Color(0xFFA1887F))

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp)
    ) {
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(gradient))) {
            Column(Modifier.padding(20.dp).fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MilitaryTech, null, Modifier.size(28.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Carte ${tierNames[w.tier] ?: w.tier}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text("${w.balance.toInt()} FCFA", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
                Text("Solde cashback disponible", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${w.currentPoints} pts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Points actuels", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${w.totalPoints} pts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Points cumulés", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// ── Carte progression niveau suivant ──
@Composable
private fun NextTierCard(wallet: ApiWallet, next: ApiNextTier) {
    val currentTierPoints = when (wallet.tier) {
        "bronze" -> 0
        "argent" -> 500
        else -> 2000
    }
    val nextTierPoints = when (next.name) {
        "argent" -> 500
        "or" -> 2000
        else -> 2000
    }
    val range = (nextTierPoints - currentTierPoints).coerceAtLeast(1)
    val progress = ((wallet.totalPoints - currentTierPoints).toFloat() / range).coerceIn(0f, 1f)
    val nextColor = tierColors[next.name] ?: Color.Gray

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, Modifier.size(20.dp), tint = nextColor)
                Spacer(Modifier.width(8.dp))
                Text("Prochain niveau : ${tierNames[next.name] ?: next.name}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = nextColor)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = nextColor,
                trackColor = Color(0xFFE0E0E0),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text("Plus que ${next.pointsNeeded} points pour atteindre ${tierNames[next.name] ?: next.name}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ── Transaction row ──
@Composable
private fun TransactionRow(tx: ApiWalletTransaction) {
    val icon = when (tx.type) {
        "earn" -> Icons.Default.AddCircle
        "spend" -> Icons.Default.RemoveCircle
        "recharge" -> Icons.Default.AccountBalanceWallet
        "cashback" -> Icons.Default.CurrencyExchange
        "bonus" -> Icons.Default.CardGiftcard
        "refund" -> Icons.Default.Refresh
        else -> Icons.Default.Receipt
    }
    val color = when (tx.type) {
        "earn", "cashback", "bonus", "refund" -> Green
        "spend" -> Color.Red
        "recharge" -> Orange
        else -> Color.Gray
    }
    val label = when (tx.type) {
        "earn" -> "Gagné"
        "spend" -> "Dépensé"
        "recharge" -> "Recharge"
        "cashback" -> "Cashback"
        "bonus" -> "Bonus"
        "refund" -> "Remboursement"
        else -> tx.type
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.description.ifBlank { label }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(tx.createdAt.take(10), fontSize = 11.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (tx.amountFcfa > 0) {
                Text("+${tx.amountFcfa.toInt()} FCFA", fontSize = 13.sp, color = Green, fontWeight = FontWeight.SemiBold)
            }
            if (tx.points > 0) {
                Text("+${tx.points} pts", fontSize = 11.sp, color = GreenAccent)
            }
        }
    }
}

// ── Coupon card ──
@Composable
private fun CouponCard(coupon: ApiCoupon, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Orange.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ConfirmationNumber, null, Modifier.size(24.dp), tint = Orange)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(coupon.code, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
                Row {
                    if (coupon.discountFcfa != null && coupon.discountFcfa > 0) {
                        Text("${coupon.discountFcfa.toInt()} FCFA de réduction", fontSize = 12.sp, color = Orange, fontWeight = FontWeight.Medium)
                    } else if (coupon.discountPct != null && coupon.discountPct > 0) {
                        Text("${coupon.discountPct.toInt()}% de réduction", fontSize = 12.sp, color = Orange, fontWeight = FontWeight.Medium)
                    }
                }
                coupon.expiresAt?.let {
                    Text("Expire le ${it.take(10)}", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp), tint = Orange)
        }
    }
}

// ── Échange points dialog ──
@Composable
private fun RedeemPointsDialog(currentPoints: Int, onDismiss: () -> Unit, onRedeem: (Int) -> Unit) {
    var points by remember { mutableStateOf("100") }
    val pts = points.toIntOrNull() ?: 0
    val couponValue = (pts / 100) * 500

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Échanger mes points") },
        text = {
            Column(modifier = Modifier.width(300.dp)) {
                Text("Vous avez $currentPoints points", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Points à échanger") },
                    supportingText = { Text("100 points = 500 FCFA. Valeur: $couponValue FCFA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            DschangButton(
                text = "Échanger",
                onClick = {
                    val p = (pts / 100) * 100
                    if (p in 100..currentPoints) onRedeem(p)
                },
                variant = DschangButtonVariant.Primary,
                fullWidth = false,
                enabled = pts in 100..currentPoints && pts % 100 == 0
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// ── Recharge dialog ──
@Composable
private fun RechargeWalletDialog(onDismiss: () -> Unit, onRecharge: (Double, String) -> Unit) {
    var amount by remember { mutableStateOf("1000") }
    var method by remember { mutableStateOf("orange") }
    val methods = listOf("orange" to "Orange Money", "mtn" to "MTN Mobile Money", "other" to "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recharger mon portefeuille") },
        text = {
            Column(modifier = Modifier.width(320.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("Montant (FCFA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Moyen de paiement", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                methods.forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth().clickable { method = key }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == key, onClick = { method = key })
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            DschangButton(
                text = "Recharger",
                onClick = { onRecharge(amount.toDoubleOrNull() ?: 1000.0, method) },
                variant = DschangButtonVariant.Primary,
                fullWidth = false,
                enabled = (amount.toIntOrNull() ?: 0) >= 100
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// ── Composants réutilisables ──
@Composable
private fun ActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(72.dp), shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.1f)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(4.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp), color = if (isSelected) Green else Color(0xFFF0F0F0)) {
        Text(text, modifier = Modifier.padding(vertical = 10.dp), fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) Color.White else Color.DarkGray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyStateIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(40.dp), tint = Color.LightGray)
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun TierAdvantageRow(name: String, points: String, advantage: String, color: Color, isActive: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(if (isActive) color else Color(0xFFE0E0E0), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            if (isActive) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.White)
            else Text(name.take(1), fontWeight = FontWeight.Bold, color = Color.Gray)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$name ($points)", fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) color else Color.DarkGray)
            Text(advantage, fontSize = 11.sp, color = Color.Gray)
        }
        if (isActive) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
                Text("ACTIF", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
