package com.tik_market.ui.checkout

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
import com.tik_market.api.*
import com.tik_market.data.models.CartItem
import com.tik_market.theme.*
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    items: List<CartItem>,
    totalAmount: Double,
    onBack: () -> Unit,
    onPlaceOrder: (shippingAddress: String, phone: String, notes: String, paymentMethod: String, paymentType: String) -> Unit
) {
    var address by remember { mutableStateOf("Dschang, Cameroun") }
    var phone by remember { mutableStateOf("+237 6") }
    var notes by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("Orange Money") }
    var placing by remember { mutableStateOf(false) }
    var promoCode by remember { mutableStateOf("") }
    var promoDiscount by remember { mutableStateOf(0) }
    var promoValid by remember { mutableStateOf(false) }
    var promoChecking by remember { mutableStateOf(false) }
    var promoError by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    // Payment type: 'delivery' or 'direct'
    var paymentType by remember { mutableStateOf("delivery") }

    val effectiveTotal = (totalAmount - promoDiscount).coerceAtLeast(0.0)
    
    val deliveryFee = when {
        address.lowercase().contains("dschang") -> 500
        address.lowercase().contains("keleng") || address.lowercase().contains("foto") -> 700
        address.isNotBlank() -> 1500
        else -> 0
    }
    
    val finalTotal = effectiveTotal + deliveryFee

    // Group vendors for direct payment display
    val vendors = remember(items) {
        items.map { it.product }
            .distinctBy { it.vendorId }
            .map { prod -> prod.shopName to prod.vendorPhone }
            .filter { (_, phone) -> phone.isNotBlank() }
    }

    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

    Scaffold(topBar = {
        TopAppBar(title = { Text(s.finalizeOrder, fontWeight = FontWeight.SemiBold, color = Color.White) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor))
    }, bottomBar = {
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.total, fontSize = 13.sp, color = Color.Gray)
                    if (promoDiscount > 0) {
                        Text("${totalAmount.toInt()} FCFA", fontSize = 13.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                    Text("${finalTotal.toInt()} FCFA", fontSize = if (isCompact) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                }
                Button(
                    onClick = {
                        placing = true
                        onPlaceOrder(address, phone, notes, selectedPayment, paymentType)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTopBarColor),
                    enabled = !placing && address.isNotBlank() && phone.isNotBlank()
                ) {
                    if (placing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else {
                        Text(
                            if (paymentType == "direct") s.payAndOrder else s.buyNow,
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
            // ── Address ──
            Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text(s.shippingAddress, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it },
                        label = { Text(s.addressLabel) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it },
                        label = { Text(s.contactPhone) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                }
            }

            // ── Payment type ──
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text(s.paymentMode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))

                    // Option: Paiement à la livraison
                    Surface(
                        onClick = { paymentType = "delivery" },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (paymentType == "delivery") GreenSurface else Color(0xFFFAFAFA),
                        border = if (paymentType == "delivery") BorderStroke(1.dp, Green) else null
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = paymentType == "delivery", onClick = { paymentType = "delivery" },
                                colors = RadioButtonDefaults.colors(selectedColor = Green))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.MoneyOff, null, Modifier.size(24.dp), tint = if (paymentType == "delivery") Green else Color.Gray)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.payOnDelivery, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(s.payOnDeliveryHint, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (paymentType == "delivery") Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = Green)
                        }
                    }

                    // Option: Paiement direct au vendeur
                    Surface(
                        onClick = { paymentType = "direct" },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (paymentType == "direct") GreenSurface else Color(0xFFFAFAFA),
                        border = if (paymentType == "direct") BorderStroke(1.dp, Green) else null
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = paymentType == "direct", onClick = { paymentType = "direct" },
                                colors = RadioButtonDefaults.colors(selectedColor = Green))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.PhoneAndroid, null, Modifier.size(24.dp), tint = if (paymentType == "direct") Green else Color.Gray)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.directToVendor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(s.directToVendorHint, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (paymentType == "direct") Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = Green)
                        }
                    }
                }
            }

            // ── Vendor info for direct payment ──
            if (paymentType == "direct") {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF8E1)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, Modifier.size(20.dp), tint = Color(0xFFF57F17))
                            Spacer(Modifier.width(8.dp))
                            Text(s.paymentInstructions, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFF57F17))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            s.transferInstructions.format(finalTotal.toInt(), s.payAndOrder),
                            fontSize = 13.sp,
                            color = Color(0xFF5D4037),
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        vendors.forEach { (shopName, vendorPhone) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(shopName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1C1C))
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, null, Modifier.size(18.dp), tint = Green)
                                        Spacer(Modifier.width(6.dp))
                                        Text(vendorPhone, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF075E54))
                                        Spacer(Modifier.weight(1f))
                                        OutlinedButton(
                                            onClick = { /* TODO: copy to clipboard */ },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(s.copy, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text("Montant à transférer : ${finalTotal.toInt()} FCFA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandTopBarColor)
                                }
                            }
                        }
                    }
                }
            }

            // ── Payment methods (only for delivery) ──
            if (paymentType == "delivery") {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp), tint = Green)
                            Spacer(Modifier.width(8.dp))
                            Text("Moyen de paiement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(s.cashOnDelivery, fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Promo code ──
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text(s.promoCode, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it.uppercase(); promoError = ""; promoValid = false; promoDiscount = 0 },
                            label = { Text(s.enterCode) },
                            modifier = Modifier.weight(1f),
                            singleLine = true, shape = RoundedCornerShape(12.dp),
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
                                                promoValid = true; promoDiscount = result.discount; promoError = ""
                                            } else {
promoValid = false; promoDiscount = 0; promoError = result.error ?: s.promoInvalid
                                        }
                                        } catch (_: Exception) { promoError = s.promoValidationError }
                                        promoChecking = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = promoCode.isNotBlank() && !promoChecking,
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            if (promoChecking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text(s.apply, fontSize = 12.sp)
                        }
                    }
                    if (promoValid) Text(s.promoApplied.format(promoDiscount), fontSize = 12.sp, color = Green, modifier = Modifier.padding(top = 4.dp))
                    if (promoError.isNotBlank()) Text("❌ $promoError", fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Order items summary ──
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text(s.articles.format(items.size), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            Text(s.subtotal, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                            Text("${totalAmount.toInt()} FCFA", fontSize = 14.sp, color = Color.Gray)
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text(s.discount.format(promoDiscount), fontSize = 14.sp, color = Color.Red, modifier = Modifier.weight(1f))
                            Text("-$promoDiscount FCFA", fontSize = 14.sp, color = Color.Red)
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(s.deliveryFee, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                        Text("$deliveryFee FCFA", fontSize = 14.sp, color = Color.Gray)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("${finalTotal.toInt()} FCFA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Green)
                    }
                }
            }

            // ── Notes ──
            Surface(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.size(20.dp), tint = Green)
                        Spacer(Modifier.width(8.dp))
                        Text(s.instructions, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text(s.notesForVendor) },
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
