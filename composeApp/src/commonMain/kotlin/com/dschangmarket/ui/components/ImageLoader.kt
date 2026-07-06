package com.dschangmarket.ui.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Fetches a remote image URL and returns it as a base64 data URL string,
 * or null on failure.
 *
 * - WasmJs: uses fetch() + FileReader
 * - Android: uses HttpURLConnection + Base64
 */
expect suspend fun fetchImageAsDataUrl(url: String): String?

/** Global image cache shared across the app. */
val globalImageCache = ImageCache(maxSize = 250)

/**
 * Convenience function: fetches a remote image URL and decodes it
 * directly to an ImageBitmap. Uses a global in-memory cache to avoid
 * re-downloading the same image.
 */
suspend fun loadImageFromUrl(url: String): ImageBitmap? {
    // Check cache first
    globalImageCache.get(url)?.let { return it }
    // Fetch and decode
    val dataUrl = fetchImageAsDataUrl(url) ?: return null
    val bitmap = decodeDataUrlToImageBitmap(dataUrl) ?: return null
    // Store in cache
    globalImageCache.put(url, bitmap)
    return bitmap
}
