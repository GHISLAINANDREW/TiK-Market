package com.tik_market.ui.vendor

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
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateReelScreen(
    onBack: () -> Unit
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    
    var videoDataUrl by remember { mutableStateOf<String?>(null) }
    var videoFileName by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var myProducts by remember { mutableStateOf<List<ApiProduct>>(emptyList()) }
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    val videoPicker = rememberMediaPickerLauncher(allowVideo = true, videoOnly = true) { result ->
        if (result != null) {
            videoDataUrl = result.dataUrl
            videoFileName = result.fileName
        }
    }

    LaunchedEffect(Unit) {
        isLoadingProducts = true
        try {
            val shop = ApiClient.fetchShopByVendor()
            if (shop != null) {
                myProducts = ApiClient.fetchProducts(shopId = shop.id)
            }
        } catch (_: Exception) {}
        isLoadingProducts = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publier un Reel", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Video Selection
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp).clickable { videoPicker() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (videoFileName != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Movie, null, Modifier.size(48.dp), tint = Green)
                            Spacer(Modifier.height(8.dp))
                            Text(videoFileName!!, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Cliquer pour changer", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddBox, null, Modifier.size(48.dp), tint = Color.LightGray)
                            Spacer(Modifier.height(8.dp))
                            Text("Sélectionner une vidéo", fontWeight = FontWeight.Bold)
                            Text("Format vertical recommandé (max 30s)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // 2. Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Légende (Optionnel)") },
                placeholder = { Text("Décrivez votre produit en quelques mots...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(20.dp))
            
            // 3. Product Link
            Text("Produit lié (Bientôt disponible)", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = { if (myProducts.isNotEmpty()) showProductPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, null, tint = Green)
                    Spacer(Modifier.width(12.dp))
                    val selectedProd = myProducts.find { it.id == selectedProductId }
                    Text(selectedProd?.title ?: "Choisir un produit de ma boutique", Modifier.weight(1f), fontSize = 14.sp)
                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // 4. Publish Button
            Button(
                onClick = {
                    scope.launch {
                        isPublishing = true
                        try {
                            if (videoDataUrl != null) {
                                val uploadedUrl = ApiClient.uploadImage(videoDataUrl!!, videoFileName!!)
                                val shop = ApiClient.fetchShopByVendor()
                                if (shop != null) {
                                    ApiClient.createReel(
                                        shopId = shop.id,
                                        videoUrl = uploadedUrl,
                                        description = description,
                                        productId = selectedProductId
                                    )
                                    onBack()
                                }
                            }
                        } catch (_: Exception) {}
                        isPublishing = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                enabled = !isPublishing && videoDataUrl != null
            ) {
                if (isPublishing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("PUBLIER MON REEL", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Lier un produit") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(myProducts) { prod ->
                        ListItem(
                            headlineContent = { Text(prod.title) },
                            supportingContent = { Text("${prod.price} FCFA") },
                            modifier = Modifier.clickable { selectedProductId = prod.id; showProductPicker = false }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProductPicker = false }) { Text("Annuler") } }
        )
    }
}
