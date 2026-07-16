package com.dschangmarket.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiProduct
import com.dschangmarket.api.toProduct
import com.dschangmarket.data.models.Product
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.*
import com.dschangmarket.utils.safeApiCall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.onFocusChanged


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onCartClick: () -> Unit,
    onVendorClick: () -> Unit,
    onShopsClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    cartCount: Int,
    notificationCount: Int = 0,
    selectedShopName: String? = null,
    onClearShopFilter: () -> Unit = {},
    onError: (String) -> Unit = {},
    comparisonCount: Int = 0,
    onCompareClick: () -> Unit = {},
    searchHistory: List<String> = emptyList(),
    onSearchQuerySubmit: (String) -> Unit = {},
    isLoggedIn: Boolean = false,
    userRole: String = "buyer",
    onStoryClick: (List<Product>, Int) -> Unit = { _, _ -> },
    onAddStory: (String, String) -> Unit = { _, _ -> },
    refreshSignal: Int = 0
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("newest") }
    var showFilters by remember { mutableStateOf(false) }
    var isNearMeSelected by remember { mutableStateOf(false) }
    var userLocationName by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var apiProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var wishlistProductIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val scope = rememberCoroutineScope()

    val pickMedia = rememberImagePickerLauncher { result ->
        if (result != null) {
            onAddStory(result.dataUrl, result.fileName)
        }
    }

    // Load from API
    suspend fun loadProducts() {
        val cat = if (selectedCategory == "Tout") null else selectedCategory
        val minP = minPrice.toDoubleOrNull()
        val maxP = maxPrice.toDoubleOrNull()
        val result = safeApiCall {
            ApiClient.fetchProducts(
                search = searchQuery.ifBlank { null },
                category = cat,
                minPrice = minP,
                maxPrice = maxP,
                sortBy = sortBy
            )
        }
        if (result.isSuccess) {
            apiProducts = result.getOrDefault(emptyList()).map { it.toProduct() }
        } else {
            val err = (result as? com.dschangmarket.utils.ApiResult.Error)?.message ?: "Erreur inconnue"
            println("[HomeScreen] API Error: $err")
            onError(err)
        }
    }

    // Load wishlist if logged in
    suspend fun loadWishlist() {
        if (!ApiClient.isLoggedIn()) return
        val result = safeApiCall { ApiClient.fetchWishlist() }
        if (result.isSuccess) {
            wishlistProductIds = result.getOrDefault(emptyList()).map { it.productId }.toSet()
        }
    }

    fun toggleFavorite(productId: Int) {
        scope.launch {
            if (productId in wishlistProductIds) {
                safeApiCall { ApiClient.removeFromWishlist(productId) }
                wishlistProductIds = wishlistProductIds - productId
            } else {
                safeApiCall { ApiClient.addToWishlist(productId) }
                wishlistProductIds = wishlistProductIds + productId
            }
        }
    }

    // Load from API on first composition or refresh signal
    LaunchedEffect(refreshSignal) {
        println("[HomeScreen] Fetching products (signal=$refreshSignal)...")
        loadProducts()
        loadWishlist()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        try { categories = ApiClient.fetchCategories() } catch (_: Exception) { }
    }

    LaunchedEffect(searchQuery, selectedCategory, minPrice, maxPrice, sortBy) {
        if (searchQuery.isNotEmpty()) delay(300)
        loadProducts()
    }

    val displayProducts = apiProducts

    val filteredProducts = displayProducts.filter { p ->
        !p.isStory &&
        (selectedShopName == null || p.shopName == selectedShopName) &&
        (selectedCategory == null || p.category == selectedCategory) &&
        (searchQuery.isBlank() || p.title.contains(searchQuery, ignoreCase = true) || p.shopName.contains(searchQuery, ignoreCase = true))
    }

    // Dynamic placeholders for search
    val searchPlaceholders = listOf("Rechercher un produit...", "Poulet frais de Dschang...", "Pagne Wax élégant...", "Smartphone Samsung...", "Boutique de Will...")
    var placeholderIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(3500)
            placeholderIndex = (placeholderIndex + 1) % searchPlaceholders.size
        }
    }

    // Use BoxWithConstraints to adapt layout to screen size
    BoxWithConstraints {
        val screenWidth = maxWidth
        val isCompact = screenWidth < 480.dp

        Scaffold(
            floatingActionButton = {
                if (comparisonCount > 0) {
                    ExtendedFloatingActionButton(
                        onClick = onCompareClick,
                        icon = { Icon(Icons.Default.CompareArrows, null) },
                        text = { Text("Comparer ($comparisonCount)") },
                        containerColor = Orange,
                        contentColor = Color.White
                    )
                }
            },
            topBar = {
                Box(Modifier.background(BrandGradient).shadow(2.dp).statusBarsPadding()) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DschangMarket",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isLoggedIn) {
                                    Button(
                                        onClick = { onVendorClick() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("S'inscrire", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else if (userRole == "vendor") {
                                    Button(
                                        onClick = { onVendorClick() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Amber.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Storefront, null, modifier = Modifier.size(14.dp), tint = Amber)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Boutique", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(Modifier.width(6.dp))
                                BadgedBox(badge = { if (notificationCount > 0) Badge { Text("$notificationCount", fontSize = 9.sp) } }) {
                                    IconButton(onClick = onNotificationsClick, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                BadgedBox(badge = { if (cartCount > 0) Badge { Text("$cartCount", fontSize = 9.sp) } }) {
                                    IconButton(onClick = onCartClick, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.ShoppingCart, "Panier", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Produits", "Boutiques").forEach { tab ->
                                val isSelected = tab == "Produits"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        if (tab == "Boutiques") onShopsClick()
                                    }
                                ) {
                                    Text(tab, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = Color.White)
                                    if (isSelected) {
                                        Box(Modifier.width(16.dp).height(2.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                    } else {
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            // Search icon
                            Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        loadProducts()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                LazyColumn(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                // Search Section
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.isNotEmpty()) onSearchQuerySubmit(it)
                            },
                            placeholder = {
                                AnimatedContent(targetState = placeholderIndex, transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) { idx ->
                                    Text(searchPlaceholders[idx % searchPlaceholders.size], style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { isSearchFocused = it.isFocused },
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Green,
                                unfocusedContainerColor = CardWhite,
                                focusedContainerColor = CardWhite
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }
                }

                // Arrivages du jour (Stories) — show if stories exist OR if user is a vendor
                val stories = apiProducts.filter { it.isStory }
                
                if (stories.isNotEmpty() || userRole == "vendor") {
                    item {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Arrivages du jour", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Spacer(Modifier.width(6.dp))
                                Text("🔥", fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // "Add story" button for vendors
                                if (userRole == "vendor") {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { pickMedia() }
                                    ) {
                                        Box(
                                            Modifier
                                                .width(68.dp)
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Green.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Add, null, tint = Green, modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.height(2.dp))
                                                Text("Story", fontSize = 9.sp, color = Green, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("Ajouter", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Green)
                                    }
                                }
                                
                                stories.forEachIndexed { index, product ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable { onStoryClick(stories, index) }
                                    ) {
                                        Box(
                                            Modifier
                                                .width(68.dp)
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.LightGray)
                                        ) {
                                            var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                                            LaunchedEffect(product.images.firstOrNull()) {
                                                product.images.firstOrNull()?.let { bitmap = loadImageFromUrl(it) }
                                            }
                                            if (bitmap != null) {
                                                Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Box(Modifier.fillMaxSize().background(Green), contentAlignment = Alignment.Center) {
                                                    Text(product.title.take(1), fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(product.title, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.width(68.dp), textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }

                // Hero Section Montage
                item {
                    AnimatedHeroSection(screenWidth)
                }

                // Welcome text
                item {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Coups de cœur",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.weight(1f))
                        Text("${filteredProducts.size} articles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Categories horizontal scroll
                if (categories.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val allCats = listOf("Tout") + categories
                                allCats.forEach { cat ->
                                    val isSelected = (cat == "Tout" && selectedCategory == null) || cat == selectedCategory
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = if (cat == "Tout") null else cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Green,
                                            selectedLabelColor = Color.White,
                                            containerColor = CardWhite,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) Green else DividerGray
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Price filters
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = CardWhite,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text("Prix", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            
                            Box(Modifier.weight(1f).height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Text("Min", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Spacer(Modifier.width(4.dp))
                                    BasicTextField(
                                        value = minPrice,
                                        onValueChange = { minPrice = it.filter { c -> c.isDigit() } },
                                        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                            
                            Text("—", color = DividerGray, style = MaterialTheme.typography.bodySmall)
                            
                            Box(Modifier.weight(1f).height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.CenterStart) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Text("Max", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Spacer(Modifier.width(4.dp))
                                    BasicTextField(
                                        value = maxPrice,
                                        onValueChange = { maxPrice = it.filter { c -> c.isDigit() } },
                                        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }

                            if (minPrice.isNotEmpty() || maxPrice.isNotEmpty()) {
                                IconButton(onClick = { minPrice = ""; maxPrice = "" }, modifier = Modifier.size(22.dp)) {
                                    Icon(Icons.Default.HighlightOff, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Sort controls
                item {
                    val sortOptions = listOf(
                        "newest" to "Nouveautés",
                        "popular" to "Populaires",
                        "price_asc" to "Prix ↑",
                        "price_desc" to "Prix ↓",
                        "rating" to "Mieux notés",
                        "name_asc" to "Nom A-Z"
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sortOptions.forEach { (key, label) ->
                            val isSelected = sortBy == key
                            Surface(
                                onClick = { sortBy = key },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Orange else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }

                // Shop filter banner
                if (selectedShopName != null) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, null, Modifier.size(16.dp), tint = Green)
                                Spacer(Modifier.width(6.dp))
                                Text("Boutique : $selectedShopName", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Green)
                            }
                            TextButton(onClick = onClearShopFilter) {
                                Text("Tous les produits", fontSize = 12.sp, color = Orange)
                            }
                        }
                    }
                }

                if (isLoading) {
                    // Shimmer skeleton pendant le chargement
                    item {
                        val columns = when {
                            screenWidth < 600.dp -> 2
                            screenWidth < 900.dp -> 3
                            screenWidth < 1200.dp -> 4
                            else -> 5
                        }
                        val gap = 8.dp
                        val sidePadding = 12.dp
                        val cardWidth = (screenWidth - sidePadding * 2 - gap * (columns - 1)) / columns
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            repeat(columns * 2) {
                                ShimmerProductCard(modifier = Modifier.width(cardWidth))
                            }
                        }
                    }
                } else if (filteredProducts.isEmpty()) {
                    item { EmptyState(Icons.Default.SearchOff, "Aucun produit trouvé", "Essayez d'autres mots-clés") }
                } else {
                    // Grille adaptative : s'ajuste selon la largeur de l'écran (Web vs Mobile)
                    val columns = when {
                        screenWidth < 600.dp -> 2
                        screenWidth < 900.dp -> 3
                        screenWidth < 1200.dp -> 4
                        else -> 5
                    }
                    val gap = 8.dp
                    val sidePadding = 12.dp
                    val cardWidth = (screenWidth - sidePadding * 2 - gap * (columns - 1)) / columns

                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                            horizontalArrangement = Arrangement.spacedBy(gap),
                            verticalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            filteredProducts.forEachIndexed { index, product ->
                                val delay = (index % 15) * 50 // Staggered animation
                                var isCardVisible by remember(product.id) { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(delay.toLong())
                                    isCardVisible = true
                                }
                                AnimatedVisibility(
                                    visible = isCardVisible,
                                    enter = fadeIn(tween(500)) + scaleIn(initialScale = 0.8f, animationSpec = tween(500))
                                ) {
                                    ProductCard(
                                        product = product,
                                        onClick = { onProductClick(product) },
                                        onAddToCart = { onAddToCart(product) },
                                        modifier = Modifier.width(cardWidth),
                                        isFavorite = product.id.toIntOrNull() in wishlistProductIds,
                                        onToggleFavorite = {
                                            val pid = product.id.toIntOrNull()
                                            if (pid != null) toggleFavorite(pid)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        }
    }
}

@Composable
fun AnimatedHeroSection(screenWidth: Dp) {
    val items = listOf(
        Triple(
            "Boutique de Will", 
            "Les produits de Will : qualité & excellence", 
            "https://images.unsplash.com/photo-1587593810167-a84920ea0781"
        ),
        Triple(
            "Marché de Dschang", 
            "Le terroir authentique à votre porte", 
            "https://images.unsplash.com/photo-1594142510255-a4968875560b"
        ),
        Triple(
            "Mode & Tissus", 
            "L'élégance du pagne traditionnel", 
            "https://images.unsplash.com/photo-1523381210434-271e8be1f52b"
        ),
        Triple(
            "Technologie", 
            "Smartphone et gadgets au centre-ville", 
            "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf"
        )
    )
    
    var index by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(5000)
            index = (index + 1) % items.size
        }
    }

    val heroHeight = when {
        screenWidth < 600.dp -> 180.dp
        screenWidth < 900.dp -> 260.dp
        else -> 320.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .shadow(4.dp)
    ) {
        AnimatedContent(
            targetState = items[index],
            transitionSpec = {
                (fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 1.1f, animationSpec = tween(1200))) togetherWith
                (fadeOut(animationSpec = tween(1200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(1200)))
            }
        ) { item ->
            Box(Modifier.fillMaxSize()) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.third) {
                    bitmap = loadImageFromUrl(item.third)
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap as ImageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(BrandGradient))
                }
                
                // Overlay
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
                )
                
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                ) {
                    Text(
                        text = item.first,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f))
                    )
                    Text(
                        text = item.second,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Floating element for "Will"
                if (item.first.contains("Will")) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        color = Orange,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "TOP VENDEUR", 
                            color = Color.White, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
