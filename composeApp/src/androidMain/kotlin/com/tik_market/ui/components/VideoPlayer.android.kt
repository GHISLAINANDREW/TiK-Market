package com.tik_market.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tik_market.cache.PersistentMediaCache
import com.tik_market.utils.UrlUtils
import kotlinx.coroutines.launch

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    onEnded: () -> Unit
) {
    val safeUrl = remember(url) { UrlUtils.resolveSafeUrl(url) }
    
    // Track the actual URI being played (could be remote then local)
    var currentUri by remember(safeUrl) { 
        mutableStateOf(PersistentMediaCache.getCachedPath(safeUrl) ?: safeUrl) 
    }
    
    // Background caching & dynamic URI update
    LaunchedEffect(safeUrl) {
        if (PersistentMediaCache.getCachedPath(safeUrl) == null) {
            PersistentMediaCache.cacheMedia(safeUrl)
            // Once cached, update the URI so the player can switch if needed
            PersistentMediaCache.getCachedPath(safeUrl)?.let { 
                currentUri = it 
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(currentUri))
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
            val uri = Uri.parse(currentUri)
            // ONLY update URI if it actually changed to avoid restart flicker
            if (vv.tag != currentUri) {
                vv.setVideoURI(uri)
                vv.tag = currentUri
            }
            
            if (isPlaying) {
                if (!vv.isPlaying) vv.start()
            } else {
                if (vv.isPlaying) vv.pause()
            }
        }
    )
}
