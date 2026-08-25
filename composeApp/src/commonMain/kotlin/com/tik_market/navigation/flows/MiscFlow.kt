package com.tik_market.navigation.flows

import androidx.compose.runtime.Composable
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.misc.AboutScreen
import com.tik_market.ui.misc.LegalNoticeScreen
import com.tik_market.ui.misc.TermsOfUseScreen

@Composable
fun MiscFlow(
    appState: AppState
) {
    when (appState.currentScreen) {
        NavScreen.Legal -> LegalNoticeScreen(onBack = { appState.goBack() })
        NavScreen.Terms -> TermsOfUseScreen(onBack = { appState.goBack() })
        NavScreen.About -> AboutScreen(onBack = { appState.goBack() })
        else -> {}
    }
}
