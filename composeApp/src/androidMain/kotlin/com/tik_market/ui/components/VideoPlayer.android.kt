package com.tik_market.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tik_market.cache.PersistentMediaCache
import com.tik_market.utils.ConnectionQuality
import com.tik_market.utils.UrlUtils

/**
 * Appends Cloudinary quality params to the URL if it targets Cloudinary.
 * e.g. ?q_auto:low (poor), ?q_auto:good (good), no param (default).
 */
private fun adaptCloudinaryQuality(url: String, quality: ConnectionQuality): String {
    if (!url.contains("cloudinary.com")) return url
    val qualityParam = when (quality) {
        ConnectionQuality.GOOD    -> return url          // full quality
        ConnectionQuality.MEDIUM -> "q_auto:good"
        ConnectionQuality.POOR   -> "q_auto:low"
    }
    val separator = if (url.contains("?")) "&" else "?"
    return "$url${separator}$qualityParam"
}

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    quality: ConnectionQuality,
    onEnded: () -> Unit
) {
    val safeUrl = remember(url) { UrlUtils.resolveSafeUrl(url) }
    val adaptedUrl = remember(safeUrl, quality) { adaptCloudinaryQuality(safeUrl, quality) }
    
    // Determine initial URI: local if available, otherwise remote.
    // We stay on this URI for the lifetime of this Composable instance
    // to avoid reloads mid-play.
    val playUri = remember(adaptedUrl) { 
        PersistentMediaCache.getCachedPath(adaptedUrl) ?: adaptedUrl 
    }
    
    // Background caching for NEXT time. Runs on a global scope so the
    // download is NOT cancelled when this screen is disposed (otherwise
    // videos would re-download on every open).
    SideEffect {
        if (PersistentMediaCache.getCachedPath(adaptedUrl) == null) {
            PersistentMediaCache.cacheMediaAsync(adaptedUrl)
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(playUri))
                setOnPreparedListener { mp ->
                    mp.isLooping = false
                    if (isPlaying) start()
                }
                setOnCompletionListener { onEnded() }
                setOnErrorListener { _, _, _ ->
                    onEnded()
                    true
                }
            }
        },
        modifier = modifier,
        update = { vv ->
            if (isPlaying) {
                if (!vv.isPlaying) vv.start()
            } else {
                if (vv.isPlaying) vv.pause()
            }
        }
    )
}
