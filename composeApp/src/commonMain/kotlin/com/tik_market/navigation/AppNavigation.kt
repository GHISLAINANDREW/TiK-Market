// TiK-Market — Structure de navigation globale.
// Extrait de App.kt dans le cadre du refactoring :
// ce fichier contient le scaffold racine (MainContent) et la table de routage
// (AppNavigation) qui délègue chaque écran à son flow dédié
// (AuthFlow / MainFlow / VendorFlow / AdminFlow / MiscFlow).
package com.tik_market.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.navigation.flows.AdminFlow
import com.tik_market.navigation.flows.AuthFlow
import com.tik_market.navigation.flows.MainFlow
import com.tik_market.navigation.flows.MiscFlow
import com.tik_market.navigation.flows.VendorFlow
import com.tik_market.ui.chat.openUrl
import com.tik_market.ui.components.AppBottomBar
import com.tik_market.ui.components.PollingManager
import com.tik_market.ui.misc.SplashScreen
import com.tik_market.utils.BackPressHandler
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainContent(appState: AppState, onExit: () -> Unit, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, isOnline: Boolean = true, userCity: String? = null) {
    var backPressedOnce by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

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
                scope.launch { snackbarHostState.showSnackbar(s.pressBackToExit) }
            }
        } else {
            appState.goBack()
        }
    }

    // Polling logic
    PollingManager(appState)

    val bottomItems = listOf(
        BottomNavItem(NavScreen.Home, Icons.Default.Home, Icons.Filled.Home, s.home),
        BottomNavItem(NavScreen.Reels, Icons.Default.Movie, Icons.Filled.Movie, "Reels"),
        BottomNavItem(NavScreen.Conversations, Icons.Default.Chat, Icons.Filled.Chat, s.messages),
        BottomNavItem(NavScreen.Cart, Icons.Default.ShoppingCart, Icons.Filled.ShoppingCart, s.cart),
        BottomNavItem(NavScreen.Profile, Icons.Default.Person, Icons.Filled.Person, s.account)
    )

    val hideBottomBar = appState.currentScreen in listOf(
        NavScreen.ProductDetail, NavScreen.Chat, NavScreen.Auth, NavScreen.Payment,
        NavScreen.ImageSearch, NavScreen.Checkout, NavScreen.AddProduct,
        NavScreen.Compare, NavScreen.VendorDashboard, NavScreen.ShopPage,
        NavScreen.StoryViewer, NavScreen.AdminDashboard, NavScreen.LiveShopping,
        NavScreen.LiveStreaming
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val showWhatsAppFab = appState.currentScreen in listOf(NavScreen.Home, NavScreen.Profile, NavScreen.Cart)
            if (showWhatsAppFab) {
                FloatingActionButton(
                    onClick = {
                        val phone = com.tik_market.utils.Constants.ASSISTANCE_PHONE_FULL
                        openUrl("https://wa.me/$phone?text=Bonjour%20TiK-Market,%20j'ai%20besoin%20d'aide.")
                    },
                    containerColor = Color(0xFF25D366),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = if (hideBottomBar) 0.dp else 16.dp)
                ) {
                    Icon(Icons.Default.SupportAgent, "WhatsApp Support")
                }
            }
        },
        bottomBar = {
            if (!hideBottomBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AppBottomBar(appState, bottomItems)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                        s.offlineBanner,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                AppNavigation(appState, scope, snackbarHostState, userCity)
            }
        }
    }
}


@Composable
fun AppNavigation(appState: AppState, scope: kotlinx.coroutines.CoroutineScope, snackbarHostState: SnackbarHostState, userCity: String? = null) {
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
        println("AppNavigation: showing screen ${screen.route}")
        when (screen) {
            NavScreen.Splash -> SplashScreen(onFinished = { appState.navigateTo(NavScreen.Home) })
            NavScreen.Auth, NavScreen.Terms, NavScreen.Legal -> AuthFlow(appState, scope, snackbarHostState)

            NavScreen.Home, NavScreen.ProductDetail, NavScreen.Cart, NavScreen.Profile,
            NavScreen.Chat, NavScreen.Conversations, NavScreen.Orders, NavScreen.Notifications,
            NavScreen.Checkout, NavScreen.Payment, NavScreen.Settings, NavScreen.Wishlist,
            NavScreen.ShopsList, NavScreen.ShopPage, NavScreen.ImageSearch, NavScreen.Compare,
            NavScreen.Loyalty, NavScreen.NotifPrefs, NavScreen.StoryViewer, NavScreen.MyGroupBuys,
            NavScreen.ShopsMap, NavScreen.EditProfile, NavScreen.LiveShopping,
            NavScreen.LiveStreaming, NavScreen.CreateReel, NavScreen.Reels -> MainFlow(appState, scope, snackbarHostState, userCity)

            NavScreen.VendorDashboard, NavScreen.ManageShop, NavScreen.AddProduct,
            NavScreen.VendorOrders, NavScreen.VendorGroupBuys, NavScreen.VendorSubscribers,
            NavScreen.CreateShop, NavScreen.FollowedShops -> VendorFlow(appState, scope, snackbarHostState)

            NavScreen.AdminDashboard -> AdminFlow(appState)

            NavScreen.Legal, NavScreen.Terms, NavScreen.About -> MiscFlow(appState)

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Écran non implémenté: ${screen.route} (v1.1)") }
        }
    }
}
