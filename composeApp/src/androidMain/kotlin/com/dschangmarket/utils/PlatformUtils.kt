package com.dschangmarket.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.dschangmarket.AndroidChatContext

actual fun getStartupParameter(key: String): String? {
    val activity = AndroidChatContext.currentActivity ?: return null
    val data = activity.intent?.data ?: return null
    return data.getQueryParameter(key)
}

actual fun copyToClipboard(text: String) {
    val activity = AndroidChatContext.currentActivity ?: return
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("DschangMarket", text)
    clipboard.setPrimaryClip(clip)
}
