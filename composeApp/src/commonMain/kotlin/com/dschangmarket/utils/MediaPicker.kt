package com.dschangmarket.utils

/**
 * Platform-specific media picker.
 * Opens the gallery/file selector for images and short videos.
 */
expect object MediaPicker {
    fun pickImageOrVideo(onResult: (dataUrl: String?, fileName: String?, isVideo: Boolean) -> Unit)
}
