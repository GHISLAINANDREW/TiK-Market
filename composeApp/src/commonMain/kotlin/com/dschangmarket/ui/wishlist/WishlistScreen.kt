package com.dschangmarket.ui.wishlist

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiWishlistItem
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.EmptyState
import com.dschangmarket.ui.components.PriceDisplay
import com.dschangmarket.ui.components.loadImageFromUrl
import com.dschangmarket.utils.safeApiCall
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onBack: () -> Unit,
    onProductClick: (ApiWishlistItem) -> Unit,
    onError: (String) -> Unit
) {
    var items by remember { mutableStateOf<List<ApiWishlistItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun loadWishlist() {
        scope.launch {
            isLoading = true
            val result = safeApiCall { ApiClient.fetchWishlist() }
            if (result.isSuccess) {
                items = result.getOrDefault(emptyList())
            } else {
                val err = (result as? com.dschangmarket.utils.ApiResult.Error)?.message ?: "Erreur"
                onError(err)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadWishlist() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Favoris", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Orange)
                }
                items.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "Aucun favori",
                        subtitle = "Ajoutez des produits en c\u0153ur depuis l'accueil"
                    )
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        itemsIndexed(items) { index, item ->
                            WishlistItemCard(
                                item = item,
                                onRemove = {
                                    scope.launch {
                                        safeApiCall { ApiClient.removeFromWishlist(item.productId) }
                                        items = items.toMutableList().apply { removeAt(index) }
                                    }
                                },
                                onClick = { onProductClick(item) }
                            )
                            if (index < items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 100.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistItemCard(
    item: ApiWishlistItem,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.imageUrl) {
        imageBitmap = loadImageFromUrl(item.imageUrl)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product image
        Box(
            Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(imageBitmap!!, "Produit", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.Favorite, null, tint = Orange.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Info
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.shopName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            PriceDisplay(
                price = item.price,
                comparePrice = item.comparePrice
            )
        }

        // Remove button
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, "Retirer", tint = MaterialTheme.colorScheme.error)
        }
    }
}
