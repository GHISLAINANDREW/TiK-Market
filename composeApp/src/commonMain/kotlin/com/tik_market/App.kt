// TiK-Market App - Version 1.0.2 (JDK 17 fix)
package com.tik_market

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiCartItemBody
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiOrder
import com.tik_market.api.toProduct
import com.tik_market.data.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import com.tik_market.theme.*
import com.tik_market.ui.auth.AuthScreen
import com.tik_market.ui.cart.CartScreen
import com.tik_market.ui.chat.ChatScreen
import com.tik_market.ui.chat.ConversationsScreen
import com.tik_market.ui.checkout.CheckoutScreen
import com.tik_market.ui.payment.PaymentScreen
import com.tik_market.ui.home.HomeScreen
import com.tik_market.ui.notifications.NotificationScreen
import com.tik_market.ui.product.ProductDetailScreen
import com.tik_market.ui.orders.OrdersScreen
import com.tik_market.ui.profile.ProfileScreen
import com.tik_market.ui.profile.EditProfileScreen
import com.tik_market.ui.settings.SettingsScreen
import com.tik_market.ui.wishlist.WishlistScreen
import com.tik_market.ui.shop.ShopPageScreen
import com.tik_market.ui.shop.ShopsListScreen
import com.tik_market.ui.vendor.AddProductScreen
import com.tik_market.ui.vendor.ProductForm
import com.tik_market.ui.vendor.VendorDashboardScreen
import com.tik_market.ui.vendor.CreateShopScreen
import com.tik_market.ui.vendor.ManageOrdersScreen
import com.tik_market.ui.vendor.ManageShopScreen
import com.tik_market.ui.vendor.VendorGroupBuysScreen
import com.tik_market.ui.vendor.SubscribersScreen
import com.tik_market.ui.loyalty.LoyaltyScreen
import com.tik_market.ui.loyalty.NotificationPrefsScreen
import com.tik_market.ui.admin.AdminDashboardScreen
import com.tik_market.navigation.NavScreen
import com.tik_market.navigation.AppState
import com.tik_market.navigation.BottomNavItem
import com.tik_market.ui.onboarding.OnboardingManager
import com.tik_market.ui.onboarding.OnboardingScreen
import com.tik_market.utils.BackPressHandler
import com.tik_market.utils.NotificationUtils
import com.tik_market.utils.updateUnreadBadge
import com.tik_market.utils.observeConnectivity
import com.tik_market.ui.barcode.BarcodeScanScreen
import com.tik_market.ui.compare.CompareScreen
import com.tik_market.ui.story.StoryViewerScreen
import com.tik_market.ui.profile.FollowedShopsScreen
import com.tik_market.ui.misc.MyGroupBuysScreen
import com.tik_market.ui.misc.ShopsMapScreen
import com.tik_market.ui.misc.SplashScreen
import com.tik_market.ui.misc.LegalNoticeScreen
import com.tik_market.ui.misc.TermsOfUseScreen

@Composable
private fun rememberAppState(initialScreen: NavScreen = NavScreen.Splash) = remember {
    AppState(
        currentScreenInitial = initialScreen,
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
fun App(onExit: () -> Unit = {}, initialScreen: NavScreen = NavScreen.Splash) {
    val appState = rememberAppState(initialScreen)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOnboarding by remember { mutableStateOf(OnboardingManager.isFirstLaunch()) }

    var userCity by remember { mutableStateOf<String?>(null) }
    // Ville de l'app choisie manuellement ou via redirection (prioritaire sur la position GPS/profil)
    var manualCity by remember { mutableStateOf<String?>(null) }
    // Ville de l'app proposée à l'utilisateur quand il est proche (≤ 20 km) d'une ville couverte
    var suggestedCity by remember { mutableStateOf<com.tik_market.utils.AppCity?>(null) }
    // Villes que l'utilisateur a refusées (pour ne pas re-proposer en boucle)
    var declinedCities by remember { mutableStateOf<Set<String>>(emptySet()) }
    // N'empêche pas l'utilisateur de consulter les produits de sa ville détectée
    var detectedLocationName by remember { mutableStateOf<String?>(null) }

    // Détermine la ville active :
    // - Non connecté : pas de filtre ville, branding par défaut (TiK-Market).
    // - Connecté : ville du profil si elle correspond à une ville connue, sinon détection GPS.
    LaunchedEffect(appState.isLoggedIn, appState.currentUser) {
        if (manualCity != null) {
            userCity = manualCity
            return@LaunchedEffect
        }

        if (appState.isLoggedIn && appState.currentUser != null) {
            val loc = appState.currentUser?.location
            val knownCity = com.tik_market.utils.appCities.firstOrNull {
                loc?.contains(it.name, ignoreCase = true) == true
            }
            if (knownCity != null) {
                userCity = knownCity.name
            } else {
                com.tik_market.utils.getCurrentLocationLatLng { lat, lng ->
                    if (lat != null && lng != null) {
                        val nearby = com.tik_market.utils.findNearbyAppCity(lat, lng)
                        userCity = nearby?.name ?: loc
                    } else {
                        userCity = loc
                    }
                }
            }
        } else {
            userCity = null
        }
    }

    // Détection périodique de la ville (connecté uniquement) :
    // si l'utilisateur se déplace vers une autre ville de l'app, on lui propose de basculer.
    LaunchedEffect(appState.isLoggedIn) {
        if (!appState.isLoggedIn) return@LaunchedEffect
        while (true) {
            com.tik_market.utils.getCurrentLocationLatLng { lat, lng ->
                if (lat != null && lng != null) {
                    val nearby = com.tik_market.utils.findNearbyAppCity(lat, lng)
                    val currentCity = userCity ?: ""
                    if (nearby != null &&
                        !currentCity.contains(nearby.name, ignoreCase = true) &&
                        nearby.name !in declinedCities
                    ) {
                        suggestedCity = nearby
                    }
                }
            }
            delay(30000) // re-vérifie toutes les 30 s
        }
    }

    TiKMarketTheme(darkTheme = appState.isDarkMode, city = userCity) {
        // ... (rest of the file)
        if (showOnboarding) {
            OnboardingScreen(
                onComplete = {
                    OnboardingManager.markOnboardingComplete()
                    showOnboarding = false
                }
            )
        } else {
            // ── Connectivity observer ──
            var isOnline by remember { mutableStateOf(true) }
            var showOfflineSnackbar by remember { mutableStateOf(false) }
            var wasOffline by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                observeConnectivity { online ->
                    isOnline = online
                    if (!online) {
                        wasOffline = true
                        showOfflineSnackbar = true
                    } else if (wasOffline) {
                        // Just came back online
                        showOfflineSnackbar = true
                        wasOffline = false
                    }
                }
            }

            // Show snackbar on connectivity change
            LaunchedEffect(showOfflineSnackbar) {
                if (showOfflineSnackbar) {
                    val msg = if (isOnline) "✅ Connexion rétablie" else "🔴 Connexion perdue"
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                    showOfflineSnackbar = false
                }
            }

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
                
                // Trigger full refresh (stories, products, etc.) after login restored
                appState.refreshSignal++

                // Fetch unread counts initially
                if (appState.isLoggedIn) {
                    try {
                        appState.unreadMessages = ApiClient.fetchUnreadCount()
                        appState.unreadNotifications = ApiClient.fetchNotifications().count { !it.isRead }
                    } catch (_: Exception) {}
                }

                // Check for Deep Link (Product)
                val productId = com.tik_market.utils.getStartupParameter("p")
                if (productId != null) {
                    try {
                        val p = ApiClient.fetchProduct(productId.toInt()).toProduct()
                        appState.selectedProduct = p
                        appState.navigateTo(NavScreen.ProductDetail)
                    } catch (_: Exception) {}
                }

                // Listen for tab focus to refresh data
                com.tik_market.utils.setupTabFocusRefresh {
                    appState.refreshSignal++
                    scope.launch {
                        try {
                            if (appState.isLoggedIn) {
                                appState.unreadMessages = ApiClient.fetchUnreadCount()
                                appState.unreadNotifications = ApiClient.fetchNotifications().count { !it.isRead }
                                updateUnreadBadge(appState.unreadMessages + appState.unreadNotifications)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Show MainContent IMMEDIATELY — data loads in background
            MainContent(appState, onExit, scope, snackbarHostState, isOnline, userCity)

            // ── Alerte système : proposition de redirection vers une ville de l'app ──
            suggestedCity?.let { city ->
                AlertDialog(
                    onDismissRequest = {
                        declinedCities = declinedCities + city.name
                        suggestedCity = null
                    },
                    title = { Text("Vous êtes proche de ${city.name}") },
                    text = {
                        Text("Souhaitez-vous voir les produits de ${city.name} ? " +
                            "Choisir « Voir les produits » pour afficher le marché de ${city.name}.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            manualCity = city.name
                            userCity = city.name
                            declinedCities = declinedCities - city.name
                            suggestedCity = null
                        }) { Text("Voir les produits") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            declinedCities = declinedCities + city.name
                            suggestedCity = null
                        }) { Text("Rester ici") }
                    }
                )
            }

            // Listen for notification navigation signal
            LaunchedEffect(Unit) {
                NotificationUtils.navigationEvents.collect {
                    val notifType = com.tik_market.utils.getStartupParameter("notif_type")
                    if (notifType != null) {
                        val relatedId = com.tik_market.utils.getStartupParameter("notif_id")?.toIntOrNull()
                        com.tik_market.utils.setStartupParameter("notif_type", null)
                        com.tik_market.utils.setStartupParameter("notif_id", null)
                        
                        if (notifType == "notification" || notifType == "story") {
                            appState.navigateTo(NavScreen.Notifications)
                        } else if (notifType == "product") {
                            if (relatedId != null) {
                                try {
                                    val p = ApiClient.fetchProduct(relatedId).toProduct()
                                    appState.selectedProduct = p
                                    appState.navigateTo(NavScreen.ProductDetail)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(appState: AppState, onExit: () -> Unit, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, isOnline: Boolean = true, userCity: String? = null) {
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
        BottomNavItem(NavScreen.Home, Icons.Default.Home, Icons.Filled.Home, "Accueil"),
        BottomNavItem(NavScreen.Conversations, Icons.Default.Chat, Icons.Filled.Chat, "Messages"),
        BottomNavItem(NavScreen.Cart, Icons.Default.ShoppingCart, Icons.Filled.ShoppingCart, "Panier"),
        BottomNavItem(NavScreen.Profile, Icons.Default.Person, Icons.Filled.Person, "Compte")
    )

    val hideBottomBar = appState.currentScreen in listOf(
        NavScreen.ProductDetail, NavScreen.Chat, NavScreen.Auth, NavScreen.Payment,
        NavScreen.BarcodeScan, NavScreen.Checkout, NavScreen.AddProduct,
        NavScreen.Compare, NavScreen.VendorDashboard, NavScreen.ShopPage,
        NavScreen.StoryViewer, NavScreen.AdminDashboard
    )

    BoxWithConstraints(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE6E0F0), Color(0xFFDED9E9))
                )
            )
    ) {
        val screenWidth = maxWidth
        val isDesktop = screenWidth > 600.dp
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .then(if (isDesktop) Modifier.width(420.dp).fillMaxHeight().padding(vertical = 16.dp).clip(RoundedCornerShape(16.dp)).shadow(12.dp) else Modifier.fillMaxSize())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!hideBottomBar) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                                tonalElevation = 8.dp,
                                shadowElevation = 16.dp
                            ) {
                                AppBottomBar(appState, bottomItems)
                            }
                        }
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize()) {
                        // ── Offline/Online persistent bar ──
                        AnimatedVisibility(
                            visible = !isOnline,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Box(
                                Modifier.fillMaxWidth().background(Color(0xFFD32F2F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🔴 Connexion perdue — certaines fonctionnalités peuvent être limitées",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            AppNavigation(appState, scope, snackbarHostState, userCity)
                        }
                    }
                }
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
                // 1. Poll Messages
                try {
                    val msgCount = ApiClient.fetchUnreadCount()
                    if (msgCount > previousUnreadMessages) {
                        NotificationUtils.showNotification("Nouveau message", "Vous avez reçu un message")
                    }
                    previousUnreadMessages = msgCount
                    appState.unreadMessages = msgCount
                } catch (e: Exception) {
                    println("[Polling] Message error: ${e.message}")
                }

                // 2. Poll Wallet/Points (Always do this)
                try {
                    val w = ApiClient.fetchWallet()
                    if (w != null) {
                        appState.currentPoints = w.currentPoints
                        appState.totalPoints = w.totalPoints
                        appState.walletBalance = w.balance
                        appState.walletTier = w.tier
                    }
                } catch (e: Exception) {
                    println("[Polling] Wallet error: ${e.message}")
                }

                // 3. Update Global Badge
                updateUnreadBadge(appState.unreadMessages + appState.unreadNotifications)

                // 4. Poll Notifications
                try {
                    val notifications = ApiClient.fetchNotifications()
                    val unreadNotifs = notifications.count { !it.isRead }
                    if (unreadNotifs > previousUnreadNotifications) {
                        notifications.filter { !it.isRead }.forEach { notif ->
                            NotificationUtils.showNotification(notif.title, notif.message)
                        }
                    }
                    previousUnreadNotifications = unreadNotifs
                    appState.unreadNotifications = unreadNotifs
                } catch (e: Exception) {
                    println("[Polling] Notif error: ${e.message}")
                }
                
                delay(4000) // Slightly increased to be gentle on server
            } catch (e: Exception) {
                println("[Polling] Global loop error: ${e.message}")
                delay(10000)
            }
        }
    }

    // Force refresh on screen change to important screens
    LaunchedEffect(appState.currentScreen) {
        if (appState.isLoggedIn && (appState.currentScreen == NavScreen.Profile || appState.currentScreen == NavScreen.Loyalty)) {
            try {
                val w = ApiClient.fetchWallet()
                if (w != null) {
                    appState.currentPoints = w.currentPoints
                    appState.totalPoints = w.totalPoints
                    appState.walletBalance = w.balance
                    appState.walletTier = w.tier
                }
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun AppNavigation(appState: AppState, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, userCity: String? = null) {
    val showError: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    AnimatedContent(
        targetState = appState.currentScreen,
        transitionSpec = {
            if (targetState.route == NavScreen.Splash.route) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (targetState.route == NavScreen.Home.route) {
                (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut() + scaleOut(targetScale = 1.1f))
            } else {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "app_nav"
    ) { screen ->
        when (screen) {
            NavScreen.Splash -> SplashScreen(onFinished = { appState.navigateTo(NavScreen.Home) })
            NavScreen.Home -> HomeScreen(
                onProductClick = { p -> appState.selectedProduct = p; appState.navigateTo(NavScreen.ProductDetail) },
                overrideCity = userCity,
                onAddToCart = { p -> 
                    appState.cartItems += CartItem(p, 1, p.shopName)
                    scope.launch { snackbarHostState.showSnackbar("${p.title} ajouté au panier") }
                },
                onCartClick = { appState.navigateTo(NavScreen.Cart) },
                onVendorClick = {
                    if (appState.isLoggedIn) {
                        if (appState.userRole == "vendor") appState.navigateTo(NavScreen.VendorDashboard)
                        else if (appState.userRole == "admin" || appState.userRole == "super_admin") appState.navigateTo(NavScreen.AdminDashboard)
                        else appState.navigateTo(NavScreen.Profile)
                    } else {
                        appState.isRegisterMode = true
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
                onAddStory = { dataUrl, name, caption ->
                    scope.launch {
                        try {
                            val snackbarJob = launch {
                                snackbarHostState.showSnackbar("Préparation de la story...", duration = SnackbarDuration.Indefinite)
                            }
                            
                            var shop = ApiClient.fetchShopByVendor()
                            // Admin without a shop: use the first available shop
                            if (shop == null && ApiClient.isAdmin()) {
                                val allShops = ApiClient.fetchAllShops()
                                shop = allShops.firstOrNull()
                            }
                            if (shop != null) {
                                val uploadedUrl = if (dataUrl.startsWith("data:")) {
                                    ApiClient.uploadImage(dataUrl, name)
                                } else {
                                    dataUrl // it might be a color code or already uploaded URL
                                }

                                // Use new dedicated stories API
                                val mediaType = when {
                                    name.endsWith(".mp4") || name.endsWith(".webm") || name.endsWith(".mov") -> "video"
                                    name == "text" -> "text"
                                    else -> "image"
                                }
                                
                                ApiClient.createStory(
                                    shopId = shop.id,
                                    mediaUrl = uploadedUrl,
                                    mediaType = mediaType,
                                    caption = caption
                                )
                                snackbarJob.cancel()
                                appState.refreshSignal++
                                snackbarHostState.showSnackbar("✅ Story publiée ! Elle disparaîtra dans 24h.")
                            } else {
                                snackbarJob.cancel()
                                snackbarHostState.showSnackbar("❌ Aucune boutique disponible. Créez une boutique d'abord.")
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
                onEditProfileClick = { appState.navigateTo(NavScreen.EditProfile) },
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
                onLegalClick = { appState.navigateTo(NavScreen.Legal) },
                onTermsClick = { appState.navigateTo(NavScreen.Terms) },
                walletBalance = appState.walletBalance,
                walletPoints = appState.currentPoints,
                walletTier = appState.walletTier,
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
                onBack = { appState.goBack(); appState.isRegisterMode = false },
                initialModeRegister = appState.isRegisterMode,
                onLoginSuccess = { token, name, role ->
                    appState.isRegisterMode = false
                    scope.launch {
                        ApiClient.setToken(token)
                        appState.isLoggedIn = true
                        appState.userName = name
                        appState.userRole = role
                        appState.currentUser = ApiClient.getCurrentUser()
                        
                        // Fetch shop name if vendor
                        if (role == "vendor") {
                            try {
                                val shop = ApiClient.fetchShopByVendor()
                                appState.vendorShopName = shop?.name ?: ""
                            } catch (_: Exception) {}
                        }
                        
                        // Fetch wallet points immediately after login
                        try {
                            val w = ApiClient.fetchWallet()
                            if (w != null) {
                                appState.currentPoints = w.currentPoints
                                appState.totalPoints = w.totalPoints
                                appState.walletBalance = w.balance
                                appState.walletTier = w.tier
                            }
                        } catch (_: Exception) {}

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
                                snackbarHostState.showSnackbar("Commande enregistrée ! Vous gagnerez des points à la livraison.")
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
                        // Points awarded automatically by backend upon delivery
                        appState.navigateTo(NavScreen.Orders)
                        scope.launch {
                            snackbarHostState.showSnackbar("Paiement réussi ! Points fidélité à la livraison.")
                        }
                    }
                )
            } ?: appState.goHome()
            NavScreen.Settings -> SettingsScreen(
                onBack = { appState.goBack() },
                isDarkMode = appState.isDarkMode,
                onToggleDarkMode = { appState.isDarkMode = !appState.isDarkMode },
                language = appState.language,
                onToggleLanguage = { appState.updateLanguage(if (appState.language == "fr") "en" else "fr") },
                onAboutClick = { /* Show about dialog or navigate */ },
                onLegalClick = { appState.navigateTo(NavScreen.Legal) },
                onTermsClick = { appState.navigateTo(NavScreen.Terms) },
                onDownloadApk = { com.tik_market.utils.downloadFile("https://github.com/GHISLAINANDREW/TiK-Market/releases/download/beta/TiK-Market.apk", "TiK-Market.apk") }
            )
            NavScreen.Wishlist -> WishlistScreen(
                onBack = { appState.goBack() },
                onProductClick = { p -> 
                    appState.selectedProduct = p.toProduct()
                    appState.navigateTo(NavScreen.ProductDetail)
                },
                onError = showError
            )
            NavScreen.ShopsList -> ShopsListScreen(onBack = { appState.goBack() }, city = userCity) { shop ->
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
                onCouponClick = { code -> scope.launch { snackbarHostState.showSnackbar("Coupon $code copié !") } },
                currentPoints = appState.currentPoints,
                totalPoints = appState.totalPoints,
                walletBalance = appState.walletBalance,
                walletTier = appState.walletTier,
                onRefresh = {
                    scope.launch {
                        try {
                            val w = ApiClient.fetchWallet()
                            if (w != null) {
                                appState.currentPoints = w.currentPoints
                                appState.totalPoints = w.totalPoints
                                appState.walletBalance = w.balance
                                appState.walletTier = w.tier
                            }
                        } catch (_: Exception) {}
                    }
                }
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
                                productTitle = product.title,
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
            NavScreen.EditProfile -> EditProfileScreen(
                onBack = { appState.goBack() },
                onProfileUpdated = { updatedUser ->
                    appState.currentUser = updatedUser
                    appState.userName = updatedUser.name
                }
            )
            NavScreen.Legal -> LegalNoticeScreen(onBack = { appState.goBack() })
            NavScreen.Terms -> TermsOfUseScreen(onBack = { appState.goBack() })
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Écran non implémenté") }
        }
    }
}
