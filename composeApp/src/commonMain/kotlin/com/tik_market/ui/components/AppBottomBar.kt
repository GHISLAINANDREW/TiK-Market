package com.tik_market.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.navigation.AppState
import com.tik_market.navigation.BottomNavItem
import com.tik_market.navigation.NavScreen
import com.tik_market.theme.Orange

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
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
