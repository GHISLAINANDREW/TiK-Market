package com.dschangmarket.ui.shop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.*
import com.dschangmarket.data.models.Product
import com.dschangmarket.theme.*
import com.dschangmarket.ui.chat.openUrl
import com.dschangmarket.ui.components.loadImageFromUrl
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShopPageScreen(
    shopId: Int,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onChat: (vendorId: Int, vendorName: String, shopName: String) -> Unit
) {
    var shop by remember { mutableStateOf<ApiShop?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var shopLogoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var shopReviews by remember { mutableStateOf<List<ApiReview>>(emptyList()) }
    var featuredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }

    LaunchedEffect(shopId) {
        isLoading = true
        try {
            val s = ApiClient.fetchShopById(shopId)
            shop = s
            if (s != null && s.logo.isNotBlank()) {
                val cleanBase = ApiClient.baseUrl.trimEnd('/')
                val cleanPath = s.logo.trimStart('/', '\\').replace("\\", "/")
                val finalUrl = if (s.logo.startsWith("http")) s.logo else "$cleanBase/$cleanPath"
                shopLogoBitmap = loadImageFromUrl(finalUrl)
            }
            if (s != null) {
                // Load reviews for this shop's products
                try {
                    val allReviews = s.products?.flatMap { p ->
                        val reviews = ApiClient.fetchProductReviews(p.id)
                        reviews?.reviews ?: emptyList()
                    }?.sortedByDescending { it.createdAt }?.take(5) ?: emptyList()
                    shopReviews = allReviews
                } catch (_: Exception) { }
                // Featured products: top sellers or highly rated
                val prods = (s.products ?: emptyList())
                    .filter { it.totalSales > 0 || it.rating >= 4.0 }
                    .sortedByDescending { it.totalSales }
                    .take(4)
                featuredProducts = prods.map { apiProduct ->
                    Product(
                        id = apiProduct.id.toString(),
                        shopId = s.id.toString(),
                        shopName = s.name,
                        shopLocation = s.location,
                        vendorId = s.vendorId.toString(),
                        vendorPhone = s.phone,
                        title = apiProduct.title,
                        description = apiProduct.description,
                        price = apiProduct.price.toDouble(),
                        comparePrice = apiProduct.comparePrice?.toDouble(),
                        category = apiProduct.category,
                        images = if (apiProduct.imageUrl.isNotBlank()) {
                            val cleanBase = ApiClient.baseUrl.trimEnd('/')
                            val cleanPath = apiProduct.imageUrl.trimStart('/', '\\').replace("\\", "/")
                            listOf(if (apiProduct.imageUrl.startsWith("http")) apiProduct.imageUrl else "$cleanBase/$cleanPath")
                        } else emptyList(),
                        stock = apiProduct.stock,
                        unit = apiProduct.unit,
                        rating = apiProduct.rating,
                        totalReviews = apiProduct.totalReviews,
                        totalSales = apiProduct.totalSales,
                        shopVerified = s.isVerified
                    )
                }
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    val s = shop
    val displayProducts = remember(s) {
        s?.products?.map { apiProduct ->
            Product(
                id = apiProduct.id.toString(),
                shopId = s.id.toString(),
                shopName = s.name,
                shopLocation = s.location,
                vendorId = s.vendorId.toString(),
                vendorPhone = s.phone,
                title = apiProduct.title,
                description = apiProduct.description,
                price = apiProduct.price.toDouble(),
                comparePrice = apiProduct.comparePrice?.toDouble(),
                category = apiProduct.category,
                images = if (apiProduct.imageUrl.isNotBlank()) {
                    val cleanBase = ApiClient.baseUrl.trimEnd('/')
                    val cleanPath = apiProduct.imageUrl.trimStart('/', '\\').replace("\\", "/")
                    listOf(if (apiProduct.imageUrl.startsWith("http")) apiProduct.imageUrl else "$cleanBase/$cleanPath")
                } else emptyList(),
                stock = apiProduct.stock,
                unit = apiProduct.unit,
                rating = apiProduct.rating,
                totalReviews = apiProduct.totalReviews,
                totalSales = apiProduct.totalSales,
                shopVerified = s.isVerified
            )
        } ?: emptyList()
    }

    var useGrid by remember { mutableStateOf(true) }
    var isFavorited by remember { mutableStateOf(false) }
    var favLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Load favorite status
    LaunchedEffect(shopId) {
        favLoading = true
        try {
            val favs = ApiClient.fetchFavoriteShops()
            isFavorited = favs.any { it.shopId == shopId }
        } catch (_: Exception) { }
        favLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s?.name ?: "Boutique", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor),
                actions = {
                    if (displayProducts.isNotEmpty()) {
                        IconButton(onClick = { useGrid = !useGrid }) {
                            Icon(if (useGrid) Icons.Default.List else Icons.Default.Apps, null, tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Orange)
            }
        } else if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Boutique non trouvée", color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Shop Header ──
                item {
                    Card(
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Orange)
                    ) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(80.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                if (shopLogoBitmap != null) {
                                    Image(
                                        bitmap = shopLogoBitmap!!,
                                        contentDescription = s.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        s.name.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = Orange
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Name + verified badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(s.name, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                                if (s.isVerified) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, null, Modifier.size(20.dp), tint = Color(0xFFFFD700))
                                }
                            }
                            if (s.description.isNotBlank()) {
                                Text(s.description, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            // Contact row
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // WhatsApp
                                if (s.phone.isNotBlank()) {
                                    Surface(
                                        onClick = { openUrl("https://wa.me/${s.phone.replace(" ", "").replace("+", "")}") },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Chat, null, Modifier.size(16.dp), tint = Color.White)
                                            Spacer(Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                                // Call
                                if (s.phone.isNotBlank()) {
                                    Surface(
                                        onClick = { openUrl("tel:${s.phone}") },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, null, Modifier.size(16.dp), tint = Color.White)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Appeler", fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Stats bar ──
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${s.productCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Orange)
                                Text("Produits", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${s.totalSales}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Orange)
                                Text("Vendus", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (s.rating > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val ratingStr = "${(s.rating * 10).toInt() / 10.0}"
                                        Text(ratingStr, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Orange)
                                        Icon(Icons.Default.Star, null, Modifier.size(14.dp), tint = Color(0xFFFFB300))
                                    }
                                    Text("Note", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // ── Follow / Unfollow button ──
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    if (isFavorited) {
                                        ApiClient.removeFavoriteShop(shopId)
                                        isFavorited = false
                                    } else {
                                        ApiClient.addFavoriteShop(shopId)
                                        isFavorited = true
                                    }
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFavorited) Color(0xFFFF6B6B) else Orange
                        ),
                        enabled = !favLoading
                    ) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            null, Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isFavorited) "Ne plus suivre" else "Suivre cette boutique",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Location ──
                if (s.location.isNotBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(s.location, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { openUrl("https://www.google.com/maps/search/${s.location.replace(" ", "+")}") }) {
                                    Text("Carte", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // ── Produits en vedette ──
                if (featuredProducts.isNotEmpty()) {
                    item {
                        Text(
                            "⭐ Produits en vedette",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            featuredProducts.forEach { product ->
                                Card(
                                    onClick = { onProductClick(product) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.width(140.dp)
                                ) {
                                    Column {
                                        Box(Modifier.fillMaxWidth().height(100.dp).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                            if (product.images.isNotEmpty()) {
                                                var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
                                                LaunchedEffect(product.images.firstOrNull()) {
                                                    product.images.firstOrNull()?.let { bmp = loadImageFromUrl(it) }
                                                }
                                                if (bmp != null) {
                                                    Image(bmp!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                } else {
                                                    Text(product.title.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Orange)
                                                }
                                            } else {
                                                Text(product.title.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Orange)
                                            }
                                            if (product.rating >= 4.5f) {
                                                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), shape = RoundedCornerShape(8.dp), color = Orange) {
                                                    Text("TOP", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Column(Modifier.padding(8.dp)) {
                                            Text(product.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${product.price.toInt()} FCFA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Green)
                                                if (product.comparePrice != null && product.comparePrice > 0) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("${product.comparePrice.toInt()} FCFA", fontSize = 10.sp, color = Color.Gray, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                                }
                                            }
                                            if (product.rating > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("${product.rating}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                                                    Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB300))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Avis clients ──
                if (shopReviews.isNotEmpty()) {
                    item {
                        Text(
                            "💬 Avis clients (${shopReviews.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    shopReviews.forEach { review ->
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(32.dp).background(Orange.copy(alpha = 0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                            Text(review.userName.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Orange)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(review.userName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Row {
                                                repeat(review.rating) { Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB300)) }
                                            }
                                        }
                                        Text(review.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
                                    }
                                    if (review.comment.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(review.comment, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                    if (review.vendorReply.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Surface(shape = RoundedCornerShape(8.dp), color = Green.copy(alpha = 0.08f)) {
                                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.Reply, null, Modifier.size(14.dp), tint = Green)
                                                Spacer(Modifier.width(6.dp))
                                                Text(review.vendorReply, fontSize = 12.sp, color = GreenDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Contacter le vendeur ──
                item {
                    Button(
                        onClick = { onChat(s.vendorId, s.name, s.name) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Icon(Icons.Default.Chat, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contacter le vendeur", fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Products section ──
                if (displayProducts.isNotEmpty()) {
                    item {
                        Text(
                            "Produits (${displayProducts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    if (useGrid) {
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                modifier = Modifier.fillMaxWidth().height((displayProducts.size * 280 / 2).dp.coerceAtMost(800.dp))
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                content = {
                                    items(displayProducts) { product ->
                                        ShopProductCard(product, onClick = { onProductClick(product) })
                                    }
                                }
                            )
                        }
                    } else {
                        items(displayProducts) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                onClick = { onProductClick(product) }
                            ) {
                                Row(Modifier.padding(12.dp)) {
                                    Box(Modifier.size(60.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Text(product.title.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Orange)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(product.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${product.price.toInt()} FCFA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aucun produit pour le moment", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ShopProductCard(product: Product, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(120.dp).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                Text(product.title.take(2).uppercase(), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Orange.copy(alpha = 0.5f))
            }
            Column(Modifier.padding(8.dp)) {
                Text(product.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("${product.price.toInt()} FCFA", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Green)
            }
        }
    }
}
