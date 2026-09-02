package com.tik_market.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event

import com.tik_market.utils.ConnectionQuality

private fun adaptCloudinaryQuality(url: String, quality: ConnectionQuality): String {
    if (!url.contains("cloudinary.com")) return url
    val qualityParam = when (quality) {
        ConnectionQuality.GOOD    -> return url
        ConnectionQuality.MEDIUM -> "q_auto:good"
        ConnectionQuality.POOR   -> "q_auto:low"
    }
    val separator = if (url.contains("?")) "&" else "?"
    return "$url${separator}$qualityParam"
}

private var videoIdCounter = 0

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isPlaying: Boolean,
    quality: ConnectionQuality,
    onEnded: () -> Unit
) {
    val videoId = remember { "story-video-js-${++videoIdCounter}" }
    val adaptedUrl = remember(url, quality) { adaptCloudinaryQuality(url, quality) }

    DisposableEffect(adaptedUrl) {
        val video = document.createElement("video") as HTMLVideoElement
        video.id = videoId
        video.src = adaptedUrl
        val style = video.style
        style.setProperty("position", "fixed")
        style.setProperty("top", "0")
        style.setProperty("left", "0")
        style.setProperty("width", "100%")
        style.setProperty("height", "100%")
        style.setProperty("object-fit", "contain")
        style.setProperty("background-color", "black")
        style.setProperty("z-index", "9999")
        
        video.muted = false
        video.autoplay = true
        video.setAttribute("playsinline", "true")
        video.controls = false
        video.loop = false

        val onEndedHandler: (Event) -> Unit = { onEnded() }
        video.addEventListener("ended", onEndedHandler)

        document.body?.appendChild(video)

        js("""
        video.play().catch(function(error) {
            video.muted = true;
            video.play();
        });
        """)

        onDispose {
            video.removeEventListener("ended", onEndedHandler)
            video.pause()
            video.removeAttribute("src")
            video.load()
            document.body?.removeChild(video)
        }
    }

    LaunchedEffect(isPlaying) {
        val video = document.getElementById(videoId) as? HTMLVideoElement
        if (video != null) {
            if (isPlaying) {
                js("video.play().catch(function(){});")
            } else {
                video.pause()
            }
        }
    }
}
