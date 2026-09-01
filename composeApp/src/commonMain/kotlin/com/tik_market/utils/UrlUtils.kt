package com.tik_market.utils

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

        // 3. Robust Proxy Logic
        
        // Strategy A: Direct path replacement for Render (Primary Backend)
        // This is THE MOST COMPATIBLE way for Android image decoders.
        // Render is often blocked by Cameroonian ISPs, so we route it through
        // the Cloudflare Worker reverse proxy (which serves /uploads, /stories, ...).
        if (absoluteUrl.contains("onrender.com")) {
            return absoluteUrl.replace(RENDER_BASE, PROXY_BASE)
        }

        // External CDNs (Unsplash, Cloudinary) are NOT blocked and send proper
        // CORS headers, so we load them DIRECTLY. The old Strategy B routed them
        // through /proxy?url= which does not exist on the Worker -> 404 -> broken
        // hero images on Android. Direct loading works on both web and Android.

        return absoluteUrl
    }
}
