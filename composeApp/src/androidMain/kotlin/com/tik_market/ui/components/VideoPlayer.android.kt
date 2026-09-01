package com.tik_market.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tik_market.cache.PersistentMediaCache
import com.tik_market.utils.UrlUtils

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    onEnded: () -> Unit
) {
    val safeUrl = remember(url) { UrlUtils.resolveSafeUrl(url) }
    
    // Determine initial URI: local if available, otherwise remote.
    // We stay on this URI for the lifetime of this Composable instance
    // to avoid reloads mid-play.
    val playUri = remember(safeUrl) { 
        PersistentMediaCache.getCachedPath(safeUrl) ?: safeUrl 
    }
    
    // Background caching for NEXT time. Runs on a global scope so the
    // download is NOT cancelled when this screen is disposed (otherwise
    // videos would re-download on every open).
    SideEffect {
        if (PersistentMediaCache.getCachedPath(safeUrl) == null) {
            PersistentMediaCache.cacheMediaAsync(safeUrl)
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
