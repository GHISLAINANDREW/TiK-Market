// TiK-Market App - Version 1.0.2 (JDK 17 fix)
// Refactoring : ce fichier ne contient plus que la racine de l'application
// (fonction App, thème, restauration de session, détection de ville).
// - Le scaffold et la table de routage : navigation/AppNavigation.kt
// - Les écrans par flux : navigation/flows/{Auth,Main,Vendor,Admin,Misc}Flow.kt
// - Composants transverses : ui/components/{AppBottomBar,PollingManager,...}.kt
package com.tik_market

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.tik_market.api.*
import com.tik_market.api.dto.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import com.tik_market.theme.*
import com.tik_market.navigation.AppState
import com.tik_market.navigation.MainContent
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.onboarding.OnboardingManager
import com.tik_market.ui.onboarding.OnboardingScreen
import com.tik_market.utils.NotificationUtils
import com.tik_market.utils.observeConnectivity
import com.tik_market.utils.updateUnreadBadge

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

    LaunchedEffect(appState.isLoggedIn) {
        if (appState.isLoggedIn) {
            com.tik_market.api.ChatWebSocketClient.connect()
        } else {
            com.tik_market.api.ChatWebSocketClient.disconnect()
        }
    }
    
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
        CompositionLocalProvider(
            com.tik_market.utils.LocalAppStrings provides appState.strings
        ) {
            // ── Root container to ensure full screen ──
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
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
                    val connectivityStrings = com.tik_market.utils.LocalAppStrings.current
                    LaunchedEffect(showOfflineSnackbar) {
                        if (showOfflineSnackbar) {
                            val msg = if (isOnline) connectivityStrings.connectionRestored else connectivityStrings.connectionLost
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                            showOfflineSnackbar = false
                        }
                    }

                    // Restore session in background — does NOT block UI
                    val sessionStrings = com.tik_market.utils.LocalAppStrings.current
                    LaunchedEffect(Unit) {
                        // Cleanup expired media (stories older than 24h)
                        try {
                            com.tik_market.cache.PersistentMediaCache.cleanupExpired()
                        } catch (_: Exception) {}

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
                                scope.launch { snackbarHostState.showSnackbar(sessionStrings.sessionExpired) }
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

                    // Show MainContent
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
    }
}
