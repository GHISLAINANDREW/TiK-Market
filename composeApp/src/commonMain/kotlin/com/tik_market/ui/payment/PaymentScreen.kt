package com.tik_market.ui.payment

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.tik_market.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    order: ApiOrder,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var paymentStatus by remember { mutableStateOf<String?>(null) }

    val providers = listOf("MTN", "Orange", "Moov", "Camtel")
    val itemsCount = order.items?.size ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Récapitulatif commande
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF5F5FF)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Récapitulatif", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Commande #${order.id}", fontSize = 14.sp)
                        Text("${order.totalAmount} FCFA", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("$itemsCount article(s)", fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sélection du provider
            Text("Choisissez votre opérateur", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                providers.forEach { p ->
                    val isSelected = provider == p
                    val color = when (p) {
                        "MTN" -> Color(0xFFFFCC00)
                        "Orange" -> Color(0xFFFF7900)
                        "Moov" -> Color(0xFF00A2E8)
                        else -> Color(0xFF008000)
                    }
                    Surface(
                        onClick = { provider = p },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) BorderStroke(2.dp, color) else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        color = if (isSelected) color.copy(alpha = 0.1f) else Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(p, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (isSelected) {
                                Spacer(Modifier.height(4.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Numéro de téléphone
            Text("Numéro Mobile Money", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it.filter { c -> c.isDigit() }
                    phoneError = null
                },
                placeholder = { Text("670000000") },
                leadingIcon = {
                    if (provider.isNotEmpty()) {
                        Text(provider, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                isError = phoneError != null,
                supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(8.dp))
            Text("Exemple : 670000000 (9 chiffres)", fontSize = 11.sp, color = Color.Gray)

            Spacer(Modifier.height(32.dp))

            // Bouton de paiement
            Button(
                onClick = {
                    if (provider.isEmpty()) {
                        phoneError = "Veuillez choisir un opérateur"
                        return@Button
                    }
                    if (phone.length < 8) {
                        phoneError = "Numéro invalide (9 chiffres attendus)"
                        return@Button
                    }
                    isProcessing = true
                    scope.launch {
                        try {
                            val result = ApiClient.initiatePayment(order.id, provider, phone)
                            paymentStatus = if (result.status == "completed") {
                                "✅ Paiement réussi !"
                            } else {
                                "❌ Échec du paiement"
                            }
                        } catch (e: Exception) {
                            paymentStatus = "❌ Erreur : ${e.message}"
                        }
                        isProcessing = false
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (provider == "Orange") Color(0xFFFF7900) else Color(0xFFFFCC00),
                    disabledContainerColor = Color.Gray
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Text(
                        "Payer ${order.totalAmount} FCFA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (provider == "Orange") Color.White else Color.Black
                    )
                }
            }

            // Message de statut
            paymentStatus?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(msg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (msg.startsWith("✅")) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSuccess,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retour aux commandes", color = Color.White)
                    }
                }
            }
        }
    }
}
