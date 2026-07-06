package com.dschangmarket.ui.checkout

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.*
import com.dschangmarket.data.models.CartItem
import com.dschangmarket.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    items: List<CartItem>,
    totalAmount: Double,
    onBack: () -> Unit,
    onPlaceOrder: (shippingAddress: String, phone: String, notes: String, paymentMethod: String) -> Unit
) {
    var address by remember { mutableStateOf("Dschang, Cameroun") }
    var phone by remember { mutableStateOf("+237 6") }
    var notes by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("Orange Money") }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var placing by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var promoDiscount by remember { mutableStateOf(0) }
    var promoValid by remember { mutableStateOf(false) }
    var promoChecking by remember { mutableStateOf(false) }
    var promoError by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val effectiveTotal = (totalAmount - promoDiscount).coerceAtLeast(0.0)
    
    // Delivery fee simulation
    val deliveryFee = when {
        address.lowercase().contains("dschang") -> 500
        address.lowercase().contains("keleng") || address.lowercase().contains("foto") -> 700
        address.isNotBlank() -> 1500
        else -> 0
    }
    
    val finalTotal = effectiveTotal + deliveryFee

    val paymentMethods = listOf("Orange Money", "MTN Mobile Money", "Paiement à la livraison")

    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

    Scaffold(topBar = {
        TopAppBar(title = { Text("Finaliser ma commande", fontWeight = FontWeight.SemiBold, color = Color.White) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor))
    }, bottomBar = {
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Total", fontSize = 13.sp, color = Color.Gray)
                    if (promoDiscount > 0) {
                        Text("${totalAmount.toInt()} FCFA", fontSize = 13.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                    Text("${finalTotal.toInt()} FCFA", fontSize = if (isCompact) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                }
                Button(
                    onClick = {
                        placing = true
                        onPlaceOrder(address, phone, notes, selectedPayment)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTopBarColor),
                    enabled = !placing && address.isNotBlank() && phone.isNotBlank()
                ) {
                    if (placing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Commander", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
            // Delivery address
            Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Adresse de livraison", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it },
                        label = { Text("Adresse") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it },
                        label = { Text("Téléphone de contact") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                }
            }

            // Payment method
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Moyen de paiement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    paymentMethods.forEach { method ->
                        val isSelected = selectedPayment == method
                        Surface(
                            onClick = { selectedPayment = method },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GreenSurface else Color(0xFFFAFAFA),
                            border = if (isSelected) BorderStroke(1.dp, Green) else null
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { selectedPayment = method },
                                    colors = RadioButtonDefaults.colors(selectedColor = Green))
                                Spacer(Modifier.width(8.dp))
                                val icon = when {
                                    method.contains("Orange") -> Icons.Default.PhoneAndroid
                                    method.contains("MTN") -> Icons.Default.PhoneIphone
                                    else -> Icons.Default.Payments
                                }
                                Icon(icon, null, Modifier.size(24.dp), tint = if (isSelected) Green else Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text(method, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                                if (isSelected) Spacer(Modifier.weight(1f))
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = Green)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Promo code
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Code promo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it.uppercase(); promoError = ""; promoValid = false; promoDiscount = 0 },
                            label = { Text("Entrez votre code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (promoValid) Green else if (promoError.isNotBlank()) Color.Red else Green,
                                focusedLabelColor = Green
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (promoCode.isNotBlank()) {
                                    promoChecking = true
                                    coroutineScope.launch {
                                        try {
                                            val result = ApiClient.validatePromoCode(promoCode, totalAmount)
                                            if (result.valid && result.promotion != null) {
                                                promoValid = true
                                                promoDiscount = result.discount
                                                promoError = ""
                                            } else {
                                                promoValid = false
                                                promoDiscount = 0
                                                promoError = result.error ?: "Code invalide"
                                            }
                                        } catch (_: Exception) {
                                            promoError = "Erreur de validation"
                                        }
                                        promoChecking = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = promoCode.isNotBlank() && !promoChecking,
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            if (promoChecking) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Appliquer", fontSize = 12.sp)
                            }
                        }
                    }
                    if (promoValid) {
                        Text("✅ Réduction de $promoDiscount FCFA appliquée !", fontSize = 12.sp, color = Green, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (promoError.isNotBlank()) {
                        Text("❌ $promoError", fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Order items summary
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Articles (${items.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${item.quantity}x", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Green)
                            Spacer(Modifier.width(8.dp))
                            Text(item.product.title, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("${item.subtotal.toInt()} FCFA", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
                    if (promoDiscount > 0) {
                        Row(Modifier.fillMaxWidth()) {
                            Text("Sous-total", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            Text("${totalAmount.toInt()} FCFA", fontSize = 14.sp, color = Color.Gray)
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text("Remise (-$promoDiscount)", fontSize = 14.sp, color = Color.Red, modifier = Modifier.weight(1f))
                            Text("-$promoDiscount FCFA", fontSize = 14.sp, color = Color.Red)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Frais de livraison", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                        Text("$deliveryFee FCFA", fontSize = 14.sp, color = Color.Gray)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("${finalTotal.toInt()} FCFA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Green)
                    }
                }
            }

            // Notes
            Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text("Instructions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notes pour le vendeur (optionnel)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
    } // BoxWithConstraints
}
