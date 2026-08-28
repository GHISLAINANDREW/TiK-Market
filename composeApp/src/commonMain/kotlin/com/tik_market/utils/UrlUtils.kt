package com.tik_market.utils

import com.tik_market.api.ApiClient

object UrlUtils {
    private const val RENDER_BASE = "https://tik-market.onrender.com"
    private const val PROXY_BASE = "https://tik-market-proxy.gtankou.workers.dev"

    /**
     * Resolves a URL to an absolute path and applies proxying rules for Android
     * to bypass ISP blocks on Render and Cloudinary.
     */
    fun resolveSafeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        
        // 1. Convert to absolute URL if relative
        var absoluteUrl = if (!url.startsWith("http") && !url.startsWith("data:")) {
            val base = ApiClient.baseUrl.trimEnd('/')
            val cleanPath = url.trimStart('/', '\\').replace("\\", "/")
            "$base/$cleanPath"
        } else {
            url
        }

        // 2. Fix protocol for common development tunnels
        if (absoluteUrl.startsWith("http://") && (absoluteUrl.contains("loca.lt") || absoluteUrl.contains("ngrok") || absoluteUrl.contains("cloudflare"))) {
            absoluteUrl = absoluteUrl.replace("http://", "https://")
        }

        // 3. Apply Proxy for Render (bypass Orange Cameroon block)
        if (absoluteUrl.contains("onrender.com")) {
            absoluteUrl = absoluteUrl.replace(RENDER_BASE, PROXY_BASE)
        } 
        // 4. Apply Proxy for Cloudinary if needed
        else if (absoluteUrl.contains("res.cloudinary.com")) {
            absoluteUrl = "$PROXY_BASE/proxy?url=" + ApiClient.encodeUri(absoluteUrl)
        }

        return absoluteUrl
    }
}
