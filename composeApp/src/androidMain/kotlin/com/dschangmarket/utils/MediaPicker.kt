package com.dschangmarket.utils

import android.content.Intent
import android.provider.MediaStore

actual object MediaPicker {
    actual fun pickImageOrVideo(onResult: (String?, String?, Boolean) -> Unit) {
        // Mock-up for Android
        onResult("data:image/jpeg;base64,mockdata", "photo.jpg", false)
    }
}
