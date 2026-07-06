package com.dschangmarket.ui.barcode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.data.models.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    onBack: () -> Unit,
    onResult: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var barcode by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Scan code-barres") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } }
        )

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Utilisez la caméra de votre appareil", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("ou saisissez le code manuellement", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it.filter { c -> c.isDigit() || c.isLetter() }; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: 4901234567890") },
                label = { Text("Code-barres") },
                leadingIcon = { Icon(Icons.Filled.QrCode, null) },
                trailingIcon = {
                    if (barcode.isNotEmpty()) {
                        Row {
                            IconButton(onClick = { barcode = ""; results = emptyList(); errorMsg = null }) { Icon(Icons.Filled.Clear, "Effacer") }
                            IconButton(onClick = {
                                isSearching = true; errorMsg = null
                                scope.launch {
                                    try {
                                        onResult(barcode)
                                    } catch (e: Exception) { errorMsg = "Erreur: ${e.message}" }
                                    finally { isSearching = false }
                                }
                            }, enabled = barcode.length >= 3) { Icon(Icons.Filled.Search, "Chercher") }
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (barcode.length >= 3) { isSearching = true; scope.launch { onResult(barcode) }; isSearching = false }
                }),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp)); Text(it, color = Color(0xFFC62828))
                    }
                }
            }
            if (isSearching) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
    }
}
