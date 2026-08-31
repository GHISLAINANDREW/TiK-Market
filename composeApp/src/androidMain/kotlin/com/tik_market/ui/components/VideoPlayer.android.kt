package com.tik_market.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tik_market.utils.UrlUtils

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    onEnded: () -> Unit
) {
    val safeUrl = remember(url) { UrlUtils.resolveSafeUrl(url) }
    var lastUrl by remember { mutableStateOf(safeUrl) }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(safeUrl))
                // Start only once the video is actually prepared, otherwise
                // start() is a no-op and the story appears stuck.
                setOnPreparedListener { mp ->
                    if (isPlaying && !mp.isPlaying) start()
                }
                setOnCompletionListener { onEnded() }
                // If loading/playback fails, advance to the next story instead
                // of freezing forever (the 5s timer is skipped for videos).
                setOnErrorListener { _, _, _ ->
                    onEnded()
                    true
                }
            }
        },
        modifier = modifier,
        update = { vv ->
            if (safeUrl != lastUrl) {
                lastUrl = safeUrl
                vv.setVideoURI(Uri.parse(safeUrl))
            }
            if (isPlaying) {
                if (!vv.isPlaying) vv.start()
            } else {
                if (vv.isPlaying) vv.pause()
            }
        }
    )
}
