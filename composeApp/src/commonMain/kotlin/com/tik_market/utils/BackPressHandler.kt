package com.tik_market.utils

import androidx.compose.runtime.Composable

/**
 * Intercept back press events.
 * - Android: intercepts system back button
 * - WasmJs: no-op
 */
@Composable
expect fun BackPressHandler(enabled: Boolean, onBack: () -> Unit)
