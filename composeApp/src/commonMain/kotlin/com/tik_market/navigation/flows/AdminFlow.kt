package com.tik_market.navigation.flows

import androidx.compose.runtime.Composable
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.admin.AdminDashboardScreen

@Composable
fun AdminFlow(
    appState: AppState
) {
    when (appState.currentScreen) {
        NavScreen.AdminDashboard -> AdminDashboardScreen(onBack = { appState.goBack() })
        else -> {}
    }
}
