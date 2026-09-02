package com.tik_market.cache

actual object PersistentMediaCache {
    actual suspend fun cacheMedia(url: String) {
        // Browser handles caching automatically via fetch/cache headers
        // For web, we usually rely on Service Worker or simple browser cache.
    }

    actual fun saveMediaBytes(url: String, bytes: ByteArray) {
        // Browser handles caching automatically via fetch
    }

    actual fun getCachedPath(url: String): String? {
        // Return null to use original URL, browser will use its cache if available
        return null
    }

    actual fun cleanupExpired() {
        // Browser handles cache cleanup automatically
    }

    actual fun clearAll() {
        // Browser handles cache cleanup
    }
}
