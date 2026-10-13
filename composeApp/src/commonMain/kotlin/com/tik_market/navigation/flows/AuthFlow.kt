package com.tik_market.navigation.flows

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.tik_market.api.ApiClient
import com.tik_market.api.*
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.auth.AuthScreen
import com.tik_market.ui.auth.WelcomeRoleScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AuthFlow(
    appState: AppState,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    when (appState.currentScreen) {
        NavScreen.WelcomeRole -> WelcomeRoleScreen(
            onBuyerClick = {
                appState.isRegisterMode = true
                appState.navigateTo(NavScreen.Auth)
            },
            onVendorClick = {
                appState.isRegisterMode = true
                appState.navigateTo(NavScreen.CreateShop)
            },
            onLoginClick = {
                appState.isRegisterMode = false
                appState.navigateTo(NavScreen.Auth)
            }
        )
        NavScreen.Auth -> AuthScreen(
            onBack = { appState.goBack(); appState.isRegisterMode = false },
            initialModeRegister = appState.isRegisterMode,
            onTermsClick = { appState.navigateTo(NavScreen.Terms) },
            onLegalClick = { appState.navigateTo(NavScreen.Legal) },
            onGoToWelcome = { appState.navigateTo(NavScreen.WelcomeRole) },
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
                            appState.updateWallet(w)
                        }
                    } catch (_: Exception) {}

                    appState.goHome()
                }
            }
        )
        else -> {}
    }
}
