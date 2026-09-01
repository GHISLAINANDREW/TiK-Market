package com.tik_market.cache

import com.tik_market.AndroidChatContext
import com.tik_market.utils.UrlUtils
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual object PersistentMediaCache {
    private val cacheDir: File? get() = AndroidChatContext.currentActivity?.cacheDir?.let { File(it, "stories_cache") }
    private val activeDownloads = ConcurrentHashMap<String, Boolean>()

    // Global scope: downloads survive screen navigation (LaunchedEffect would
    // cancel the download when the composable is disposed -> videos re-download
    // every time the user opens a reel/story/hero).
    private val scope = CoroutineScope(Dispatchers.IO)

    /** Fire-and-forget download that keeps running after the UI leaves the screen. */
    fun cacheMediaAsync(url: String) {
        if (url.isBlank()) return
        scope.launch { cacheMedia(url) }
    }

    private fun getFileName(url: String): String {
        return url.replace("[^a-zA-Z0-9]".toRegex(), "_").takeLast(100) + "_" + url.hashCode()
    }

    actual suspend fun cacheMedia(url: String) {
        if (url.isBlank()) return
        val dir = cacheDir ?: return
        if (!dir.exists()) dir.mkdirs()

        val fileName = getFileName(url)
        val file = File(dir, fileName)

        if (file.exists() && System.currentTimeMillis() - file.lastModified() < TimeUnit.DAYS.toMillis(1)) {
            return // Already cached and fresh
        }

        if (activeDownloads.containsKey(url)) return // Already being downloaded

        activeDownloads[url] = true
        try {
            withContext(Dispatchers.IO) {
                val safeUrl = UrlUtils.resolveSafeUrl(url)
                val connection = URL(safeUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

                if (connection.responseCode == 200) {
                    val tempFile = File(dir, "$fileName.tmp")
                    connection.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (file.exists()) file.delete()
                        val ok = tempFile.renameTo(file)
                        if (ok) {
                            println("[Cache] Successfully cached $url to ${file.absolutePath}")
                        } else {
                            println("[Cache] Failed to rename temp file for $url")
                        }
                    }
                } else {
                    println("[Cache] HTTP ${connection.responseCode} for $safeUrl")
                }
                connection.disconnect()
            }
        } catch (e: Exception) {
            println("[Cache] Failed to cache $url: ${e.message}")
        } finally {
            activeDownloads.remove(url)
        }
    }

    actual fun saveMediaBytes(url: String, bytes: ByteArray) {
        val dir = cacheDir ?: return
        if (!dir.exists()) dir.mkdirs()
        
        try {
            val file = File(dir, getFileName(url))
            file.writeBytes(bytes)
        } catch (e: Exception) {
            println("[Cache] Failed to save bytes for $url: ${e.message}")
        }
    }

    actual fun getCachedPath(url: String): String? {
        val dir = cacheDir ?: return null
        val file = File(dir, getFileName(url))
        
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
