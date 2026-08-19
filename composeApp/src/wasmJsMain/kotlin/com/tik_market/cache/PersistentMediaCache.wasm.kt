package com.tik_market.cache

actual object PersistentMediaCache {
    actual suspend fun cacheMedia(url: String) {
        // Browser handles caching automatically via fetch
    }

    actual fun getCachedPath(url: String): String? {
        // Return null to use original URL
        return null
    }

    actual fun cleanupExpired() {
        // Browser handles cache cleanup
    }
}
