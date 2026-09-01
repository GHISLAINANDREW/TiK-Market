package com.tik_market.ui.search

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.data.models.Product
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit
) {
    val s = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    
    var selectedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            selectedImageBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
            isSearching = true
            errorMsg = null
            scope.launch {
                try {
                    val apiResults = ApiClient.searchByImage(result.dataUrl)
                    results = apiResults.map { it.toProduct() }
                    if (results.isEmpty()) errorMsg = "Aucun produit similaire trouvé."
                } catch (e: Exception) {
                    errorMsg = "Erreur lors de la recherche visuelle."
                } finally {
                    isSearching = false
                }
            }
        }
    }

    val cameraLauncher = rememberTakePhotoLauncher { dataUrl ->
        if (dataUrl != null) {
            selectedImageBitmap = decodeDataUrlToImageBitmap(dataUrl)
            isSearching = true
            errorMsg = null
            scope.launch {
                try {
                    val apiResults = ApiClient.searchByImage(dataUrl)
                    results = apiResults.map { it.toProduct() }
                } catch (_: Exception) {
                    errorMsg = "Erreur réseau."
                } finally {
                    isSearching = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.visualSearchTitle.ifBlank { "Recherche visuelle" }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
            // Selection area
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedImageBitmap != null) {
                        Image(
                            bitmap = selectedImageBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Prenez une photo d'un produit pour le trouver", fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { cameraLauncher() }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Prendre une photo")
                        }
                        OutlinedButton(onClick = { imagePicker() }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Galerie")
                        }
                    }
                }
            }

            if (isSearching) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            } else if (errorMsg != null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = Color.Red)
                }
            } else if (results.isNotEmpty()) {
                Text(
                    "Résultats correspondants",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product) },
                            onAddToCart = { onAddToCart(product) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
