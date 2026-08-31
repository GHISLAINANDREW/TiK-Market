package com.tik_market.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.data.models.Product
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.ui.home.components.*
import com.tik_market.ui.story.StoryItem
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        HomeViewModel(
            scope = scope,
            initialProducts = cachedProducts,
            initialCategories = cachedCategories,
            initialWishlist = wishlistProductIds,
            onCacheData = onCacheData
        )
    }
    
    val state = viewModel.state
    val s = LocalAppStrings.current
    val cityColors = LocalCityColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    var isSearchFocused by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var viewedStoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // Navigation & Story creation states
    var showStoryTypeDialog by remember { mutableStateOf(false) }
    var pendingStoryDataUrl by remember { mutableStateOf<String?>(null) }
    var pendingStoryFileName by remember { mutableStateOf<String?>(null) }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var storyCaption by remember { mutableStateOf("") }
    var showTextStoryDialog by remember { mutableStateOf(false) }
    var textStoryContent by remember { mutableStateOf("") }
    var textStoryColor by remember { mutableStateOf(primary) }

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

    // Effects
    LaunchedEffect(overrideCity, isLoggedIn) {
        if (!isLoggedIn) {
            viewModel.setLocation(null)
            return@LaunchedEffect
        }
        if (!overrideCity.isNullOrBlank()) {
            viewModel.setLocation(overrideCity)
            return@LaunchedEffect
        }
        com.tik_market.utils.getCurrentLocationName { city ->
            viewModel.setLocation(city)
        }
    }

    LaunchedEffect(refreshSignal) {
        viewModel.loadAll(isLoggedIn, force = true)
    }

    LaunchedEffect(viewModel.searchQuery, viewModel.selectedCategory, viewModel.minPrice, viewModel.maxPrice, viewModel.sortBy) {
        if (viewModel.searchQuery.isNotEmpty()) delay(300)
        viewModel.loadProducts(isLoggedIn)
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            onError(it)
            viewModel.clearError()
        }
    }

    val filteredProducts = state.products.filter { p ->
        (selectedShopName == null || p.shopName == selectedShopName)
    }

    val searchPlaceholders = listOf("HG shop le meilleur...", "Rechercher un produit...", "Pagne Wax élégant...", "Smartphone Samsung...", "Boutique de Will...")
    var placeholderIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while(true) { delay(3500); placeholderIndex = (placeholderIndex + 1) % searchPlaceholders.size } }

    BoxWithConstraints {
        val screenWidth = maxWidth
        Scaffold(
            floatingActionButton = {
                if (comparisonCount > 0) {
                    ExtendedFloatingActionButton(
                        onClick = onCompareClick,
                        icon = { Icon(Icons.Default.CompareArrows, null) },
                        text = { Text(s.compareCta.format(comparisonCount)) },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = Color.White
                    )
                }
            },
            topBar = {
                HomeTopBar(
                    marketName = viewModel.marketName,
                    isLoggedIn = isLoggedIn,
                    userRole = userRole,
                    notificationCount = notificationCount,
                    cartCount = cartCount,
                    selectedShopName = selectedShopName,
                    cityColors = cityColors,
                    onVendorClick = onVendorClick,
                    onNotificationsClick = onNotificationsClick,
                    onCartClick = onCartClick,
                    onShopsClick = onShopsClick,
                    onClearShopFilter = onClearShopFilter,
                    onRefresh = { viewModel.refresh(isLoggedIn) },
                    isRefreshing = state.isRefreshing
                )
            }
        ) { padding ->
            val pullRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh(isLoggedIn) },
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize().padding(padding),
                indicator = {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        RotatingRefreshIcon(
                            modifier = Modifier.padding(8.dp).size(24.dp),
                            isRefreshing = state.isRefreshing,
                            tint = primary
                        )
                    }
                }
            ) {
                LazyColumn(Modifier.fillMaxSize().background(BackgroundViolet)) {
                    item {
                        SearchBarSection(
                            query = viewModel.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it); if (it.isNotEmpty()) onSearchQuerySubmit(it) },
                            placeholder = searchPlaceholders[placeholderIndex % searchPlaceholders.size],
                            primary = primary,
                            onFocusChanged = { isSearchFocused = it }
                        )
                    }

                    item {
                        HomeStories(
                            stories = state.stories,
                            userRole = userRole,
                            viewedStoryIds = viewedStoryIds,
                            isLoading = state.isLoading,
                            onStoryClick = { index -> viewedStoryIds = viewedStoryIds + state.stories[index].storyId; onStoryClick(state.stories, index) },
                            onAddStoryClick = { showStoryTypeDialog = true }
                        )
                    }

                    item {
                        HomeHero(
                            heroItems = state.heroItems,
                            screenWidth = screenWidth,
                            cityName = viewModel.userLocationName
                        )
                    }

                    item {
                        SectionTitle(
                            title = s.favorites,
                            count = filteredProducts.size,
                            articlesText = s.articlesCount
                        )
                    }

                    item {
                        HomeCategories(
                            categories = state.categories,
                            selectedCategory = viewModel.selectedCategory,
                            onCategoryClick = { viewModel.onCategoryChange(it) }
                        )
                    }

                    item {
                        FiltersSection(
                            showFilters = showFilters,
                            minPrice = viewModel.minPrice,
                            maxPrice = viewModel.maxPrice,
                            sortBy = viewModel.sortBy,
                            onToggleFilters = { showFilters = !showFilters },
                            onMinPriceChange = { viewModel.minPrice = it.filter { c -> c.isDigit() } },
                            onMaxPriceChange = { viewModel.maxPrice = it.filter { c -> c.isDigit() } },
                            onSortByChange = { viewModel.sortBy = it },
                            onReset = { viewModel.minPrice = ""; viewModel.maxPrice = ""; viewModel.sortBy = "newest" },
                            primary = primary
                        )
                    }

                    FlashSalesSection(
                        products = state.products,
                        wishlistIds = state.wishlistIds,
                        onProductClick = onProductClick,
                        onAddToCart = onAddToCart,
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )

                    if (state.isLoading && state.products.isEmpty()) {
                        item {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(6) { ProductShimmer() }
                            }
                        }
                    } else if (filteredProducts.isEmpty()) {
                        item { EmptyState(Icons.Default.Inventory, "Aucun produit trouvé") }
                    } else {
                        val columns = if (screenWidth < 600.dp) 2 else if (screenWidth < 900.dp) 3 else 4
                        item {
                            HomeProductGrid(
                                products = filteredProducts,
                                columns = columns,
                                wishlistIds = state.wishlistIds,
                                onProductClick = onProductClick,
                                onAddToCart = onAddToCart,
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Dialogs
        HomeDialogs(
            showStoryTypeDialog = showStoryTypeDialog,
            onDismissStoryType = { showStoryTypeDialog = false },
            onPickPhoto = { showStoryTypeDialog = false; pickPhoto() },
            onPickVideo = { showStoryTypeDialog = false; pickVideo() },
            onTextStory = { showStoryTypeDialog = false; showTextStoryDialog = true },
            showCaptionDialog = showCaptionDialog,
            onDismissCaption = { showCaptionDialog = false },
            storyCaption = storyCaption,
            onCaptionChange = { storyCaption = it },
            onPublishMediaStory = { 
                showCaptionDialog = false
                val dataUrl = pendingStoryDataUrl
                val fileName = pendingStoryFileName
                if (dataUrl != null && fileName != null) {
                    onAddStory(dataUrl, fileName, storyCaption.ifBlank { null })
                }
                storyCaption = ""
            },
            showTextStoryDialog = showTextStoryDialog,
            onDismissTextStory = { showTextStoryDialog = false },
            textStoryContent = textStoryContent,
            onTextStoryContentChange = { textStoryContent = it },
            textStoryColor = textStoryColor,
            onTextStoryColorChange = { textStoryColor = it },
            onPublishTextStory = {
                showTextStoryDialog = false
                val colorHex = when(textStoryColor) { 
                    primary -> "#4CAF50"; secondary -> "#FF9800"; BlueAccent -> "#2196F3"; RedAccent -> "#F44336"; else -> "#333333" 
                }
                onAddStory(colorHex, "text", textStoryContent)
                textStoryContent = ""
            }
        )
    }
}

@Composable
private fun HomeTopBar(
    marketName: String,
    isLoggedIn: Boolean,
    userRole: String,
    notificationCount: Int,
    cartCount: Int,
    selectedShopName: String?,
    cityColors: CityColors,
    onVendorClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onCartClick: () -> Unit,
    onShopsClick: () -> Unit,
    onClearShopFilter: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    val s = LocalAppStrings.current
    
    Box(Modifier.background(cityColors.gradient).shadow(2.dp).statusBarsPadding()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(marketName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isLoggedIn) {
                        Button(
                            onClick = onVendorClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(s.registerShort, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (userRole == "vendor") {
                        Button(
                            onClick = onVendorClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Storefront, null, modifier = Modifier.size(14.dp), tint = Amber)
                            Spacer(Modifier.width(4.dp))
                            Text(s.shop, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

                    Spacer(Modifier.width(6.dp))

                    IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                        RotatingRefreshIcon(
                            isRefreshing = isRefreshing,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(s.products, s.shop).forEach { tab ->
                    val isSelected = (tab == s.products && selectedShopName == null) || (tab == s.shop && selectedShopName != null)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (tab == s.shop) onShopsClick()
                            else onClearShopFilter()
                        }
                    ) {
                        Text(tab, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = Color.White)
                        if (isSelected) {
                            Box(Modifier.width(16.dp).height(2.dp).background(cityColors.secondary, RoundedCornerShape(1.dp)))
                        } else {
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    primary: Color,
    onFocusChanged: (Boolean) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = { com.tik_market.ui.chat.startSpeechToText { text -> if (text.isNotBlank()) onQueryChange(text) } }) {
                        Icon(Icons.Default.Mic, "Recherche vocale", tint = primary, modifier = Modifier.size(20.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = primary,
                unfocusedContainerColor = CardWhite,
                focusedContainerColor = CardWhite
            ),
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp)
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, articlesText: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        Text(articlesText.format(count), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun FiltersSection(
    showFilters: Boolean,
    minPrice: String,
    maxPrice: String,
    sortBy: String,
    onToggleFilters: () -> Unit,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onSortByChange: (String) -> Unit,
    onReset: () -> Unit,
    primary: Color
) {
    val s = LocalAppStrings.current
    
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = CardWhite,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.5.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.filters, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                if (showFilters) {
                    TextButton(onClick = onReset) {
                        Text(s.reset, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                IconButton(onClick = onToggleFilters) {
                    Icon(Icons.Outlined.FilterList, null, tint = if (showFilters) MaterialTheme.colorScheme.secondary else TextSecondary)
                }
            }
            AnimatedVisibility(visible = showFilters) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minPrice,
                            onValueChange = onMinPriceChange,
                            placeholder = { Text("Min CFA", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = onMaxPriceChange,
                            placeholder = { Text("Max CFA", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.sortBy, fontSize = 12.sp, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            listOf("newest" to s.newest, "price_asc" to s.priceAsc, "price_desc" to s.priceDesc).forEach { (key, label) ->
                                FilterChip(
                                    selected = sortBy == key,
                                    onClick = { onSortByChange(key) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = TextSecondary
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDialogs(
    showStoryTypeDialog: Boolean,
    onDismissStoryType: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onTextStory: () -> Unit,
    showCaptionDialog: Boolean,
    onDismissCaption: () -> Unit,
    storyCaption: String,
    onCaptionChange: (String) -> Unit,
    onPublishMediaStory: () -> Unit,
    showTextStoryDialog: Boolean,
    onDismissTextStory: () -> Unit,
    textStoryContent: String,
    onTextStoryContentChange: (String) -> Unit,
    textStoryColor: Color,
    onTextStoryColorChange: (Color) -> Unit,
    onPublishTextStory: () -> Unit
) {
    val s = LocalAppStrings.current
    val primary = MaterialTheme.colorScheme.primary

    if (showStoryTypeDialog) {
        AlertDialog(
            onDismissRequest = onDismissStoryType,
            title = { Text(s.addStory) },
            text = { Text(s.addStoryHint) },
            confirmButton = {
                Column {
                    TextButton(onClick = onPickPhoto) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.photo)
                        }
                    }
                    TextButton(onClick = onPickVideo) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.video)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onTextStory) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextSnippet, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.textOnly)
                    }
                }
            }
        )
    }

    if (showCaptionDialog) {
        AlertDialog(
            onDismissRequest = onDismissCaption,
            title = { Text(s.addCaption) },
            text = {
                Column {
                    Text(s.addCaptionHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = storyCaption,
                        onValueChange = onCaptionChange,
                        placeholder = { Text(s.yourMessage) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = onPublishMediaStory) {
                    Text(s.publish)
                }
            },
            dismissButton = {
                TextButton(onClick = onPublishMediaStory) { // "Skip" acts as publish without caption
                    Text(s.skip)
                }
            }
        )
    }

    if (showTextStoryDialog) {
        AlertDialog(
            onDismissRequest = onDismissTextStory,
            title = { Text(s.textStory) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textStoryContent,
                        onValueChange = onTextStoryContentChange,
                        placeholder = { Text(s.whatDoYouWantToSay) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = textStoryColor.copy(alpha = 0.1f))
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(s.backgroundColor, style = MaterialTheme.typography.labelSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val secondary = MaterialTheme.colorScheme.secondary
                        listOf(primary, secondary, BlueAccent, RedAccent, Color.DarkGray).forEach { color ->
                            Box(
                                Modifier.size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .border(if (textStoryColor == color) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
                                    .clickable { onTextStoryColorChange(color) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onPublishTextStory, enabled = textStoryContent.isNotBlank()) {
                    Text(s.publish)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissTextStory) {
                    Text(s.cancel)
                }
            }
        )
    }
}
