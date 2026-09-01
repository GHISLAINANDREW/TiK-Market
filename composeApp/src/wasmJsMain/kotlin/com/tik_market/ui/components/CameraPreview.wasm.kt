package com.tik_market.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun CameraPreview(modifier: Modifier) {
    Box(modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
        Text("Caméra non supportée sur Web (Wasm)", color = Color.White)
    }
}
