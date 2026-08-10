package com.tik_market.utils

import android.content.Intent
import com.tik_market.AndroidChatContext

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
