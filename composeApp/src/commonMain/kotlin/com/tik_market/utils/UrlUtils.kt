package com.tik_market.utils

import com.tik_market.api.ApiClient

object UrlUtils {

    /**
     * Resolves a URL to an absolute path using the currently working ApiClient base domain.
     * Bypasses ISP blocks (such as Orange Cameroun blocking onrender.com or workers.dev subdomains)
     * by routing media requests through whichever base URL is active and functional.
     */
    fun resolveSafeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        if (url.startsWith("data:")) return url

        val activeBase = ApiClient.baseUrl.trimEnd('/')

        // 1. Resolve relative URLs against active base domain
        var absoluteUrl = if (!url.startsWith("http")) {
            val cleanPath = url.trimStart('/', '\\').replace("\\", "/")
            "$activeBase/$cleanPath"
        } else {
            url
        }

        // 2. Fix protocol for development tunnels
        if (absoluteUrl.startsWith("http://") && (absoluteUrl.contains("loca.lt") || absoluteUrl.contains("ngrok") || absoluteUrl.contains("cloudflare"))) {
            absoluteUrl = absoluteUrl.replace("http://", "https://")
        }

        // 3. Dynamic domain swap: if URL uses an backend host, swap it to activeBase
        if (absoluteUrl.contains("onrender.com") || absoluteUrl.contains("workers.dev")) {
            val uriWithoutScheme = absoluteUrl.removePrefix("https://").removePrefix("http://")
            val firstSlash = uriWithoutScheme.indexOf('/')
            val path = if (firstSlash >= 0) uriWithoutScheme.substring(firstSlash) else ""
            return "$activeBase$path"
        }

        return absoluteUrl
    }
}

