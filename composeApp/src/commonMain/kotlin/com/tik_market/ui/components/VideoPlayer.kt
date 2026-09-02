package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tik_market.utils.ConnectionQuality

/**
 * Cross-platform video player for story / reel videos.
 *
 * @param quality If the URL points to a Cloudinary-hosted video, this quality
 *   level is used to request an adaptive bitrate variant (q_auto).
 */
@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    quality: ConnectionQuality = ConnectionQuality.GOOD,
    onEnded: () -> Unit = {}
)
