package com.dschangmarket.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

actual fun updateUnreadBadge(count: Int) {
    // No-op on Android; APK handles badge via native notifications
}

actual fun setupTabFocusRefresh(callback: () -> Unit) {
    // No-op on Android
}

actual fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit {
    val activity = AndroidChatContext.currentActivity ?: return {}

    val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return {}

    // Run initial check
    val activeNetwork = connectivityManager.activeNetwork
    val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
    val initiallyOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    onChange(initiallyOnline)

    // Register callback for future changes
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onChange(true)
        }

        override fun onLost(network: Network) {
            // Check if there are any other available networks
            val currentNetwork = connectivityManager.activeNetwork
            val currentCaps = connectivityManager.getNetworkCapabilities(currentNetwork)
            if (currentCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true) {
                onChange(false)
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val online = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            onChange(online)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)

    return {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}

actual fun downloadFile(url: String, filename: String) {
    // No-op on Android
}
