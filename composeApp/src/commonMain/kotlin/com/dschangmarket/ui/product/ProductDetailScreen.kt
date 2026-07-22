package com.dschangmarket.ui.product

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.data.models.Product
import com.dschangmarket.data.models.Review
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.*
import com.dschangmarket.utils.shareText
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import com.dschangmarket.api.ApiProduct
import com.dschangmarket.api.toProduct

import com.dschangmarket.utils.FormatUtils
import com.dschangmarket.data.Resource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onChat: (Product) -> Unit,
    onShopClick: (Product) -> Unit = {},
    onSimilarProductClick: (Product) -> Unit = {},
    isCompared: Boolean = false,
    onToggleCompare: () -> Unit = {}
) {
    var selectedImageIndex by remember { mutableStateOf(0) }
    var reviewResource by remember { mutableStateOf<Resource<List<Review>>>(Resource.Loading) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isInWishlist by remember { mutableStateOf(false) }
    var shopLogoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()
    val productId = product.id.toIntOrNull() ?: 0

    // Load wishlist status
    LaunchedEffect(Unit) {
        if (ApiClient.isLoggedIn()) {
            try {
                if (productId > 0) {
                    val wishlist = ApiClient.fetchWishlist()
                    isInWishlist = wishlist.any { it.productId == productId }
                }
            } catch (_: Exception) { }
        }
    }

    // Load reviews
    LaunchedEffect(product.id) {
        reviewResource = Resource.Loading
        try {
            if (productId > 0) {
                val apiProduct = ApiClient.fetchProduct(productId)
                val reviews = (apiProduct.reviews ?: emptyList()).map { r ->
                    Review(
                        id = r.id.toString(),
                        userId = r.userId.toString(),
                        userName = r.userName,
                        rating = r.rating,
                        comment = r.comment,
                        date = r.createdAt
                    )
                }
                reviewResource = Resource.Success(reviews)
            } else {
                reviewResource = Resource.Success(emptyList())
            }
        } catch (e: Exception) {
            reviewResource = Resource.Error(e.message ?: "Erreur de chargement des avis")
        }
    }

    // Load similar products from API
    var similarResource by remember { mutableStateOf<Resource<List<Product>>>(Resource.Loading) }
    LaunchedEffect(product.category) {
        similarResource = Resource.Loading
        try {
            val apiProducts = ApiClient.fetchProducts(category = product.category)
            val similar = apiProducts
                .asSequence()
                .filter { it.id.toString() != product.id }
                .take(9)
                .map { it.toProduct() }
                .toList()
            similarResource = Resource.Success(similar)
        } catch (e: Exception) {
            similarResource = Resource.Error(e.message ?: "Erreur de chargement")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du produit", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = onToggleCompare) {
                        Icon(
                            if (isCompared) Icons.Default.CompareArrows else Icons.Default.Compare,
                            null,
                            tint = if (isCompared) Orange else Color.White
                        )
                    }
                    IconButton(onClick = {
                        val shareUrl = "https://dschang-marke.vercel.app/?p=${product.id}"
                        val shareText = "🚀 Découvre ${product.title} sur DschangMarket\nPrix : ${FormatUtils.formatPrice(product.price)}\n\nLien : $shareUrl"
                        shareText(shareText, "DschangMarket - ${product.title}")
                    }) { Icon(Icons.Default.Share, null) }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Signaler ce produit") },
                                onClick = { showMenu = false; showReportDialog = true },
                                leadingIcon = { Icon(Icons.Default.Report, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            val screenWidth = maxWidth
            val isWeb = screenWidth > 800.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color.White)
            ) {
                // ─── PARTIE HAUTE : IMAGES ET INFOS PRINCIPALES ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = if (isWeb) Arrangement.Center else Arrangement.spacedBy(16.dp)
                ) {
                    // Colonne des miniatures (à gauche)
                    Column(
                        modifier = Modifier.width(60.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val images = product.images.ifEmpty { listOf("") }
                        images.take(5).forEachIndexed { index, url ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .border(
                                        width = if (selectedImageIndex == index) 2.dp else 1.dp,
                                        color = if (selectedImageIndex == index) Orange else Color.LightGray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedImageIndex = index }
                                    .padding(2.dp)
                            ) {
                                var imgBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                                LaunchedEffect(url) { if (url.isNotBlank()) imgBitmap = loadImageFromUrl(url) }
                                
                                if (imgBitmap != null) {
                                    Image(bitmap = imgBitmap!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp)))
                                } else {
                                    Box(Modifier.fillMaxSize().background(GreenSurface), contentAlignment = Alignment.Center) {
                                        Text(SampleData.productEmojis[product.id] ?: "📦", fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (isWeb) Spacer(Modifier.width(32.dp))

                    // Image principale (au centre)
                    Box(
                        modifier = Modifier
                            .let { if (isWeb) it.size(400.dp) else it.weight(1f).aspectRatio(1f) }
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8F8F8)),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentUrl = product.images.getOrNull(selectedImageIndex) ?: ""
                        var mainImgBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                        LaunchedEffect(currentUrl) { if (currentUrl.isNotBlank()) mainImgBitmap = loadImageFromUrl(currentUrl) }

                        if (mainImgBitmap != null) {
                            Image(bitmap = mainImgBitmap!!, contentDescription = product.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(SampleData.productEmojis[product.id] ?: "📦", fontSize = if (isWeb) 150.sp else 100.sp)
                        }

                        // Boutons sur l'image
                        IconButton(
                            onClick = { /* Zoom */ },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.White.copy(alpha = 0.7f), CircleShape).size(36.dp)
                        ) { Icon(Icons.Default.ZoomIn, null, Modifier.size(20.dp)) }
                        
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (productId > 0) {
                                        try {
                                            if (isInWishlist) {
                                                ApiClient.removeFromWishlist(productId)
                                                isInWishlist = false
                                            } else {
                                                ApiClient.addToWishlist(productId)
                                                isInWishlist = true
                                            }
                                        } catch (_: Exception) { }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.White.copy(alpha = 0.7f), CircleShape).size(36.dp)
                        ) {
                            Icon(
                                if (isInWishlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isInWishlist) Color.Red else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Limiter la largeur du contenu sur le web pour une meilleure lecture
                val contentModifier = if (isWeb) Modifier.widthIn(max = 800.dp).align(Alignment.CenterHorizontally) else Modifier.fillMaxWidth()
                
                Column(modifier = contentModifier.padding(horizontal = 16.dp)) {
                // Titre
                Text(
                    text = product.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp,
                    color = Color.Black
                )

                Spacer(Modifier.height(8.dp))

                // Ratings and Sales
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingBar(product.rating, product.totalReviews)
                    Text("|", color = Color.LightGray)
                    Text("${product.totalSales} vendus", fontSize = 13.sp, color = Color.Gray)
                    if (product.stock in 1..5) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("Plus que ${product.stock} restants !", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(color = Orange.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text("N°1 des ventes", color = Orange, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Grille de Prix (Paliers)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PriceTierItem(FormatUtils.formatPrice(product.price), "1-99 pièces")
                    PriceTierItem(FormatUtils.formatPrice(product.price * 0.95), "100-499 pièces")
                    PriceTierItem(FormatUtils.formatPrice(product.price * 0.9), "≥ 500 pièces")
                }

                Spacer(Modifier.height(24.dp))

                // Options : Couleur
                Text("Couleur", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(if (i==0) GreenSurface else Color(0xFFF0F0F0)).border(1.dp, if (i==0) Green else Color.Transparent, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(SampleData.productEmojis[product.id] ?: "📦", fontSize = 20.sp)
                        }
                    }
                    Box(Modifier.height(40.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFF0F0F0)).clickable { }, contentAlignment = Alignment.Center) {
                        Text("Voir tout", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ─── SECTION VENDEUR (avec logo) ───
                LaunchedEffect(product.shopId) {
                    try {
                        val shopId = product.shopId.toIntOrNull() ?: 0
                        if (shopId > 0) {
                            val apiShop = ApiClient.fetchShopById(shopId)
                            if (apiShop?.logo?.isNotBlank() == true) {
                                shopLogoBitmap = loadImageFromUrl(apiShop.logo)
                            }
                        }
                    } catch (_: Exception) {}
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FBFF),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE6F4FF))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                if (shopLogoBitmap != null) {
                                    Image(bitmap = shopLogoBitmap!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                } else {
                                    Icon(Icons.Default.Store, null, tint = Green, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(product.shopName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF003A8C))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (product.shopVerified) {
                                        Icon(Icons.Default.Verified, null, Modifier.size(14.dp), tint = Color(0xFF1890FF))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(if (product.shopVerified) "Vendeur vérifié" else "Vendeur", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            VendorStatItem(if (product.rating > 0) "${product.rating}/5" else "N/A", "Note")
                            VendorStatItem("${product.totalReviews}", "Avis")
                            VendorStatItem("${product.totalSales}", "Ventes")
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Contact vendeur (WhatsApp / Appel) ──
                if (product.vendorPhone.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { com.dschangmarket.ui.chat.openUrl("https://wa.me/${product.vendorPhone.replace(" ", "").replace("+", "")}?text=Bonjour%2C%20je%20suis%20int%C3%A9ress%C3%A9%20par%20${product.title.replace(" ", "%20")}") },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF25D366)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                        ) {
                            Text("WhatsApp", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { com.dschangmarket.ui.chat.openUrl("tel:${product.vendorPhone}") },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF1976D2)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2))
                        ) {
                            Text("Appeler", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── Section Achat Groupé ──────────────────────────
                val groupBuyProductId = product.id.toIntOrNull() ?: 0
                val groupBuyShopId = product.shopId.toIntOrNull() ?: 0
                GroupBuySection(
                    productId = groupBuyProductId,
                    shopId = groupBuyShopId
                )

                // ── Section Avis ──────────────────────────────────
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFE0E0E0))
                Spacer(Modifier.height(16.dp))

                // En-tête des avis
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Avis & Notes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (product.totalReviews > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(product.rating.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFA000), modifier = Modifier.size(20.dp))
                            Text(" (${product.totalReviews})", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Afficher les avis existants
                ResourceBox(resource = reviewResource) { reviews ->
                    if (reviews.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucun avis pour le moment", fontSize = 14.sp, color = Color.Gray)
                        }
                    } else {
                        reviews.forEach { review ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF5F5F5)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(review.userName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Row {
                                            repeat(5) { i ->
                                                Icon(
                                                    if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                    null,
                                                    tint = Color(0xFFFFA000),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (review.comment.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(review.comment, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(review.date, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Formulaire d'avis (si connecté et pas le vendeur)
                val vendorId = product.vendorId.toIntOrNull() ?: 0
                val isLoggedIn = ApiClient.isLoggedIn()
                val currentUserId = ApiClient.getCurrentUser()?.id ?: 0
                if (isLoggedIn && vendorId != currentUserId) {
                    var myRating by remember { mutableStateOf(0) }
                    var myComment by remember { mutableStateOf("") }
                    var submittingReview by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F0FF)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Donner mon avis", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(5) { i ->
                                    IconButton(onClick = { myRating = i + 1 }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            if (i < myRating) Icons.Default.Star else Icons.Default.StarBorder,
                                            null,
                                            tint = Color(0xFFFFA000),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = myComment,
                                onValueChange = { myComment = it },
                                placeholder = { Text("Votre commentaire (optionnel)...") },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (myRating > 0) {
                                        submittingReview = true
                                        scope.launch {
                                            try {
                                                ApiClient.submitReview(productId, myRating, myComment)
                                                if (productId > 0) {
                                                    val apiProduct = ApiClient.fetchProduct(productId)
                                                    val reviews = (apiProduct.reviews ?: emptyList()).map { r ->
                                                        Review(
                                                            id = r.id.toString(),
                                                            userId = r.userId.toString(),
                                                            userName = r.userName,
                                                            rating = r.rating,
                                                            comment = r.comment,
                                                            date = r.createdAt
                                                        )
                                                    }
                                                    reviewResource = Resource.Success(reviews)
                                                }
                                                myRating = 0
                                                myComment = ""
                                            } catch (_: Exception) { }
                                            submittingReview = false
                                        }
                                    }
                                },
                                enabled = myRating > 0 && !submittingReview,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (submittingReview) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                else Text("Publier mon avis")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ─── PRODUITS SIMILAIRES ───
                Text("Produits similaires", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                
                ResourceBox(resource = similarResource) { similar ->
                    if (similar.isNotEmpty()) {
                        val columns = when {
                            screenWidth < 600.dp -> 2
                            screenWidth < 900.dp -> 3
                            else -> 4
                        }
                        val gap = 8.dp
                        val sidePadding = 8.dp
                        val cardWidth = (screenWidth - sidePadding * 2 - gap * (columns - 1)) / columns

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            similar.forEach { p ->
                                ProductCard(
                                    product = p,
                                    onClick = { onSimilarProductClick(p) },
                                    onAddToCart = { onAddToCart(p) },
                                    modifier = Modifier.width(cardWidth)
                                )
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Aucun produit similaire", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
                }
            }

            // ─── ACTIONS FLOTTANTES (En bas) ───
            Box(Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shop Icon Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onShopClick(product) }.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Text("Boutique", fontSize = 10.sp, color = Color.Gray)
                        }

                        // Chat Button
                        OutlinedButton(
                            onClick = { onChat(product) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            border = BorderStroke(1.dp, Orange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange)
                        ) {
                            Text("Discuter", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Order Button
                        Button(
                            onClick = { onAddToCart(product) },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Orange)
                        ) {
                            Text("Commander", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                        
                        // Bargain Button (New)
                        Surface(
                            onClick = { onChat(product) },
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = Green.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Green)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Handshake, "Négocier", tint = Green, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Report Dialog ──
    if (showReportDialog) {
        val scope = rememberCoroutineScope()
        var reason by remember { mutableStateOf("") }
        var reportComment by remember { mutableStateOf("") }
        var submitted by remember { mutableStateOf(false) }
        val reasons = listOf("Produit frauduleux", "Prix abusif", "Contenu inapproprié", "Arnaque", "Contrefaçon", "Autre")

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Signaler ce produit") },
            text = {
                if (submitted) {
                    Text("✅ Signalement envoyé. Merci de contribuer à la qualité de DschangMarket.", fontSize = 14.sp)
                } else {
                    Column(modifier = Modifier.width(280.dp)) {
                        reasons.forEach { r ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reason = r }
                                .padding(vertical = 4.dp)) {
                                RadioButton(selected = reason == r, onClick = { reason = r })
                                Spacer(Modifier.width(4.dp))
                                Text(r, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reportComment,
                            onValueChange = { reportComment = it },
                            placeholder = { Text("Commentaire (optionnel)", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(72.dp),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )
                    }
                }
            },
            confirmButton = {
                if (submitted) {
                    TextButton(onClick = { showReportDialog = false }) { Text("Fermer") }
                } else {
                    Button(
                        onClick = {
                            if (reason.isNotBlank()) {
                                scope.launch {
                                    try {
                                        if (productId > 0) {
                                            ApiClient.submitReport("product", productId, reason, reportComment)
                                        }
                                    } catch (_: Exception) {}
                                    submitted = true
                                }
                            }
                        },
                        enabled = reason.isNotBlank()
                    ) { Text("Envoyer le signalement") }
                }
            },
            dismissButton = {
                if (!submitted) {
                    TextButton(onClick = { showReportDialog = false }) { Text("Annuler") }
                }
            }
        )
    }
}

@Composable
fun PriceTierItem(price: String, quantity: String) {
    Column {
        Text(price, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Text(quantity, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun VendorStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

