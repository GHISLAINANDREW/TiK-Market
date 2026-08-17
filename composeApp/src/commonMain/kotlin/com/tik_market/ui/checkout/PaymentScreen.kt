package com.tik_market.ui.checkout

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.*
import com.tik_market.utils.AppStrings
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    orderNumber: String = "CMD-20260615-001",
    amount: Double = 12500.0,
    paymentMethod: String = "Orange Money",
    phone: String = "+237 690 00 00 01",
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onDone: () -> Unit
) {
    val s = LocalAppStrings.current
    var step by remember { mutableStateOf(0) } // 0: confirm, 1: processing, 2: success
    var paymentPhone by remember { mutableStateOf(phone) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(s.paymentTitle, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, navigationIconContentColor = Color.White))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> StepConfirmPayment(amount, paymentMethod, paymentPhone, onPhoneChange = { paymentPhone = it },
                    onConfirm = { step = 1 }, s = s)
                1 -> StepProcessing(amount, s) { step = 2 }
                2 -> StepSuccess(orderNumber, amount, onDone, s)
            }
        }
    }
}

@Composable
private fun StepConfirmPayment(amount: Double, method: String, phone: String, onPhoneChange: (String) -> Unit, onConfirm: () -> Unit, s: AppStrings) {
    Column(Modifier.padding(16.dp)) {
        // Amount card
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Green)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.amountToPay, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("${amount.toInt()} FCFA", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(method, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Phone input
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, Modifier.size(24.dp), tint = Green)
                    Spacer(Modifier.width(8.dp))
                    Text(s.phoneNumberPrefix.format(method), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = phone, onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(s.phone) },
                    leadingIcon = { Text("+237", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                Spacer(Modifier.height(8.dp))
                Text(s.step2ConfirmDesc, fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) {
            Text(s.confirmPayment, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Payment info
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text(s.howItWorks, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "${s.phoneNumberPrefix.format("Mobile Money")}" to "Orange Money ou MTN Mobile Money",
                    s.step2Confirm to s.step2ConfirmDesc,
                    s.step3Validate to s.step3ValidateDesc
                ).forEach { (title, desc) ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Box(Modifier.size(6.dp).background(Green, RoundedCornerShape(3.dp)).offset(y = 6.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(desc, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProcessing(amount: Double, s: AppStrings, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = Green, modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
        Spacer(Modifier.height(24.dp))
        Text(s.processingPayment, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(s.confirmOnPhone, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("${amount.toInt()} FCFA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Green)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, shape = RoundedCornerShape(12.dp)) {
            Text(s.paymentDone, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDone) { Text(s.paymentAlreadyDone, color = Color.Gray, fontSize = 13.sp) }
    }
}

@Composable
private fun StepSuccess(orderNumber: String, amount: Double, onDone: () -> Unit, s: AppStrings) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(Modifier.size(80.dp), shape = RoundedCornerShape(40.dp), color = GreenSurface) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp), tint = Green)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(s.paymentSuccess, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Green)
        Spacer(Modifier.height(8.dp))
        Text(s.orderConfirmedFmt.format(orderNumber), color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("${s.amount} : ${amount.toInt()} FCFA", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) {
            Text(s.returnToHome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
