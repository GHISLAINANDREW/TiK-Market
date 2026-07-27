package com.dschangmarket.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    onEnded: () -> Unit
) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(url))
                setOnCompletionListener { onEnded() }
                start()
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
