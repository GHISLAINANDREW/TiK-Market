package com.tik_market.cache

import com.tik_market.AndroidChatContext
import com.tik_market.utils.UrlUtils
import java.io.File
import java.net.HttpURLConnection
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
            val safeUrl = UrlUtils.resolveSafeUrl(url)
            val connection = URL(safeUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            
            if (connection.responseCode == 200) {
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                println("[Cache] HTTP ${connection.responseCode} for $safeUrl")
            }
            connection.disconnect()
        } catch (e: Exception) {
            println("[Cache] Failed to cache $url: ${e.message}")
        }
    }

    actual fun saveMediaBytes(url: String, bytes: ByteArray) {
        val dir = cacheDir ?: return
        if (!dir.exists()) dir.mkdirs()
        
        try {
            val file = File(dir, url.hashCode().toString())
            file.writeBytes(bytes)
        } catch (e: Exception) {
            println("[Cache] Failed to save bytes for $url: ${e.message}")
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

    actual fun clearAll() {
        val dir = cacheDir ?: return
        if (!dir.exists()) return
        dir.listFiles()?.forEach { it.delete() }
    }
}
