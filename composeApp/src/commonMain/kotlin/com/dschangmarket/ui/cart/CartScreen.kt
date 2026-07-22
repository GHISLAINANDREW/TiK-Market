package com.dschangmarket.ui.cart

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.data.models.CartItem
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.EmptyState
import com.dschangmarket.ui.components.PriceDisplay
import com.dschangmarket.ui.components.loadImageFromUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    items: List<CartItem>,
    onBack: () -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCheckout: () -> Unit
) {
    var pendingRemoveIndex by remember { mutableStateOf(-1) }

    // Confirm remove dialog
    if (pendingRemoveIndex >= 0 && pendingRemoveIndex < items.size) {
        AlertDialog(
            onDismissRequest = { pendingRemoveIndex = -1 },
            icon = { Icon(Icons.Default.Delete, null, tint = RedAccent) },
            title = { Text("Retirer du panier") },
            text = { Text("Supprimer « ${items[pendingRemoveIndex].product.title} » du panier ?") },
            confirmButton = {
                TextButton(onClick = { onRemove(pendingRemoveIndex); pendingRemoveIndex = -1 }) {
                    Text("Retirer", color = RedAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveIndex = -1 }) {
                    Text("Annuler")
                }
            }
        )
    }

    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Panier (${items.size})", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Total", fontSize = if (isCompact) 11.sp else 13.sp, color = Color.Gray)
                            Text("${items.sumOf { it.subtotal }.toInt()} FCFA", fontSize = if (isCompact) 18.sp else 22.sp, fontWeight = FontWeight.Bold, color = Green)
                        }
                        Button(onClick = onCheckout, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text("Commander", fontWeight = FontWeight.Bold, fontSize = if (isCompact) 14.sp else 16.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(Icons.Default.ShoppingCart, "Votre panier est vide", "Ajoutez des produits depuis l'accueil")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
                itemsIndexed(items) { index, item ->
                    CartItemCard(item = item, isCompact = isCompact,
                        onIncrease = { onUpdateQuantity(index, item.quantity + 1) },
                        onDecrease = { if (item.quantity > 1) onUpdateQuantity(index, item.quantity - 1) },
                        onRemove = { pendingRemoveIndex = index }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
    } // BoxWithConstraints
}

@Composable
private fun CartItemCard(item: CartItem, isCompact: Boolean = false, onIncrease: () -> Unit, onDecrease: () -> Unit, onRemove: () -> Unit) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val imgSize = if (isCompact) 56.dp else 72.dp
    val productImageUrl = item.product.images.firstOrNull()

    LaunchedEffect(productImageUrl) {
        if (!productImageUrl.isNullOrBlank()) {
            imageBitmap = loadImageFromUrl(productImageUrl)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Image: loaded bitmap or fallback emoji
            val loadedBitmap = imageBitmap
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap,
                    contentDescription = item.product.title,
                    modifier = Modifier.size(imgSize).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                val cartEmoji = SampleData.productEmojis[item.product.id] ?: "📦"
                Box(Modifier.size(imgSize).clip(RoundedCornerShape(8.dp)).background(GreenSurface), contentAlignment = Alignment.Center) {
                    Text(cartEmoji, fontSize = if (isCompact) 22.sp else 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.product.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                PriceDisplay(item.product.price, item.product.comparePrice, 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quantité
                    IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp), enabled = item.quantity > 1) {
                        Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                    }
                    Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("= ${item.subtotal.toInt()} FCFA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = RedAccent)
            }
        }
    }
}
