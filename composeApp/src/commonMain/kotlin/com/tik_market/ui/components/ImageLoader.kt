package com.tik_market.ui.components

import androidx.compose.ui.graphics.ImageBitmap

import com.tik_market.cache.PersistentMediaCache
import com.tik_market.utils.UrlUtils

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
 * Fetches a remote image URL and returns it as a base64 data URL string,
 * or null on failure.
 */
expect suspend fun fetchImageAsDataUrl(url: String): String?

/**
 * Fetches remote image bytes.
 */
expect suspend fun fetchImageBytes(url: String): ByteArray?

/**
 * Fetches a remote image URL and returns an ImageBitmap.
 */
expect suspend fun fetchImageAsBitmap(url: String): ImageBitmap?

/**
 * Convenience function: fetches a remote image URL and decodes it
 * directly to an ImageBitmap. Uses a global in-memory cache to avoid
 * re-downloading the same image. Automatically optimizes Cloudinary URLs
 * to w_400,q_auto for faster loading.
 */
suspend fun loadImageFromUrl(url: String): ImageBitmap? {
    if (url.isBlank()) return null
    
    val absoluteUrl = UrlUtils.resolveSafeUrl(url)
    val optimizedUrl = optimizeImageUrl(absoluteUrl)

    // 1. Check memory cache first
    globalImageCache.get(optimizedUrl)?.let { return it }

    // 2. Check persistent disk cache
    try {
        PersistentMediaCache.getCachedPath(optimizedUrl)?.let { cachedUrl ->
            val bitmap = fetchImageAsBitmap(cachedUrl)
            if (bitmap != null) {
                globalImageCache.put(optimizedUrl, bitmap)
                return bitmap
            }
        }
    } catch (_: Exception) {}

    // 3. Fetch from network via Ktor (modern & fast)
    val bytes = ImageFetcher.fetchBytes(optimizedUrl) ?: return null
    
    // 4. Decode to bitmap
    val bitmap = decodeBytesToBitmap(bytes) ?: return null
    
    // 5. Store in memory cache
    globalImageCache.put(optimizedUrl, bitmap)
    
    // 6. Immediate disk cache (SAVE THE BYTES WE JUST GOT)
    // No redundant network call anymore.
    if (optimizedUrl.contains("/stories/") || optimizedUrl.contains("/uploads/")) {
        PersistentMediaCache.saveMediaBytes(optimizedUrl, bytes)
    }

    return bitmap
}

/** Decodes a byte array to an ImageBitmap. */
expect fun decodeBytesToBitmap(bytes: ByteArray): ImageBitmap?
