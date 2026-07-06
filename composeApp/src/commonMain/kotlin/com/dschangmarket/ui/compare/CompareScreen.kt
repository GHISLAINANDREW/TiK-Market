package com.dschangmarket.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.data.models.CartItem
import com.dschangmarket.data.models.Product
import com.dschangmarket.api.toProduct
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    products: List<Product>,
    onBack: () -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    if (products.isEmpty() && !isSearching) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = { Text("Comparateur") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } }
            )
            OutlinedTextField(
                value = searchQuery, onValueChange = {
                    searchQuery = it
                    if (it.length >= 2) { isSearching = true; scope.launch { searchResults = ApiClient.fetchProducts(search = it).map { p -> p.toProduct() } } }
                    else { searchResults = emptyList() }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Rechercher un produit à comparer...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; searchResults = emptyList(); isSearching = false }) {
                            Icon(Icons.Filled.Clear, "Effacer")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
            if (searchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(searchResults) { product ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onProductClick(product) }, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${product.price} FCFA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Ajoutez 2 à 4 produits à comparer", style = MaterialTheme.typography.titleMedium)
                        Text("Sélectionnez \"Comparer\" sur un produit", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Comparateur (${products.size}/4)") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } },
            actions = {
                IconButton(onClick = { searchQuery = ""; searchResults = emptyList(); isSearching = !isSearching }) {
                    Icon(if (isSearching) Icons.Filled.Close else Icons.Filled.Add, "Ajouter/Retirer")
                }
            }
        )

        if (isSearching) {
            OutlinedTextField(
                value = searchQuery, onValueChange = {
                    searchQuery = it
                    if (it.length >= 2) { scope.launch { searchResults = ApiClient.fetchProducts(search = it).map { p -> p.toProduct() } } } else { searchResults = emptyList() }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Ajouter un produit...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )
            if (searchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(searchResults) { product ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onProductClick(product) }, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${product.price} FCFA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                if (products.any { it.id == product.id }) {
                                    Text("Déjà ajouté", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        val scrollState = rememberScrollState()
        val rowLabels = listOf<Pair<String, (Product) -> String>>(
            "Prix" to { p -> "${p.price} FCFA" },
            "Catégorie" to { p -> p.category },
            "Stock" to { p -> if (p.stock > 0) "${p.stock} unités" else "Rupture" },
            "Vendeur" to { p -> p.shopName.ifEmpty { "—" } },
            "Localisation" to { p -> p.shopLocation.ifEmpty { "—" } },
            "Note" to { p -> if (p.rating > 0f) "${"⭐".repeat(p.rating.toInt())}" else "Aucun avis" },
            "Vendu" to { p -> "${p.totalSales} vendus" }
        )

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Column(modifier = Modifier.width(100.dp)) {}
                    products.forEach { product ->
                        Column(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(product.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                            val minPrice = products.minOfOrNull { it.price }
                            if (product.price == minPrice && products.size > 1) {
                                Text("Meilleur prix", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row {
                                IconButton(onClick = { onProductClick(product) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Visibility, "Voir", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onRemoveProduct(product) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Close, "Retirer", modifier = Modifier.size(18.dp), tint = Color.Red)
                                }
                                IconButton(onClick = { onAddToCart(product) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.AddShoppingCart, "Panier", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
            }

            items(rowLabels) { (label, valueFn) ->
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(vertical = 6.dp)) {
                    Box(modifier = Modifier.width(100.dp).padding(end = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    products.forEach { product ->
                        Box(modifier = Modifier.width(150.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
                            Text(valueFn(product), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (products.size >= 2) {
                item {
                    Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                    val bestValue = products.maxByOrNull { p ->
                        (p.rating * 10.0) + (p.totalSales.toDouble()) + (1000.0 / (p.price + 1.0))
                    }
                    if (bestValue != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Meilleur choix", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text(bestValue.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Rapport qualité/prix optimal", color = Color(0xFF558B2F), fontSize = 12.sp)
                                }
                                FilledTonalButton(onClick = { onProductClick(bestValue) }) { Text("Voir") }
                            }
                        }
                    }
                }
            }
        }
    }
}
