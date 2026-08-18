package com.tik_market.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiProduct
import com.tik_market.api.ApiStory
import com.tik_market.api.toProduct
import com.tik_market.data.models.Product
import com.tik_market.data.models.SampleData
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.ui.story.StoryItem
import com.tik_market.utils.safeApiCall
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
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
    onStoryClick: (List<StoryItem>, Int) -> Unit = { _, _ -> },
    onAddStory: (String, String, String?) -> Unit = { _, _, _ -> },
    refreshSignal: Int = 0,
    cachedProducts: List<Product> = emptyList(),
    cachedCategories: List<String> = emptyList(),
    wishlistProductIds: Set<Int> = emptySet(),
    onCacheData: (products: List<Product>, categories: List<String>, wishlist: Set<Int>) -> Unit = { _, _, _ -> },
    overrideCity: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("newest") }
    var userLocationName by remember { mutableStateOf<String?>(null) }
    var marketName by remember { mutableStateOf("TiK-Market") }
    
    LaunchedEffect(overrideCity, isLoggedIn) {
        if (!isLoggedIn) {
            userLocationName = null
            marketName = "TiK-Market"
            return@LaunchedEffect
        }
        if (!overrideCity.isNullOrBlank()) {
            userLocationName = overrideCity
            marketName = com.tik_market.utils.marketNameForCity(overrideCity)
            return@LaunchedEffect
        }
        com.tik_market.utils.getCurrentLocationName { city ->
            userLocationName = city
            marketName = com.tik_market.utils.marketNameForCity(city)
        }
    }
    
    var localCategories by remember { mutableStateOf(cachedCategories) }
    var localProducts by remember { mutableStateOf(cachedProducts) }
    var localStories by remember { mutableStateOf<List<StoryItem>>(emptyList()) }
    var localWishlist by remember { mutableStateOf(wishlistProductIds) }
    var viewedStoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localHeroItems by remember { mutableStateOf<List<com.tik_market.api.ApiHeroItem>>(emptyList()) }

    val primary = MaterialTheme.colorScheme.primary
    val cityColors = LocalCityColors.current
    val s = LocalAppStrings.current
    
    LaunchedEffect(cachedProducts, cachedCategories, wishlistProductIds) {
        if (cachedProducts.isNotEmpty()) localProducts = cachedProducts
        if (cachedCategories.isNotEmpty()) localCategories = cachedCategories
        if (wishlistProductIds.isNotEmpty()) localWishlist = wishlistProductIds
    }
    
    var isLoading by remember { mutableStateOf(localProducts.isEmpty()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showStoryTypeDialog by remember { mutableStateOf(false) }
    var pendingStoryDataUrl by remember { mutableStateOf<String?>(null) }
    var pendingStoryFileName by remember { mutableStateOf<String?>(null) }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var storyCaption by remember { mutableStateOf("") }
    var showTextStoryDialog by remember { mutableStateOf(false) }
    var textStoryContent by remember { mutableStateOf("") }
    var textStoryColor by remember { mutableStateOf(Green) }

    val pickPhoto = rememberMediaPickerLauncher(allowVideo = false) { result ->
        if (result != null) {
            pendingStoryDataUrl = result.dataUrl
            pendingStoryFileName = result.fileName
            showCaptionDialog = true
        }
    }
    val pickVideo = rememberMediaPickerLauncher(allowVideo = true, videoOnly = true) { result ->
        if (result != null) {
            pendingStoryDataUrl = result.dataUrl
            pendingStoryFileName = result.fileName
            showCaptionDialog = true
        }
    }

    suspend fun loadProducts(force: Boolean = false) {
        if (!force && localProducts.isNotEmpty() && searchQuery.isBlank() && selectedCategory == null) return
        val cat = if (selectedCategory == s.allCategories) null else selectedCategory
        val minP = minPrice.toDoubleOrNull()
        val maxP = maxPrice.toDoubleOrNull()
        val cityFilter = if (isLoggedIn) userLocationName else null
        val result = safeApiCall {
            ApiClient.fetchProducts(search = searchQuery.ifBlank { null }, category = cat, minPrice = minP, maxPrice = maxP, sortBy = sortBy, location = cityFilter)
        }
        if (result.isSuccess) {
            localProducts = result.getOrDefault(emptyList()).map { it.toProduct() }.filter { !it.isStory }
            onCacheData(localProducts, localCategories, localWishlist)
        } else {
            val err = (result as? com.tik_market.utils.ApiResult.Error)?.message ?: s.unknownError
            onError(err)
        }
    }

    suspend fun loadStories() {
        try {
            val apiStories = ApiClient.fetchStories(replies = true)
            localStories = apiStories.map { apiStory ->
                val cleanBase = ApiClient.baseUrl.trimEnd('/')
                val cleanPath = apiStory.mediaUrl.trimStart('/', '\\').replace("\\", "/")
                val finalMediaUrl = if (apiStory.mediaUrl.startsWith("http") || apiStory.mediaType == "text") apiStory.mediaUrl else "$cleanBase/$cleanPath"

                StoryItem(
                    title = apiStory.shopName.ifBlank { apiStory.userName },
                    imageUrl = finalMediaUrl,
                    storyId = apiStory.id,
                    shopId = apiStory.shopId,
                    mediaType = apiStory.mediaType,
                    caption = apiStory.caption,
                    userId = apiStory.userId,
                    userAvatar = apiStory.userAvatar,
                    shopLogo = apiStory.shopLogo
                )
            }
        } catch (_: Exception) {}
    }

    fun toggleFavorite(productId: Int) {
        val isFav = productId in localWishlist
        val newWishlist = if (isFav) localWishlist - productId else localWishlist + productId
        localWishlist = newWishlist
        onCacheData(localProducts, localCategories, localWishlist)
        scope.launch { safeApiCall { if (isFav) ApiClient.removeFromWishlist(productId) else ApiClient.addToWishlist(productId) } }
    }

    suspend fun loadWishlist() {
        if (!isLoggedIn) return
        val result = safeApiCall { ApiClient.fetchWishlist() }
        if (result.isSuccess) {
            localWishlist = result.getOrDefault(emptyList()).map { it.id }.toSet()
            onCacheData(localProducts, localCategories, localWishlist)
        }
    }

    LaunchedEffect(refreshSignal) {
        isLoading = localProducts.isEmpty()
        loadProducts(force = true)
        loadStories()
        loadWishlist()
        try { localHeroItems = ApiClient.fetchHeroItems() } catch (_: Exception) {}
        isLoading = false
    }

    LaunchedEffect(Unit) {
        if (localCategories.isEmpty()) {
            try { localCategories = ApiClient.fetchCategories(); onCacheData(localProducts, localCategories, localWishlist) } catch (_: Exception) { }
        }
    }

    LaunchedEffect(searchQuery, selectedCategory, minPrice, maxPrice, sortBy) {
        if (searchQuery.isNotEmpty()) delay(300)
        loadProducts(force = true)
    }

    val filteredProducts = localProducts.filter { p ->
        (selectedShopName == null || p.shopName == selectedShopName) &&
        (selectedCategory == null || p.category == selectedCategory) &&
        (searchQuery.isBlank() || p.title.contains(searchQuery, ignoreCase = true) || p.shopName.contains(searchQuery, ignoreCase = true))
    }

    val searchPlaceholders = listOf("Poulet frais...", "Rechercher un produit...", "Pagne Wax élégant...", "Smartphone Samsung...", "Boutique de Will...")
    var placeholderIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(3500); placeholderIndex = (placeholderIndex + 1) % searchPlaceholders.size } }

    BoxWithConstraints {
        val screenWidth = maxWidth
        Scaffold(
            floatingActionButton = {
                if (comparisonCount > 0) {
                    ExtendedFloatingActionButton(onClick = onCompareClick, icon = { Icon(Icons.Default.CompareArrows, null) }, text = { Text(s.compareCta.format(comparisonCount)) }, containerColor = Orange, contentColor = Color.White)
                }
            },
            topBar = {
                Box(Modifier.background(cityColors.gradient).shadow(2.dp).statusBarsPadding()) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(marketName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isLoggedIn) {
                                    Button(onClick = { onVendorClick() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp), modifier = Modifier.height(30.dp)) {
                                        Text(s.registerShort, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else if (userRole == "vendor") {
                                    Button(onClick = { onVendorClick() }, colors = ButtonDefaults.buttonColors(containerColor = Amber.copy(alpha = 0.25f)), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp), modifier = Modifier.height(30.dp)) {
                                        Icon(Icons.Default.Storefront, null, modifier = Modifier.size(14.dp), tint = Amber); Spacer(Modifier.width(4.dp))
                                        Text(s.shop, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                BadgedBox(badge = { if (notificationCount > 0) Badge { Text("$notificationCount", fontSize = 9.sp) } }) {
                                    IconButton(onClick = onNotificationsClick, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; loadProducts(force = true); loadStories(); isRefreshing = false } }, modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it; if (it.isNotEmpty()) onSearchQuerySubmit(it) },
                                placeholder = { AnimatedContent(targetState = placeholderIndex) { idx -> Text(searchPlaceholders[idx % searchPlaceholders.size], style = MaterialTheme.typography.bodySmall) } },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                                        IconButton(onClick = { com.tik_market.ui.chat.startSpeechToText { text -> if (text.isNotBlank()) { searchQuery = text; onSearchQuerySubmit(text) } } }) {
                                            Icon(Icons.Default.Mic, "Recherche vocale", tint = primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().onFocusChanged { isSearchFocused = it.isFocused },
                                shape = RoundedCornerShape(28.dp),
                                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = primary, unfocusedContainerColor = CardWhite, focusedContainerColor = CardWhite),
                                singleLine = true, textStyle = TextStyle(fontSize = 13.sp)
                            )
                        }
                    }

                    if (localStories.isNotEmpty() || userRole == "vendor") {
                        item {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.arrivalsToday, style = MaterialTheme.typography.titleSmall, color = TextPrimary); Spacer(Modifier.width(6.dp)); Text("🔥", fontSize = 14.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (userRole == "vendor" || userRole == "admin" || userRole == "super_admin") {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showStoryTypeDialog = true }) {
                                            Box(Modifier.width(68.dp).height(96.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE0E0E0).copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Add, null, tint = primary, modifier = Modifier.size(28.dp))
                                                    Spacer(Modifier.height(2.dp)); Text(s.story, fontSize = 10.sp, color = primary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp)); Text(s.add, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primary)
                                        }
                                    }
                                    localStories.forEachIndexed { index, item ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewedStoryIds = viewedStoryIds + item.storyId; onStoryClick(localStories, index) }) {
                                            val hasRing = item.storyId > 0 && item.storyId !in viewedStoryIds
                                            Box(Modifier.width(72.dp).height(96.dp).then(if (hasRing) Modifier.border(2.dp, cityColors.gradient, RoundedCornerShape(14.dp)).padding(2.dp) else Modifier.border(1.dp, DividerGray, RoundedCornerShape(14.dp)).padding(1.dp))) {
                                                Box(Modifier.width(68.dp).height(92.dp).clip(RoundedCornerShape(12.dp)).background(if (item.imageUrl.isEmpty()) primary else Color(0xFFF0F0F0))) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                                                        LaunchedEffect(item.imageUrl) { bitmap = loadImageFromUrl(item.imageUrl) }
                                                        if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                        else Box(Modifier.fillMaxSize().background(primary), contentAlignment = Alignment.Center) { Text(item.title.take(1), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp) }
                                                        if (item.mediaType == "video") Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                                                            Surface(shape = RoundedCornerShape(4.dp), color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.padding(4.dp)) {
                                                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp).padding(2.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(item.title, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(68.dp), textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { if (localHeroItems.isNotEmpty()) DynamicHeroSection(localHeroItems, screenWidth) else AnimatedHeroSection(screenWidth, userLocationName) }

                    item {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.favorites, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.width(8.dp))
                            Text(s.articlesCount.format(filteredProducts.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (localCategories.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val allCats = listOf(s.allCategories) + localCategories
                                    allCats.forEach { cat ->
                                        val isSelected = (cat == s.allCategories && selectedCategory == null) || cat == selectedCategory
                                        FilterChip(selected = isSelected, onClick = { selectedCategory = if (cat == s.allCategories) null else cat }, label = { Text(cat, style = MaterialTheme.typography.labelMedium) }, shape = RoundedCornerShape(20.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primary, selectedLabelColor = Color.White, containerColor = CardWhite, labelColor = TextSecondary), border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = if (isSelected) primary else DividerGray))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Surface(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = CardWhite, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FilterList, null, Modifier.size(18.dp), tint = primary)
                                OutlinedTextField(value = minPrice, onValueChange = { if (it.all { c -> c.isDigit() }) minPrice = it }, placeholder = { Text("Min CFA", fontSize = 10.sp) }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = TextStyle(fontSize = 11.sp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DividerGray))
                                OutlinedTextField(value = maxPrice, onValueChange = { if (it.all { c -> c.isDigit() }) maxPrice = it }, placeholder = { Text("Max CFA", fontSize = 10.sp) }, modifier = Modifier.weight(1f).height(42.dp), shape = RoundedCornerShape(8.dp), singleLine = true, textStyle = TextStyle(fontSize = 11.sp), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = DividerGray))
                            }
                        }
                    }

                    if (isLoading) {
                        item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = primary) } }
                    } else if (filteredProducts.isEmpty()) {
                        item { EmptyState(Icons.Default.Inventory, "Aucun produit trouvé") }
                    } else {
                        val columns = if (screenWidth < 600.dp) 2 else if (screenWidth < 900.dp) 3 else 4
                        item {
                            ProductGridSection(
                                products = filteredProducts,
                                columns = columns,
                                wishlistIds = localWishlist,
                                onProductClick = onProductClick,
                                onAddToCart = onAddToCart,
                                onToggleFavorite = { toggleFavorite(it) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showStoryTypeDialog) {
            AlertDialog(onDismissRequest = { showStoryTypeDialog = false }, title = { Text(s.addStory) }, text = { Text(s.addStoryHint) }, confirmButton = { Column { TextButton(onClick = { showStoryTypeDialog = false; pickPhoto() }) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(s.photo) } }; TextButton(onClick = { showStoryTypeDialog = false; pickVideo() }) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(s.video) } } } }, dismissButton = { TextButton(onClick = { showStoryTypeDialog = false; showTextStoryDialog = true }) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TextSnippet, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(s.textOnly) } } })
        }
        if (showCaptionDialog) {
            AlertDialog(onDismissRequest = { showCaptionDialog = false }, title = { Text(s.addCaption) }, text = { Column { Text(s.addCaptionHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary); Spacer(Modifier.height(12.dp)); OutlinedTextField(value = storyCaption, onValueChange = { storyCaption = it }, placeholder = { Text(s.yourMessage) }, modifier = Modifier.fillMaxWidth(), maxLines = 3) } }, confirmButton = { Button(onClick = { showCaptionDialog = false; val dataUrl = pendingStoryDataUrl; val fileName = pendingStoryFileName; if (dataUrl != null && fileName != null) { onAddStory(dataUrl, fileName, storyCaption.ifBlank { null }) }; storyCaption = "" }) { Text(s.publish) } }, dismissButton = { TextButton(onClick = { showCaptionDialog = false }) { Text(s.skip) } })
        }
        if (showTextStoryDialog) {
            AlertDialog(onDismissRequest = { showTextStoryDialog = false }, title = { Text(s.textStory) }, text = { Column { OutlinedTextField(value = textStoryContent, onValueChange = { textStoryContent = it }, placeholder = { Text(s.whatDoYouWantToSay) }, modifier = Modifier.fillMaxWidth().height(120.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = textStoryColor.copy(alpha = 0.1f))); Spacer(Modifier.height(12.dp)); Text(s.backgroundColor, style = MaterialTheme.typography.labelSmall); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) { listOf(Green, Orange, BlueAccent, RedAccent, Color.DarkGray).forEach { color -> Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).background(color).border(if (textStoryColor == color) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp)).clickable { textStoryColor = color }) } } } }, confirmButton = { Button(onClick = { showTextStoryDialog = false; val colorHex = when(textStoryColor) { Green -> "#4CAF50"; Orange -> "#FF9800"; BlueAccent -> "#2196F3"; RedAccent -> "#F44336"; else -> "#333333" }; onAddStory(colorHex, "text", textStoryContent); textStoryContent = "" }, enabled = textStoryContent.isNotBlank()) { Text(s.publish) } }, dismissButton = { TextButton(onClick = { showTextStoryDialog = false }) { Text(s.cancel) } })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductGridSection(products: List<Product>, columns: Int, wishlistIds: Set<Int>, onProductClick: (Product) -> Unit, onAddToCart: (Product) -> Unit, onToggleFavorite: (Int) -> Unit) {
    FlowRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        products.forEach { product ->
            val isFav = product.id.toIntOrNull() in wishlistIds
            ProductCard(product = product, onClick = { onProductClick(product) }, onAddToCart = { onAddToCart(product) }, modifier = Modifier.width(160.dp), isFavorite = isFav, onToggleFavorite = { onToggleFavorite(product.id.toIntOrNull() ?: 0) })
        }
    }
}

@Composable
fun DynamicHeroSection(items: List<com.tik_market.api.ApiHeroItem>, screenWidth: Dp) {
    var index by remember { mutableStateOf(0) }
    val cityColors = LocalCityColors.current
    LaunchedEffect(items) { while(items.isNotEmpty()) { delay(5000); index = (index + 1) % items.size } }
    val heroHeight = if (screenWidth < 480.dp) 200.dp else if (screenWidth < 900.dp) 280.dp else 350.dp
    if (items.isEmpty()) return
    Box(modifier = Modifier.fillMaxWidth().height(heroHeight).padding(16.dp).clip(RoundedCornerShape(24.dp)).shadow(4.dp)) {
        AnimatedContent(targetState = items[index], transitionSpec = { (fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 1.1f, animationSpec = tween(1200))) togetherWith (fadeOut(animationSpec = tween(1200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(1200))) }) { item ->
            Box(Modifier.fillMaxSize()) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.imageUrl) {
                    val finalUrl = if (item.imageUrl.startsWith("http")) item.imageUrl else "${ApiClient.baseUrl.trimEnd('/')}/${item.imageUrl.trimStart('/', '\\').replace("\\", "/")}"
                    bitmap = loadImageFromUrl(finalUrl)
                }
                if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(cityColors.gradient))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Orange.copy(alpha = 0.5f), GreenDark.copy(alpha = 0.7f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(text = item.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)))
                    Text(text = item.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                if (item.shopName != null) Surface(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), color = Orange, shape = RoundedCornerShape(12.dp)) {
                    Text("TOP BOUTIQUE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedHeroSection(screenWidth: Dp, cityName: String?) {
    val cityColors = LocalCityColors.current
    val items = when {
        cityName?.contains("Bafoussam", ignoreCase = true) == true -> listOf(Triple("Marché de Bafoussam", "Le cœur du commerce à l'Ouest", "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=800&q=80"), Triple("Produits du terroir", "Frais et naturels de Fousap", "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=800&q=80"))
        cityName?.contains("Douala", ignoreCase = true) == true -> listOf(Triple("Douala Shopping", "Les meilleures boutiques du Littoral", "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&q=80"), Triple("Sawa Market", "Fraîcheur marine et mode urbaine", "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80"))
        cityName?.contains("Yaoundé", ignoreCase = true) == true -> listOf(Triple("Yaoundé Direct", "La capitale à portée de main", "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=800&q=80"), Triple("Ongola Market", "Qualité et prestige réunis", "https://images.unsplash.com/photo-1441984904996-e0b6ba687e12?w=800&q=80"))
        else -> listOf(Triple("Will & Fils", "Volaille fraîche livrée à domicile", "https://images.unsplash.com/photo-1587593810167-a84920ea0781?w=800&q=80"), Triple("Mode & Élégance", "Pagne Wax de qualité", "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80"), Triple("Saveurs locales", "Fruits et légumes du terroir", "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=800&q=80"), Triple("Tech & Gadgets", "Smartphones et accessoires", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&q=80"))
    }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(5000); index = (index + 1) % items.size } }
    val heroHeight = if (screenWidth < 480.dp) 200.dp else if (screenWidth < 900.dp) 280.dp else 350.dp
    Box(modifier = Modifier.fillMaxWidth().height(heroHeight).padding(16.dp).clip(RoundedCornerShape(24.dp)).shadow(4.dp)) {
        AnimatedContent(targetState = items[index], transitionSpec = { (fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 1.1f, animationSpec = tween(1200))) togetherWith (fadeOut(animationSpec = tween(1200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(1200))) }) { item ->
            Box(Modifier.fillMaxSize()) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.third) { bitmap = loadImageFromUrl(item.third) }
                if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(cityColors.gradient))
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Orange.copy(alpha = 0.5f), GreenDark.copy(alpha = 0.7f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(text = item.first, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f)))
                    Text(text = item.second, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                if (item.first.contains("Will")) Surface(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), color = Orange, shape = RoundedCornerShape(12.dp)) {
                    Text("TOP VENDEUR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
