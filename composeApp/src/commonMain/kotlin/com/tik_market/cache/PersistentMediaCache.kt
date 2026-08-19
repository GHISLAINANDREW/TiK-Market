package com.tik_market.cache

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Handles persistent storage of media files (stories) for 24 hours.
 */
expect object PersistentMediaCache {
    /** Saves a media file from a URL to local storage. */
    suspend fun cacheMedia(url: String)

    /** Returns a local path/URL if the media is cached and not expired. */
    fun getCachedPath(url: String): String?

    /** Cleans up expired media (older than 24h). */
    fun cleanupExpired()
}
