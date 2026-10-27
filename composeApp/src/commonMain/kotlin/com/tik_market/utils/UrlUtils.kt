package com.tik_market.utils

object UrlUtils {
    private const val PROXY_BASE = "https://tik-market-proxy.gtankou.workers.dev"
    private const val RENDER_BASE = "https://tik-market.onrender.com"

    /**
     * Resolves a URL to an absolute path and applies proxying rules for Android
     * to bypass ISP blocks on Render and Cloudinary.
     */
    fun resolveSafeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("data:")) return url
        
        // 1. Resolve relative URLs against the Cloudflare proxy domain
        var absoluteUrl = if (!url.startsWith("http")) {
            val cleanPath = url.trimStart('/', '\\').replace("\\", "/")
            "$PROXY_BASE/$cleanPath"
        } else {
            url
        }

        // 2. Fix protocol for development tunnels
        if (absoluteUrl.startsWith("http://") && (absoluteUrl.contains("loca.lt") || absoluteUrl.contains("ngrok") || absoluteUrl.contains("cloudflare"))) {
            absoluteUrl = absoluteUrl.replace("http://", "https://")
        }

        // 3. Robust Proxy Logic: route all render.com requests through Cloudflare proxy
        if (absoluteUrl.contains("onrender.com")) {
            return absoluteUrl.replace(RENDER_BASE, PROXY_BASE)
        }

        return absoluteUrl
    }
}
