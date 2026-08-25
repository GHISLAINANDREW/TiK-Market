package com.tik_market.ui.components

import androidx.compose.runtime.*
import com.tik_market.api.ApiClient
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.utils.NotificationUtils
import com.tik_market.utils.updateUnreadBadge
import kotlinx.coroutines.delay

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
