package com.tik_market.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.dto.ApiGroupBuy
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import com.tik_market.utils.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Section "Achat groupé" pour la fiche produit.
 * Affiche les group-buys ouverts + bouton pour créer/rejoindre.
 */
@Composable
fun GroupBuySection(
    productId: Int,
    shopId: Int,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var groupBuys by remember { mutableStateOf<List<ApiGroupBuy>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Charger les group-buys ouverts
    LaunchedEffect(productId) {
        loading = true
        groupBuys = ApiClient.fetchGroupBuys(productId)
        loading = false
    }

    // Ne rien afficher si pas de chargement et pas de groupes
    if (!loading && groupBuys.isEmpty()) return

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HorizontalDivider(color = DividerGray)
        Spacer(Modifier.height(16.dp))

        // ── En-tête ──
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, null, Modifier.size(20.dp), tint = Green)
                Spacer(Modifier.width(8.dp))
                Text("Achat groupé en cours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (loading) {
            Spacer(Modifier.height(8.dp))
            TiKShimmer(modifier = Modifier.fillMaxWidth().height(80.dp))
        } else {
            Spacer(Modifier.height(8.dp))
            groupBuys.forEach { gb ->
                ActiveGroupBuyCard(gb, onJoin = { qty ->
                    scope.launch {
                        try {
                            ApiClient.joinGroupBuy(gb.id, qty)
                            // Recharger
                            groupBuys = ApiClient.fetchGroupBuys(productId)
                        } catch (_: Exception) { }
                    }
                })
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ── Carte d'un group-buy actif ──
@Composable
private fun ActiveGroupBuyCard(
    gb: ApiGroupBuy,
    onJoin: (Int) -> Unit
) {
    val progress = if (gb.minQuantity > 0) (gb.currentQty.toFloat() / gb.minQuantity).coerceAtMost(1f) else 0f
    val remaining = gb.minQuantity - gb.currentQty
    val timeLeft = rememberCountdown(gb.expiresAt)

    TiKCard(elevation = TiKCardElevation.Low) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${gb.discountPct.toInt()}% de réduction", style = MaterialTheme.typography.titleSmall, color = Green, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        TiKBadge(text = "${gb.currentQty}/${gb.minQuantity}", color = TiKBadgeColor.Orange)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Prix groupe: ${FormatUtils.formatPrice(gb.targetPrice)} au lieu de ${FormatUtils.formatPrice(gb.originalPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Lancé par ${gb.creatorName.ifBlank { "un acheteur" }}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TiKButton(
                        text = if (remaining > 0) "Rejoindre" else "✅ OK",
                        onClick = { onJoin(1) },
                        variant = if (remaining > 0) TiKButtonVariant.Primary else TiKButtonVariant.Secondary,
                        fullWidth = false,
                        modifier = Modifier.height(36.dp).widthIn(min = 90.dp)
                    )
                    if (remaining > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text("Plus que $remaining", style = MaterialTheme.typography.labelSmall, color = Orange)
                    }
                }
            }

            // Barre de progression
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progression", fontSize = 10.sp, color = Color.Gray)
                Text("${gb.currentQty}/${gb.minQuantity}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (progress >= 1f) Green else Orange)
            }
            Spacer(Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (progress >= 1f) Green else Orange,
                trackColor = Color(0xFFE0E0E0),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Compte à rebours
            if (timeLeft.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, Modifier.size(14.dp), tint = if (timeLeft.startsWith("0")) Color.Red else Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (timeLeft.startsWith("0")) "Expiré !" else "Temps restant : $timeLeft",
                        fontSize = 11.sp,
                        color = if (timeLeft.startsWith("0")) Color.Red else Color.Gray,
                        fontWeight = if (timeLeft.startsWith("0")) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/** Compte à rebours simple : montre le temps restant formaté */
@Composable
private fun rememberCountdown(expiresAt: String): String {
    if (expiresAt.isBlank()) return ""
    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(expiresAt) {
        while (true) { delay(30_000L); ticks++ }
    }
    // Parse "2026-07-03 15:30:00" → estimate remaining hours
    try {
        val cleaned = expiresAt.replace("T", " ").substringBefore(".")
        val parts = cleaned.split(" ", "-", ":")
        if (parts.size < 5) return ""
        val y = parts[0].toInt(); val m = parts[1].toInt(); val d = parts[2].toInt()
        val hh = parts[3].toInt(); val mm = parts[4].toInt()
        // Simple: compare with "now" using a rough day count
        val expDays = y * 365 + m * 30 + d
        val expMins = expDays * 1440 + hh * 60 + mm - ticks * 30 // subtract elapsed ticks
        if (expMins <= 0) return "0h 0m"
        return "${expMins / 60}h ${expMins % 60}m"
    } catch (_: Exception) { return "" }
}

// ── Dialog création ──
// Removed as vendors now initiate group buys from their dashboard
