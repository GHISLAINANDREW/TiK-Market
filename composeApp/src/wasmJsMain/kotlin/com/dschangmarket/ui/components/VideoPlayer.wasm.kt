package com.dschangmarket.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event

private var videoIdCounter = 0

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    onEnded: () -> Unit
) {
    val videoId = remember { "story-video-${++videoIdCounter}" }

    DisposableEffect(url) {
        val video = document.createElement("video") as HTMLVideoElement
        video.id = videoId
        video.src = url
        video.style.apply {
            position = "fixed"
            top = "0"
            left = "0"
            width = "100%"
            height = "100%"
            objectFit = "contain"
            backgroundColor = "black"
            zIndex = "9999"
        }
        video.muted = false
        video.autoplay = true
        video.playsInline = true
        video.controls = false
        video.loop = false

        // Handle video end
        val onEndedHandler: (Event) -> Unit = { onEnded() }
        video.addEventListener("ended", onEndedHandler)

        document.body?.appendChild(video)

        // Try playing the video (returns a Promise that we safely ignore)
        try {
            video.play()
        } catch (_: Exception) { }

        onDispose {
            video.removeEventListener("ended", onEndedHandler)
            video.pause()
            video.removeAttribute("src")
            video.load()
            document.body?.removeChild(video)
        }
    }

    // Update playback state
    LaunchedEffect(isPlaying) {
        val video = document.getElementById(videoId) as? HTMLVideoElement
        if (video != null) {
            if (isPlaying) {
                try { video.play() } catch (_: Exception) { }
            } else {
                video.pause()
            }
        }
    }
}
