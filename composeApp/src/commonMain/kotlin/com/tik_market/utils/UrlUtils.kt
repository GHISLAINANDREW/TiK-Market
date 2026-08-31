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
        if (url.startsWith("data:")) return url
        
        // 1. Resolve relative URLs against the primary domain
        var absoluteUrl = if (!url.startsWith("http")) {
            val cleanPath = url.trimStart('/', '\\').replace("\\", "/")
            "$RENDER_BASE/$cleanPath"
        } else {
            url
        }

        // 2. Fix protocol for development tunnels
        if (absoluteUrl.startsWith("http://") && (absoluteUrl.contains("loca.lt") || absoluteUrl.contains("ngrok") || absoluteUrl.contains("cloudflare"))) {
            absoluteUrl = absoluteUrl.replace("http://", "https://")
        }

        // 3. Proxy logic
        
        // Strategy A: Direct path replacement for Render (Primary Backend)
        // Matches ApiClient logic: very fast, no encoding overhead.
        if (absoluteUrl.startsWith(RENDER_BASE)) {
            return absoluteUrl.replace(RENDER_BASE, PROXY_BASE)
        }

        // Strategy B: Proxy query parameter for external domains
        // Required for domains with strict CORS or potentially blocked (Unsplash, Cloudinary)
        if (absoluteUrl.contains("res.cloudinary.com") || absoluteUrl.contains("images.unsplash.com")) {
            // Avoid double proxying
            if (absoluteUrl.contains("$PROXY_BASE/proxy?url=")) return absoluteUrl
            
            // Optimize Unsplash for mobile if no params
            var optimized = absoluteUrl
            if (optimized.contains("images.unsplash.com") && !optimized.contains("?")) {
                optimized += "?w=800&q=80"
            }
            
            return "$PROXY_BASE/proxy?url=" + ApiClient.encodeUri(optimized)
        }

        return absoluteUrl
    }
}
