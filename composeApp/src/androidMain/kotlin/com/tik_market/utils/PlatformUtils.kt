package com.tik_market.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.tik_market.AndroidChatContext

private val startupParams = mutableMapOf<String, String?>()

actual fun getStartupParameter(key: String): String? {
    if (startupParams.containsKey(key)) return startupParams[key]
    val activity = AndroidChatContext.currentActivity ?: return null
    val intent = activity.intent ?: return null
    
    // Check URL data (Deep Link)
    val data = intent.data?.getQueryParameter(key)
    if (data != null) return data
    
    // Check Extras (Notifications)
    val extra = intent.getStringExtra(key)
    if (extra != null) return extra
    
    val intExtra = intent.getIntExtra(key, -1)
    if (intExtra != -1) return intExtra.toString()
    
    return null
}

actual fun setStartupParameter(key: String, value: String?) {
    startupParams[key] = value
}

actual fun copyToClipboard(text: String) {
    val activity = AndroidChatContext.currentActivity ?: return
    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("TiK-Market", text)
    clipboard.setPrimaryClip(clip)
}

actual fun updateUnreadBadge(count: Int) {
    // No-op on Android; APK handles badge via native notifications
}

actual fun setupTabFocusRefresh(callback: () -> Unit) {
    // No-op on Android
}

actual fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit {
    val activity = AndroidChatContext.currentActivity ?: return {}
    val connectivityManager: ConnectivityManager? = try {
        activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (_: Exception) { null }
    if (connectivityManager == null) { onChange(true); return {} }

    // Run initial check safely
    try {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val initiallyOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        onChange(initiallyOnline)
    } catch (_: Exception) {
        onChange(true) // Assume online if check fails
    }

    // Register callback for future changes
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onChange(true)
        }

        override fun onLost(network: Network) {
            try {
                val currentNetwork = connectivityManager.activeNetwork
                val currentCaps = connectivityManager.getNetworkCapabilities(currentNetwork)
                if (currentCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true) {
                    onChange(false)
                }
            } catch (_: Exception) {
                onChange(false)
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            onChange(online)
        }
    }

    try {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    } catch (_: Exception) {
        return {} // Callback registration failed
    }

    return {
        try { connectivityManager.unregisterNetworkCallback(callback) } catch (_: Exception) {}
    }
}

actual fun downloadFile(url: String, filename: String) {
    // No-op on Android
}

actual fun installPwa() {
    // No-op on Android
}
