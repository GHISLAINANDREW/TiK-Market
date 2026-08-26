package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MapView(
    modifier: Modifier,
    lat: Double,
    lng: Double,
    title: String,
    zoom: Float = 15f
)
