package com.tik_market.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun MapView(
    modifier: Modifier,
    lat: Double,
    lng: Double,
    title: String,
    zoom: Float
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                val url = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}
