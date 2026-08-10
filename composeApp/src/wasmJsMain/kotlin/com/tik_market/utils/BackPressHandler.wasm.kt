package com.tik_market.utils

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on web: browser handles back navigation
}
