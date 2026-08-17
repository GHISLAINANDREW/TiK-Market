package com.tik_market.utils

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    // Non applicable on Web browser, but must have an actual implementation
}
