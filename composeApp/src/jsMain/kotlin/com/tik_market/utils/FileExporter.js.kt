package com.tik_market.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    return { _, _ -> false }
}
