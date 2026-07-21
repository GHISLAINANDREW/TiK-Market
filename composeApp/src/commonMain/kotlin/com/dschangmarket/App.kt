package com.dschangmarket

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiCartItemBody
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiOrder
import com.dschangmarket.api.toProduct
import com.dschangmarket.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.dschangmarket.theme.*
import com.dschangmarket.ui.auth.AuthScreen
import com.dschangmarket.ui.cart.CartScreen
import com.dschangmarket.ui.chat.ChatScreen
import com.dschangmarket.ui.chat.ConversationsScreen
import com.dschangmarket.ui.checkout.CheckoutScreen
import com.dschangmarket.ui.payment.PaymentScreen
import com.dschangmarket.ui.home.HomeScreen
import com.dschangmarket.ui.notifications.NotificationScreen
import com.dschangmarket.ui.product.ProductDetailScreen
import com.dschangmarket.ui.orders.OrdersScreen
import com.dschangmarket.ui.profile.ProfileScreen
import com.dschangmarket.ui.settings.SettingsScreen
import com.dschangmarket.ui.wishlist.WishlistScreen
import com.dschangmarket.ui.shop.ShopPageScreen
import com.dschangmarket.ui.shop.ShopsListScreen
import com.dschangmarket.ui.vendor.AddProductScreen
import com.dschangmarket.ui.vendor.ProductForm
import com.dschangmarket.ui.vendor.VendorDashboardScreen
import com.dschangmarket.ui.vendor.CreateShopScreen
import com.dschangmarket.ui.vendor.ManageOrdersScreen
import com.dschangmarket.ui.vendor.ManageShopScreen
import com.dschangmarket.ui.vendor.VendorGroupBuysScreen
import com.dschangmarket.ui.vendor.SubscribersScreen
import com.dschangmarket.ui.loyalty.LoyaltyScreen
import com.dschangmarket.ui.loyalty.NotificationPrefsScreen
import com.dschangmarket.ui.admin.AdminDashboardScreen
import com.dschangmarket.navigation.NavScreen
import com.dschangmarket.navigation.AppState
import com.dschangmarket.navigation.BottomNavItem
import com.dschangmarket.ui.onboarding.OnboardingManager
import com.dschangmarket.ui.onboarding.OnboardingScreen
import com.dschangmarket.utils.BackPressHandler
import com.dschangmarket.utils.NotificationUtils
import com.dschangmarket.ui.barcode.BarcodeScanScreen
import com.dschangmarket.ui.compare.CompareScreen
import com.dschangmarket.ui.story.StoryViewerScreen
import com.dschangmarket.ui.profile.FollowedShopsScreen
import com.dschangmarket.ui.misc.MyGroupBuysScreen
import com.dschangmarket.ui.misc.ShopsMapScreen

@Composable
private fun rememberAppState() = remember {
    AppState(
        currentScreenInitial = NavScreen.Home,
        cartItemsInitial = emptyList(),
        isLoggedInInitial = false,
        userNameInitial = "",
        userRoleInitial = "buyer",
        userTokenInitial = null,
        vendorShopNameInitial = "",
        unreadMessagesInitial = 0,
        isDarkModeInitial = false,
    )
}

@Composable
fun App(onExit: () -> Unit = {}) {
    val appState = rememberAppState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOnboarding by remember { mutableStateOf(OnboardingManager.isFirstLaunch()) }

    DschangTheme(darkTheme = appState.isDarkMode) {
        if (showOnboarding) {
            OnboardingScreen(
                onComplete = {
                    OnboardingManager.markOnboardingComplete()
                    showOnboarding = false
                }
            )
        } else {
            // Restore session in background — does NOT block UI
            LaunchedEffect(Unit) {
                ApiClient.initToken()
                if (ApiClient.isLoggedIn()) {
                    try {
                        val user = ApiClient.fetchMe()
                        appState.isLoggedIn = true
                        appState.userName = user.name
                        appState.userRole = user.role
                        appState.currentUser = user
                        if (user.role == "vendor") {
                            val shop = ApiClient.fetchShopByVendor()
                            appState.vendorShopName = shop?.name ?: ""
                        }
                    } catch (_: Exception) {
                        ApiClient.logout()
                        appState.isLoggedIn = false
                        scope.launch { snackbarHostState.showSnackbar("Session expirée") }
                    }
                }
                
                // Fetch unread counts initially
                if (appState.isLoggedIn) {
                    try {
                        appState.unreadMessages = ApiClient.fetchUnreadCount()
                        appState.unreadNotifications = ApiClient.fetchNotifications().count { !it.isRead }
                    } catch (_: Exception) {}
                }

                // Check for Deep Link (Product)
                val productId = com.dschangmarket.utils.getStartupParameter("p")
                if (productId != null) {
                    try {
                        val p = ApiClient.fetchProduct(productId.toInt()).toProduct()
                        appState.selectedProduct = p
                        appState.navigateTo(NavScreen.ProductDetail)
                    } catch (_: Exception) {}
                }
            }

            // Show MainContent IMMEDIATELY — data loads in background
            MainContent(appState, onExit, scope, snackbarHostState)
        }
    }
}

@Composable
fun MainContent(appState: AppState, onExit: () -> Unit, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState) {
    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000L)
            backPressedOnce = false
        }
    }

    BackPressHandler(enabled = true) {
        if (appState.currentScreen == NavScreen.Home && appState.previousScreens.isEmpty()) {
            if (backPressedOnce) onExit()
            else {
                backPressedOnce = true
                scope.launch { snackbarHostState.showSnackbar("Appuyez encore pour quitter") }
            }
        } else {
            appState.goBack()
        }
    }

    // Polling logic
    PollingManager(appState)

    val bottomItems = listOf(
        BottomNavItem(NavScreen.Home, Icons.Outlined.Home, Icons.Filled.Home, "Accueil"),
        BottomNavItem(NavScreen.Conversations, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble, "Messages"),
        BottomNavItem(NavScreen.Cart, Icons.Outlined.ShoppingCart, Icons.Filled.ShoppingCart, "Panier"),
        BottomNavItem(NavScreen.Profile, Icons.Outlined.Person, Icons.Filled.Person, "Compte")
    )

    val hideBottomBar = appState.currentScreen in listOf(
        NavScreen.ProductDetail, NavScreen.Chat, NavScreen.Auth, NavScreen.Payment,
        NavScreen.BarcodeScan, NavScreen.Checkout, NavScreen.AddProduct,
        NavScreen.Compare, NavScreen.VendorDashboard, NavScreen.ShopPage,
        NavScreen.StoryViewer, NavScreen.AdminDashboard
    )

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        containerColor = Color.Transparent, // Transparent to show gradient
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!hideBottomBar) {
                AppBottomBar(appState, bottomItems)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE6E0F0), Color(0xFFDED9E9))
                    )
                )
        ) {
            // "Transparent" overlay effect
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(padding)
            ) {
                AppNavigation(appState, scope, snackbarHostState)
            }
        }
    }
}

@Composable
fun AppBottomBar(appState: AppState, items: List<BottomNavItem>) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        items.forEach { item ->
            val selected = appState.currentScreen.route == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if ((item.screen == NavScreen.Profile || item.screen == NavScreen.Conversations) && !appState.isLoggedIn) {
                        appState.navigateTo(NavScreen.Auth)
                    } else {
                        appState.currentScreen = item.screen
                        appState.previousScreens = emptyList()
                    }
                },
                icon = {
                    val badgeCount = when (item.screen) {
                        NavScreen.Cart -> appState.cartItems.size
                        NavScreen.Conversations -> appState.unreadMessages
                        else -> 0
                    }
                    if (badgeCount > 0) {
                        BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                            Icon(if (selected) item.activeIcon else item.icon, item.label)
                        }
                    } else {
                        Icon(if (selected) item.activeIcon else item.icon, item.label)
                    }
                },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Orange, selectedTextColor = Orange, indicatorColor = Color(0xFFFFF3E0)
                )
            )
        }
    }
}

@Composable
fun PollingManager(appState: AppState) {
    var previousUnreadMessages by remember { mutableStateOf(0) }
    var previousUnreadNotifications by remember { mutableStateOf(0) }
    
    LaunchedEffect(appState.isLoggedIn) {
        if (!appState.isLoggedIn) return@LaunchedEffect
        while (true) {
            try {
                // Poll Messages
                val msgCount = ApiClient.fetchUnreadCount()
                if (msgCount > previousUnreadMessages) {
                    NotificationUtils.showNotification("Nouveau message", "Vous avez reçu un message")
                }
                previousUnreadMessages = msgCount
                appState.unreadMessages = msgCount

                // Poll Notifications
                val notifications = ApiClient.fetchNotifications()
                val unreadNotifs = notifications.count { !it.isRead }
                if (unreadNotifs > previousUnreadNotifications) {
                    notifications.filter { !it.isRead }.forEach { notif ->
                        NotificationUtils.showNotification(notif.title, notif.message)
                    }
                }
                previousUnreadNotifications = unreadNotifs
                appState.unreadNotifications = unreadNotifs
                
                delay(3000) // Reduced from 15s to 3s for better interactivity
            } catch (_: Exception) {
                delay(10000)
            }
        }
    }
}

@Composable
fun AppNavigation(appState: AppState, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState) {
    val showError: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    AnimatedContent(
        targetState = appState.currentScreen,
        transitionSpec = {
            if (targetState.route == NavScreen.Home.route) {
                (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut() + scaleOut(targetScale = 1.1f))
            } else {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "app_nav"
    ) { screen ->
        when (screen) {
            NavScreen.Home -> HomeScreen(
                onProductClick = { p -> appState.selectedProduct = p; appState.navigateTo(NavScreen.ProductDetail) },
                onAddToCart = { p -> 
                    appState.cartItems += CartItem(p, 1, p.shopName)
                    scope.launch { snackbarHostState.showSnackbar("${p.title} ajouté au panier") }
                },
                onCartClick = { appState.navigateTo(NavScreen.Cart) },
                onVendorClick = {
                    if (appState.isLoggedIn) {
                        if (appState.userRole == "vendor") appState.navigateTo(NavScreen.VendorDashboard)
                        else if (appState.userRole == "admin") appState.navigateTo(NavScreen.AdminDashboard)
                        else appState.navigateTo(NavScreen.Profile)
                    } else {
                        appState.navigateTo(NavScreen.Auth)
                    }
                },
                onShopsClick = { appState.navigateTo(NavScreen.ShopsList) },
                onNotificationsClick = { appState.navigateTo(NavScreen.Notifications) },
                cartCount = appState.cartItems.sumOf { it.quantity },
                notificationCount = appState.unreadNotifications,
                selectedShopName = appState.selectedShopName,
                onClearShopFilter = { appState.selectedShopName = null },
                onError = showError,
                comparisonCount = appState.comparisonList.size,
                onCompareClick = { appState.navigateTo(NavScreen.Compare) },
                searchHistory = appState.searchHistory,
                onSearchQuerySubmit = { query -> appState.addSearchQuery(query) },
                isLoggedIn = appState.isLoggedIn,
                userRole = appState.userRole,
                onStoryClick = { storyItems, index ->
                    appState.storyItems = storyItems
                    appState.storyIndex = index
                    appState.navigateTo(NavScreen.StoryViewer)
                },
                onAddStory = { dataUrl, name ->
                    scope.launch {
                        try {
                            val snackbarJob = launch {
                                snackbarHostState.showSnackbar("Préparation de la story...", duration = SnackbarDuration.Indefinite)
                            }
                            
                            val shop = ApiClient.fetchShopByVendor()
                            if (shop != null) {
                                val uploadedUrl = ApiClient.uploadImage(dataUrl, name)
                                // Use new dedicated stories API
                                val mediaType = if (name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mov")) "video" else "image"
                                ApiClient.createStory(
                                    shopId = shop.id,
                                    mediaUrl = uploadedUrl,
                                    mediaType = mediaType
                                )
                                snackbarJob.cancel()
                                appState.refreshSignal++
                                snackbarHostState.showSnackbar("✅ Story publiée ! Elle disparaîtra dans 24h.")
                            } else {
                                snackbarJob.cancel()
                                snackbarHostState.showSnackbar("❌ Créez une boutique d'abord.")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("❌ Erreur : ${e.message}")
                        }
                    }
                },
                refreshSignal = appState.refreshSignal,
                cachedProducts = appState.cachedProducts,
                cachedCategories = appState.cachedCategories,
                wishlistProductIds = appState.wishlistProductIds,
                onCacheData = { p, c, w ->
                    appState.cachedProducts = p
                    appState.cachedCategories = c
                    appState.wishlistProductIds = w
                }
            )
            NavScreen.ProductDetail -> appState.selectedProduct?.let { product ->
                ProductDetailScreen(
                    product = product,
                    onBack = { appState.goBack() },
                    onAddToCart = { p -> 
                        appState.cartItems += CartItem(p, 1, p.shopName)
                        scope.launch { snackbarHostState.showSnackbar("${p.title} ajouté au panier") }
                    },
                    onChat = {
                        appState.chatVendorName = product.shopName
                        appState.chatProductTitle = product.title
                        appState.chatProductImage = product.images.firstOrNull()
                        appState.chatProductPrice = "${product.price.toInt()} FCFA"
                        appState.chatVendorId = product.vendorId.toIntOrNull() ?: 0
                        appState.chatVendorAvatar = null
                        appState.navigateTo(NavScreen.Chat)
                    },
                    onShopClick = { p ->
                        appState.selectedShopId = p.shopId.toIntOrNull() ?: 0
                        appState.selectedShopName = p.shopName
                        appState.navigateTo(NavScreen.ShopPage)
                    },
                    onSimilarProductClick = { p -> appState.selectedProduct = p },
                    isCompared = appState.comparisonList.any { it.id == product.id },
                    onToggleCompare = { appState.toggleComparison(product) }
                )
            }
            NavScreen.Cart -> CartScreen(
                items = appState.cartItems,
                onBack = { appState.goBack() },
                onUpdateQuantity = { i, q -> 
                    appState.cartItems = appState.cartItems.toMutableList().apply {
                        if (q <= 0) removeAt(i) else set(i, this[i].copy(quantity = q))
                    }
                },
                onRemove = { i -> appState.cartItems = appState.cartItems.toMutableList().apply { removeAt(i) } },
                onCheckout = {
                    if (appState.isLoggedIn) appState.navigateTo(NavScreen.Checkout)
                    else appState.navigateTo(NavScreen.Auth)
                }
            )
            NavScreen.Profile -> ProfileScreen(
                isLoggedIn = appState.isLoggedIn,
                userName = appState.userName,
                userRole = appState.userRole,
                onBack = { appState.goBack() },
                onLoginClick = { appState.navigateTo(NavScreen.Auth) },
                onOrdersClick = { appState.navigateTo(NavScreen.Orders) },
                onMessagesClick = { appState.navigateTo(NavScreen.Conversations) },
                onWishlistClick = { appState.navigateTo(NavScreen.Wishlist) },
                onSettingsClick = { appState.navigateTo(NavScreen.Settings) },
                onVendorDashboardClick = { appState.navigateTo(NavScreen.VendorDashboard) },
                onAdminDashboardClick = { appState.navigateTo(NavScreen.AdminDashboard) },
                onLoyaltyClick = { appState.navigateTo(NavScreen.Loyalty) },
                onFollowedShopsClick = { appState.navigateTo(NavScreen.FollowedShops) },
                onNotifPrefsClick = { appState.navigateTo(NavScreen.NotifPrefs) },
                onGroupBuysClick = { appState.navigateTo(NavScreen.MyGroupBuys) },
                onShopsMapClick = { appState.navigateTo(NavScreen.ShopsMap) },
                onLogout = {
                    ApiClient.logout()
                    appState.isLoggedIn = false
                    appState.goHome()
                }
            )
            NavScreen.Chat -> ChatScreen(
                onBack = { appState.goBack() },
                vendorName = appState.chatVendorName,
                productTitle = appState.chatProductTitle,
                productImage = appState.chatProductImage,
                productPrice = appState.chatProductPrice,
                vendorId = appState.chatVendorId,
                vendorIsOnline = appState.chatVendorIsOnline
            )
            NavScreen.Conversations -> ConversationsScreen(
                onBack = { appState.goBack() },
                onConversationClick = { n, p, id, online ->
                    appState.chatVendorName = n
                    appState.chatProductTitle = p
                    appState.chatVendorId = id
                    appState.chatVendorIsOnline = online
                    appState.navigateTo(NavScreen.Chat)
                },
                showBack = appState.previousScreens.isNotEmpty()
            )
            NavScreen.Orders -> OrdersScreen(
                onBack = { appState.goBack() },
                onPay = { order ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Paiement désactivé pour le moment. La commande est enregistrée.")
                    }
                },
                onContactVendor = { productId ->
                    scope.launch {
                        try {
                            val product = ApiClient.fetchProduct(productId)
                            appState.chatVendorName = product.shopName
                            appState.chatProductTitle = product.title
                            appState.chatProductImage = product.imageUrl
                            appState.chatProductPrice = "${product.price} FCFA"
                            appState.chatVendorId = product.vendorId
                            appState.chatVendorAvatar = null
                            appState.chatVendorIsOnline = false
                            appState.navigateTo(NavScreen.Chat)
                        } catch (_: Exception) {
                            showError("Impossible de contacter le vendeur")
                        }
                    }
                }
            )
            NavScreen.Notifications -> NotificationScreen(
                onBack = { appState.goBack() },
                onProductClick = { id ->
                    scope.launch {
                        try {
                            val p = ApiClient.fetchProduct(id).toProduct()
                            appState.selectedProduct = p
                            appState.navigateTo(NavScreen.ProductDetail)
                        } catch (_: Exception) {}
                    }
                },
                onOrderClick = { appState.navigateTo(NavScreen.Orders) }
            )
            NavScreen.Auth -> AuthScreen(
                onBack = { appState.goBack() },
                onLoginSuccess = { token, name, role ->
                    scope.launch {
                        ApiClient.setToken(token)
                        appState.isLoggedIn = true
                        appState.userName = name
                        appState.userRole = role
                        
                        // Fetch shop name if vendor
                        if (role == "vendor") {
                            try {
                                val shop = ApiClient.fetchShopByVendor()
                                appState.vendorShopName = shop?.name ?: ""
                            } catch (_: Exception) {}
                        }

                        appState.goHome()
                    }
                }
            )
            NavScreen.VendorDashboard -> VendorDashboardScreen(
                onBack = { appState.goBack() },
                shopName = appState.vendorShopName,
                onManageShop = {
                    scope.launch {
                        val shop = ApiClient.fetchShopByVendor()
                        if (shop != null) {
                            appState.vendorShopName = shop.name
                            appState.navigateTo(NavScreen.ManageShop)
                        } else {
                            appState.navigateTo(NavScreen.CreateShop)
                        }
                    }
                },
                onAddProduct = {
                    scope.launch {
                        val shop = ApiClient.fetchShopByVendor()
                        if (shop != null) {
                            appState.vendorShopName = shop.name
                            appState.selectedProduct = null
                            appState.navigateTo(NavScreen.AddProduct)
                        } else {
                            appState.navigateTo(NavScreen.CreateShop)
                        }
                    }
                },
                onViewOrders = {
                    scope.launch {
                        val shop = ApiClient.fetchShopByVendor()
                        if (shop != null) {
                            appState.vendorShopName = shop.name
                            appState.navigateTo(NavScreen.VendorOrders)
                        } else {
                            appState.navigateTo(NavScreen.CreateShop)
                        }
                    }
                },
                onGroupBuys = {
                    scope.launch {
                        val shop = ApiClient.fetchShopByVendor()
                        if (shop != null) {
                            appState.vendorShopName = shop.name
                            appState.navigateTo(NavScreen.VendorGroupBuys)
                        } else {
                            appState.navigateTo(NavScreen.CreateShop)
                        }
                    }
                },
                onSubscribers = { appState.navigateTo(NavScreen.VendorSubscribers) }
            )
            NavScreen.ManageShop -> ManageShopScreen(
                onBack = { appState.goBack() },
                shopName = appState.vendorShopName,
                onSaveShop = { _, _, _, _, _ -> /* Géré en interne */ },
                onEditProduct = { id, title, desc, price, compare, cat, stock, unit, img ->
                    appState.selectedProduct = Product(
                        id = id.toString(),
                        title = title,
                        description = desc,
                        price = price.toDoubleOrNull() ?: 0.0,
                        comparePrice = compare.toDoubleOrNull(),
                        category = cat,
                        stock = stock.toIntOrNull() ?: 0,
                        unit = unit,
                        images = if (img.isNotBlank()) img.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                    )
                    appState.navigateTo(NavScreen.AddProduct)
                }
            )
            NavScreen.AddProduct -> AddProductScreen(
                onBack = { appState.goBack() },
                onSave = { form ->
                    scope.launch {
                        try {
                            val shop = ApiClient.fetchShopByVendor()
                            if (shop != null) {
                                // Upload new images
                                val newUploadedUrls = form.newImages.map { img ->
                                    ApiClient.uploadImage(img.dataUrl, img.fileName)
                                }
                                
                                // Combine with existing ones
                                val allImages = (form.imageUrls + newUploadedUrls).joinToString(",")
                                
                                if (form.productId == 0) {
                                    ApiClient.createProduct(
                                        shopId = shop.id,
                                        title = form.title,
                                        description = form.description,
                                        price = form.price.toDoubleOrNull() ?: 0.0,
                                        comparePrice = form.comparePrice.toDoubleOrNull(),
                                        category = form.category,
                                        stock = form.stock.toIntOrNull() ?: 0,
                                        unit = form.unit,
                                        imageUrl = allImages,
                                        isStory = form.isStory
                                    )
                                } else {
                                    ApiClient.updateProduct(
                                        productId = form.productId,
                                        title = form.title,
                                        description = form.description,
                                        price = form.price.toDoubleOrNull(),
                                        comparePrice = form.comparePrice.toDoubleOrNull(),
                                        category = form.category,
                                        stock = form.stock.toIntOrNull(),
                                        unit = form.unit,
                                        imageUrl = allImages,
                                        isStory = form.isStory
                                    )
                                }
                                appState.goBack()
                            } else {
                                showError("Vous devez d'abord créer une boutique")
                            }
                        } catch (e: Exception) {
                            showError(e.message ?: "Erreur lors de l'enregistrement")
                        }
                    }
                },
                editProduct = appState.selectedProduct?.let { p ->
                    ProductForm(
                        productId = p.id.toIntOrNull() ?: 0,
                        title = p.title,
                        description = p.description,
                        price = p.price.toString(),
                        comparePrice = p.comparePrice?.toString() ?: "",
                        category = p.category,
                        stock = p.stock.toString(),
                        unit = p.unit,
                        imageUrls = p.images,
                        isStory = p.isStory
                    )
                }
            )
            NavScreen.VendorOrders -> ManageOrdersScreen(onBack = { appState.goBack() })
            NavScreen.VendorGroupBuys -> VendorGroupBuysScreen(
                onBack = { appState.goBack() },
                shopName = appState.vendorShopName
            )
            NavScreen.VendorSubscribers -> SubscribersScreen(onBack = { appState.goBack() })
            NavScreen.FollowedShops -> FollowedShopsScreen(
                onBack = { appState.goBack() },
                onShopClick = { id ->
                    appState.selectedShopId = id
                    appState.navigateTo(NavScreen.ShopPage)
                }
            )
            NavScreen.CreateShop -> CreateShopScreen(
                onBack = { appState.goBack() },
                onShopCreated = { name ->
                    appState.vendorShopName = name
                    appState.goBack()
                }
            )
            NavScreen.Checkout -> CheckoutScreen(
                items = appState.cartItems,
                totalAmount = appState.cartItems.sumOf { it.subtotal },
                onBack = { appState.goBack() },
                onPlaceOrder = { addr, ph, n, m, paymentType ->
                    scope.launch {
                        try {
                            val items = appState.cartItems.map { cartItem ->
                                ApiCartItemBody(
                                    productId = cartItem.product.id.toIntOrNull() ?: 0,
                                    quantity = cartItem.quantity,
                                    price = cartItem.product.price,
                                    title = cartItem.product.title
                                )
                            }
                            val order = ApiClient.createOrder(addr, ph, n, m, paymentType, items)
                            appState.cartItems = emptyList()
                            appState.navigateTo(NavScreen.Orders)
                            if (paymentType == "delivery") {
                                // Earn loyalty points
                                try {
                                    val resp = ApiClient.earnPoints(order.totalAmount, order.id)
                                    if (resp.success) {
                                        snackbarHostState.showSnackbar(
                                            "${resp.earnedCashback.toInt()} FCFA cashback • ${resp.earnedPoints} pts gagnés !"
                                        )
                                    }
                                } catch (_: Exception) { }
                            } else {
                                snackbarHostState.showSnackbar(
                                    "Payez le vendeur, il validera votre commande"
                                )
                            }
                        } catch (e: Exception) { showError(e.message ?: "Erreur") }
                    }
                }
            )
            NavScreen.Payment -> appState.paymentOrder?.let { order ->
                PaymentScreen(
                    order = order,
                    onBack = { appState.goBack() },
                    onSuccess = {
                        // Gagner des points de fidélité sur la commande
                        val order = appState.paymentOrder
                        if (order != null) {
                            scope.launch {
                                try {
                                    val resp = ApiClient.earnPoints(order.totalAmount, order.id)
                                    if (resp.success) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("${resp.earnedCashback.toInt()} FCFA cashback • ${resp.earnedPoints} pts gagnés !")
                                        }
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                        appState.navigateTo(NavScreen.Orders)
                    }
                )
            }
            NavScreen.Settings -> SettingsScreen(
                onBack = { appState.goBack() },
                isDarkMode = appState.isDarkMode,
                onToggleDarkMode = { appState.isDarkMode = !appState.isDarkMode },
                language = appState.language,
                onToggleLanguage = { appState.updateLanguage(if (appState.language == "fr") "en" else "fr") },
                onAboutClick = { /* Show about dialog or navigate */ }
            )
            NavScreen.Wishlist -> WishlistScreen(
                onBack = { appState.goBack() },
                onProductClick = { p -> 
                    appState.selectedProduct = p.toProduct()
                    appState.navigateTo(NavScreen.ProductDetail)
                },
                onError = showError
            )
            NavScreen.ShopsList -> ShopsListScreen(onBack = { appState.goBack() }) { shop ->
                appState.selectedShopName = shop.name
                appState.goHome()
            }
            NavScreen.ShopPage -> ShopPageScreen(
                shopId = appState.selectedShopId,
                onBack = { appState.goBack() },
                onProductClick = { p -> appState.selectedProduct = p; appState.navigateTo(NavScreen.ProductDetail) },
                onChat = { id, n, _ -> appState.chatVendorId = id; appState.chatVendorName = n; appState.chatVendorIsOnline = false; appState.navigateTo(NavScreen.Chat) }
            )
            NavScreen.BarcodeScan -> BarcodeScanScreen(
                onBack = { appState.goBack() },
                onResult = { barcode ->
                    scope.launch {
                        try {
                            // Find product by title containing barcode or by ID if barcode is numeric ID
                            val products = ApiClient.fetchProducts(search = barcode)
                            val product = products.firstOrNull()?.toProduct()
                            if (product != null) {
                                appState.selectedProduct = product
                                appState.navigateTo(NavScreen.ProductDetail)
                            } else {
                                showError("Produit non trouvé ($barcode)")
                            }
                        } catch (e: Exception) {
                            showError("Erreur lors de la recherche")
                        }
                    }
                }
            )
            NavScreen.Compare -> CompareScreen(
                products = appState.comparisonList,
                onBack = { appState.goBack() },
                onRemoveProduct = { p -> appState.toggleComparison(p) },
                onAddToCart = { p -> 
                    appState.cartItems += CartItem(p, 1, p.shopName)
                    scope.launch { snackbarHostState.showSnackbar("${p.title} ajouté au panier") }
                },
                onProductClick = { p ->
                    appState.selectedProduct = p
                    appState.navigateTo(NavScreen.ProductDetail)
                }
            )
            NavScreen.AdminDashboard -> AdminDashboardScreen(onBack = { appState.goBack() })
            NavScreen.Loyalty -> LoyaltyScreen(
                onBack = { appState.goBack() },
                onCouponClick = { code -> scope.launch { snackbarHostState.showSnackbar("Coupon $code copié !") } }
            )
            NavScreen.NotifPrefs -> NotificationPrefsScreen(onBack = { appState.goBack() })
            NavScreen.StoryViewer -> StoryViewerScreen(
                stories = appState.storyItems,
                initialIndex = appState.storyIndex,
                onBack = { appState.goBack() },
                onProductClick = { p ->
                    appState.selectedProduct = p
                    appState.navigateTo(NavScreen.ProductDetail)
                },
                onReply = { msg, product ->
                    appState.chatVendorName = product.shopName
                    appState.chatProductTitle = product.title
                    appState.chatProductImage = product.images.firstOrNull()
                    appState.chatProductPrice = "${product.price.toInt()} FCFA"
                    appState.chatVendorId = product.vendorId.toIntOrNull() ?: 0
                    appState.chatVendorAvatar = null
                    appState.chatVendorIsOnline = false
                    scope.launch {
                        try {
                            ApiClient.sendMessage(
                                receiverId = appState.chatVendorId,
                                text = msg.trim(),
                                productId = product.id.toIntOrNull(),
                                productImageUrl = product.images.firstOrNull()
                            )
                        } catch (_: Exception) { }
                        appState.navigateTo(NavScreen.Chat)
                    }
                },
                onRefreshStories = { appState.refreshSignal++ },
                currentUserId = appState.currentUser?.id ?: 0
            )
            NavScreen.MyGroupBuys -> MyGroupBuysScreen(
                onBack = { appState.goBack() },
                onProductClick = { productId ->
                    scope.launch {
                        try {
                            val product = ApiClient.fetchProduct(productId)
                            appState.selectedProduct = product.toProduct()
                            appState.navigateTo(NavScreen.ProductDetail)
                        } catch (_: Exception) { }
                    }
                }
            )
            NavScreen.ShopsMap -> ShopsMapScreen(
                onBack = { appState.goBack() },
                onShopClick = { id ->
                    appState.selectedShopId = id
                    appState.navigateTo(NavScreen.ShopPage)
                }
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Écran non implémenté") }
        }
    }
}
