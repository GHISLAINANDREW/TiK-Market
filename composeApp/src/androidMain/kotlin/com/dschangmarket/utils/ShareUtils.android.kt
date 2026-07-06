package com.dschangmarket.utils

import android.content.Intent
import com.dschangmarket.AndroidChatContext

actual fun shareText(text: String, title: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AndroidChatContext.currentActivity?.startActivity(chooser)
    } catch (_: Exception) {}
}
