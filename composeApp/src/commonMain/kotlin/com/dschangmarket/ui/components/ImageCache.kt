package com.dschangmarket.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Simple in-memory image cache that stores decoded ImageBitmaps keyed by URL.
 * Uses a fixed capacity with FIFO eviction when full.
 *
 * Implemented with arrays instead of LinkedHashMap for WasmJs compatibility.
 */
class ImageCache(private val maxSize: Int = 50) {
    private val keys = mutableListOf<String>()
    private val values = mutableListOf<ImageBitmap>()

    fun get(url: String): ImageBitmap? {
        val index = keys.indexOf(url)
        return if (index >= 0) values[index] else null
    }

    fun put(url: String, bitmap: ImageBitmap) {
        val index = keys.indexOf(url)
        if (index >= 0) {
            values[index] = bitmap
        } else {
            if (keys.size >= maxSize) {
                keys.removeAt(0)
                values.removeAt(0)
            }
            keys.add(url)
            values.add(bitmap)
        }
    }

    fun clear() {
        keys.clear()
        values.clear()
    }
}

/**
 * Remember a singleton ImageCache for the composition lifecycle.
 */
@Composable
fun rememberImageCache(maxSize: Int = 50): ImageCache = remember { ImageCache(maxSize) }
