package com.tik_market.ui.components

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
 * Adds Cloudinary image transformation for thumbnails/w proofiles.
 * Only applies to Cloudinary-hosted URLs.
 * Example: .../image/upload/v12345/file.jpg → .../image/upload/w_400,q_auto,f_auto/v12345/file.jpg
 */
fun optimizeImageUrl(url: String, width: Int = 400): String {
    if (url.contains("res.cloudinary.com") && url.contains("/image/upload/")) {
        return url.replace("/image/upload/", "/image/upload/w_$width,q_auto,f_auto/")
    }
    return url
}

/**
 * Convenience function: fetches a remote image URL and decodes it
 * directly to an ImageBitmap. Uses a global in-memory cache to avoid
 * re-downloading the same image. Automatically optimizes Cloudinary URLs
 * to w_400,q_auto for faster loading.
 */
suspend fun loadImageFromUrl(url: String): ImageBitmap? {
    val optimizedUrl = optimizeImageUrl(url)
    // Check cache first
    globalImageCache.get(optimizedUrl)?.let { return it }
    // Fetch and decode
    val dataUrl = fetchImageAsDataUrl(optimizedUrl) ?: return null
    val bitmap = decodeDataUrlToImageBitmap(dataUrl) ?: return null
    // Store in cache
    globalImageCache.put(optimizedUrl, bitmap)
    return bitmap
}
