package com.tik_market.utils

import androidx.compose.runtime.Composable

/**
 * Saves a document file directly into the device's public Downloads directory.
 */
@Composable
expect fun rememberFileExporter(): (filename: String, content: String) -> Boolean
