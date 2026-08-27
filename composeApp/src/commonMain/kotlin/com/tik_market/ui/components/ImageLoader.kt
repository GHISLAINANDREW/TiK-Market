package com.tik_market.ui.components

import androidx.compose.ui.graphics.ImageBitmap

import com.tik_market.cache.PersistentMediaCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    if (url.isBlank()) return null
    
    // Auto-fix relative URLs and cleaning
    val absoluteUrl = if (!url.startsWith("http") && !url.startsWith("data:")) {
        val base = com.tik_market.api.ApiClient.baseUrl.trimEnd('/')
        val cleanPath = url.trimStart('/', '\\').replace("\\", "/")
        "$base/$cleanPath"
    } else {
        url
    }

    val optimizedUrl = optimizeImageUrl(absoluteUrl)

    // Check memory cache first
    globalImageCache.get(optimizedUrl)?.let { return it }

    // Check persistent cache
    PersistentMediaCache.getCachedPath(optimizedUrl)?.let { cachedUrl ->
        val dataUrl = fetchImageAsDataUrl(cachedUrl)
        if (dataUrl != null) {
            val bitmap = decodeDataUrlToImageBitmap(dataUrl)
            if (bitmap != null) {
                globalImageCache.put(optimizedUrl, bitmap)
                return bitmap
            }
        }
    }

    // For stories, try to cache locally (especially on Android)
    if (optimizedUrl.contains("/stories/")) {
        try {
            PersistentMediaCache.cacheMedia(optimizedUrl)
        } catch (_: Exception) {}
    }

    // Fetch and decode
    val dataUrl = fetchImageAsDataUrl(optimizedUrl) ?: return null
    val bitmap = decodeDataUrlToImageBitmap(dataUrl) ?: return null
    // Store in cache
    globalImageCache.put(optimizedUrl, bitmap)
    return bitmap
}
