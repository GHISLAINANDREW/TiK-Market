package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cross-platform video player for story videos.
 */
@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    onEnded: () -> Unit = {}
)
