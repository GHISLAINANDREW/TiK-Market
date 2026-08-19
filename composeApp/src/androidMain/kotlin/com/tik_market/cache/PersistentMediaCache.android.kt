package com.tik_market.cache

import com.tik_market.AndroidChatContext
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

actual object PersistentMediaCache {
    private val cacheDir: File? get() = AndroidChatContext.currentActivity?.cacheDir?.let { File(it, "stories_cache") }

    actual suspend fun cacheMedia(url: String) {
        val dir = cacheDir ?: return
        if (!dir.exists()) dir.mkdirs()

        val fileName = url.hashCode().toString()
        val file = File(dir, fileName)

        if (file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.DAYS.toMillis(1)) {
            return // Already cached and fresh
        }

        try {
            URL(url).openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            println("[Cache] Failed to cache $url: ${e.message}")
        }
    }

    actual fun getCachedPath(url: String): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, url.hashCode().toString())
        
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.DAYS.toMillis(1)) {
            return "file://${file.absolutePath}"
        }
        return null
    }

    actual fun cleanupExpired() {
        val dir = cacheDir ?: return
        if (!dir.exists()) return
        
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > TimeUnit.DAYS.toMillis(1)) {
                file.delete()
            }
        }
    }
}
