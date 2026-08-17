package com.tik_market.utils

import com.tik_market.ui.chat.pickFile

actual object MediaPicker {
    actual fun pickImageOrVideo(onResult: (dataUrl: String?, fileName: String?, isVideo: Boolean) -> Unit) {
        pickFile { dataUrl ->
            if (dataUrl != null) {
                val isVideo = dataUrl.startsWith("data:video/")
                onResult(dataUrl, "file", isVideo)
            } else {
                onResult(null, null, false)
            }
        }
    }
}
