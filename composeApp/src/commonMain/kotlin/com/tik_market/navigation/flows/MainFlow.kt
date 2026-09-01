package com.tik_market.navigation.flows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tik_market.api.*
import com.tik_market.api.dto.ApiShop
import com.tik_market.api.dto.ApiWishlistItem
import com.tik_market.api.dto.toProduct
import com.tik_market.api.dto.*
import com.tik_market.data.models.CartItem
import com.tik_market.data.models.Product
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.barcode.BarcodeScanScreen
import com.tik_market.ui.cart.CartScreen
import com.tik_market.ui.chat.ChatScreen
import com.tik_market.ui.chat.ConversationsScreen
import com.tik_market.ui.checkout.CheckoutScreen
import com.tik_market.ui.compare.CompareScreen
import com.tik_market.ui.home.HomeScreen
import com.tik_market.ui.loyalty.LoyaltyScreen
import com.tik_market.ui.loyalty.NotificationPrefsScreen
import com.tik_market.ui.misc.MyGroupBuysScreen
import com.tik_market.ui.misc.ShopsMapScreen
import com.tik_market.ui.notifications.NotificationScreen
import com.tik_market.ui.orders.OrdersScreen
import com.tik_market.ui.payment.PaymentScreen
import com.tik_market.ui.product.ProductDetailScreen
import com.tik_market.ui.profile.EditProfileScreen
import com.tik_market.ui.profile.ProfileScreen
import com.tik_market.ui.settings.SettingsScreen
import com.tik_market.ui.shop.ShopPageScreen
import com.tik_market.ui.shop.ShopsListScreen
import com.tik_market.ui.story.StoryViewerScreen
import com.tik_market.ui.live.LiveShoppingScreen
import com.tik_market.ui.live.LiveStreamingScreen
import com.tik_market.ui.reels.ReelsScreen
import com.tik_market.ui.search.ImageSearchScreen
import com.tik_market.ui.vendor.CreateReelScreen
import com.tik_market.ui.wishlist.WishlistScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainFlow(
    appState: AppState,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    userCity: String?
) {
    val showError: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    when (val screen = appState.currentScreen) {
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
            onLiveClick = { _ -> appState.navigateTo(NavScreen.LiveShopping) },
            onImageSearchClick = { appState.navigateTo(NavScreen.ImageSearch) },
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
                            val allShops = ApiClient.fetchShops()
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
                    appState.chatVendorIsOnline = false
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
            onLiveStreamingClick = { appState.navigateTo(NavScreen.LiveStreaming) },
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
            showBack = appState.previousScreens.isNotEmpty(),
            snackbarHostState = snackbarHostState
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
        NavScreen.Checkout -> CheckoutScreen(
            items = appState.cartItems,
            totalAmount = appState.cartItems.sumOf { it.subtotal },
            walletBalance = appState.walletBalance,
            onBack = { appState.goBack() },
            onPlaceOrder = { addr, ph, n, m, paymentType, useWallet ->
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
                        val order = ApiClient.createOrder(addr, ph, n, m, paymentType, items, useWallet)
                        appState.cartItems = emptyList()
                        appState.navigateTo(NavScreen.Orders)
                        
                        // Update wallet state after order (points might have changed if instant)
                        val w = ApiClient.fetchWallet()
                        if (w != null) appState.updateWallet(w)

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
            onSelectLanguage = { lang -> appState.updateLanguage(lang) },
            onAboutClick = { appState.navigateTo(NavScreen.About) },
            onLegalClick = { appState.navigateTo(NavScreen.Legal) },
            onTermsClick = { appState.navigateTo(NavScreen.Terms) },
            onDownloadApk = { com.tik_market.utils.downloadFile("https://github.com/GHISLAINANDREW/TiK-Market/releases/download/beta/TiK-Market.apk", "TiK-Market.apk") },
            onInstallPwa = { com.tik_market.utils.installPwa() }
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
        NavScreen.ImageSearch -> ImageSearchScreen(
            onBack = { appState.goBack() },
            onProductClick = { p -> appState.selectedProduct = p; appState.navigateTo(NavScreen.ProductDetail) },
            onAddToCart = { p ->
                appState.cartItems += CartItem(p, 1, p.shopName)
                scope.launch { snackbarHostState.showSnackbar("${p.title} ajouté") }
            }
        )
        NavScreen.LiveStreaming -> LiveStreamingScreen(
            onBack = { appState.goBack() }
        )
        NavScreen.CreateReel -> CreateReelScreen(
            onBack = { appState.goBack() }
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
        NavScreen.Loyalty -> LoyaltyScreen(
            onBack = { appState.goBack() },
            onCouponClick = { code -> scope.launch { snackbarHostState.showSnackbar("Coupon $code copié !") } },
            currentPoints = appState.currentPoints,
            totalPoints = appState.totalPoints,
            walletBalance = appState.walletBalance,
            walletTier = appState.walletTier,
            cashbackPct = appState.walletCashbackPct,
            bonusPct = appState.walletBonusPct,
            nextTierPointsNeeded = appState.nextTierPointsNeeded,
            nextTierName = appState.nextTierName,
            onRefresh = {
                scope.launch {
                    try {
                        val w = ApiClient.fetchWallet()
                        if (w != null) {
                            appState.updateWallet(w)
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
        NavScreen.LiveShopping -> LiveShoppingScreen(
            streamId = 1,
            onBack = { appState.goBack() },
            onProductClick = { p -> appState.selectedProduct = p; appState.navigateTo(NavScreen.ProductDetail) }
        )
        NavScreen.Reels -> ReelsScreen(
            onBack = { appState.goBack() },
            onShopClick = { id -> 
                appState.selectedShopId = id
                appState.navigateTo(NavScreen.ShopPage)
            }
        )
        else -> {}
    }
}
