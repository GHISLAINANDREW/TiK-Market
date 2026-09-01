package com.tik_market.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.tik_market.api.dto.ApiOrder
import com.tik_market.api.dto.ApiUser
import com.tik_market.data.models.CartItem
import com.tik_market.data.models.Product
import com.tik_market.ui.story.StoryItem
import com.tik_market.ui.vendor.ProductForm
import com.tik_market.utils.getSavedLanguage
import com.tik_market.utils.setSavedLanguage
import com.tik_market.utils.getStrings

data class BottomNavItem(val screen: NavScreen, val icon: ImageVector, val activeIcon: ImageVector, val label: String)

@Stable
class AppState(
    currentScreenInitial: NavScreen,
    cartItemsInitial: List<CartItem>,
    isLoggedInInitial: Boolean,
    userNameInitial: String,
    userRoleInitial: String,
    userTokenInitial: String?,
    vendorShopNameInitial: String,
    unreadMessagesInitial: Int,
    isDarkModeInitial: Boolean = false
) {
    var currentScreen by mutableStateOf(currentScreenInitial)
    var cartItems by mutableStateOf(cartItemsInitial)
    var selectedProduct by mutableStateOf<Product?>(null)
    var previousScreens by mutableStateOf<List<NavScreen>>(emptyList())
    var isLoggedIn by mutableStateOf(isLoggedInInitial)
    var userName by mutableStateOf(userNameInitial)
    var userRole by mutableStateOf(userRoleInitial)
    var currentUser by mutableStateOf<ApiUser?>(null)
    var userToken by mutableStateOf(userTokenInitial)
    var vendorShopName by mutableStateOf(vendorShopNameInitial)
    var refreshSignal by mutableStateOf(0)
    var intentSignal by mutableStateOf(0)
    var editProductData by mutableStateOf<ProductForm?>(null)
    var checkoutAmount by mutableStateOf(0.0)
    var checkoutPaymentMethod by mutableStateOf("Orange Money")
    var checkoutPhone by mutableStateOf("")
    var chatVendorName by mutableStateOf("")
    var chatVendorAvatar by mutableStateOf<String?>(null)
    var chatProductTitle by mutableStateOf<String?>(null)
    var chatProductImage by mutableStateOf<String?>(null)
    var chatProductPrice by mutableStateOf<String?>(null)
    var chatVendorId by mutableStateOf(0)
    var chatVendorIsOnline by mutableStateOf(false)
    var unreadMessages by mutableStateOf(unreadMessagesInitial)
    var unreadNotifications by mutableStateOf(0)
    var currentPoints by mutableStateOf(0)
    var totalPoints by mutableStateOf(0)
    var walletBalance by mutableStateOf(0.0)
    var walletTier by mutableStateOf("bronze")
    var walletCashbackPct by mutableStateOf(1.0)
    var walletBonusPct by mutableStateOf(0.0)
    var nextTierPointsNeeded by mutableStateOf(0)
    var nextTierName by mutableStateOf<String?>(null)
    
    var isDarkMode by mutableStateOf(isDarkModeInitial)
    var language by mutableStateOf(getSavedLanguage())
    val strings get() = getStrings(language)
    var paymentOrder by mutableStateOf<ApiOrder?>(null)
    var selectedShopName by mutableStateOf<String?>(null)
    var selectedShopId by mutableStateOf(0)
    var selectedLiveStreamId by mutableStateOf(0)
    
    // Data Cache
    var cachedProducts by mutableStateOf<List<Product>>(emptyList())
    var cachedCategories by mutableStateOf<List<String>>(emptyList())
    var wishlistProductIds by mutableStateOf<Set<Int>>(emptySet())

    // Comparison list
    var comparisonList by mutableStateOf<List<Product>>(emptyList())

    // Search History
    var searchHistory by mutableStateOf<List<String>>(emptyList())

    // Registration mode (true = show register form instead of login)
    var isRegisterMode by mutableStateOf(false)

    // Story Viewer
    var storyItems by mutableStateOf<List<StoryItem>>(emptyList())
    var storyIndex by mutableStateOf(0)

    fun toggleComparison(product: Product) {
        comparisonList = if (comparisonList.any { it.id == product.id }) {
            comparisonList.filter { it.id != product.id }
        } else {
            (comparisonList + product).take(4) // Limite à 4 produits
        }
    }

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        searchHistory = (listOf(query) + searchHistory.filter { it != query }).take(5)
    }

    fun removeSearchQuery(query: String) {
        searchHistory = searchHistory.filter { it != query }
    }

    fun navigateTo(screen: NavScreen) {
        previousScreens = previousScreens + currentScreen
        currentScreen = screen
    }

    fun goBack() {
        if (previousScreens.isNotEmpty()) {
            currentScreen = previousScreens.last()
            previousScreens = previousScreens.dropLast(1)
        } else if (currentScreen != NavScreen.Home) {
            currentScreen = NavScreen.Home
        }
    }

    fun goHome() {
        previousScreens = emptyList()
        selectedShopName = null
        currentScreen = NavScreen.Home
    }

    fun updateLanguage(lang: String) {
        language = lang
        setSavedLanguage(lang)
    }

    fun refreshWallet() {
        if (!isLoggedIn) return
        // Trigger a global refresh signal for wallet
        refreshSignal++
    }

    fun updateWallet(w: com.tik_market.api.dto.ApiWallet) {
        currentPoints = w.currentPoints
        totalPoints = w.totalPoints
        walletBalance = w.balance
        walletTier = w.tier
        walletCashbackPct = w.cashbackPct
        walletBonusPct = w.bonusPct
        nextTierPointsNeeded = w.nextTier?.pointsNeeded ?: 0
        nextTierName = w.nextTier?.name
    }
}
