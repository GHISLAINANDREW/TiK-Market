package com.tik_market.ui.components

import com.tik_market.api.ApiClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.http.*

object ImageFetcher {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 6000
        }
    }

    suspend fun fetchBytes(url: String): ByteArray? {
        if (url.isBlank()) return null

        // Try primary URL first
        val primaryBytes = tryFetch(url)
        if (primaryBytes != null) return primaryBytes

        // If primary fails and url contains a domain from baseUrls, attempt fallback candidate domains
        if (url.contains("workers.dev") || url.contains("onrender.com")) {
            val uriWithoutScheme = url.removePrefix("https://").removePrefix("http://")
            val firstSlash = uriWithoutScheme.indexOf('/')
            val path = if (firstSlash >= 0) uriWithoutScheme.substring(firstSlash) else ""

            for (base in ApiClient.getBaseUrls()) {
                val candidateUrl = "${base.trimEnd('/')}$path"
                if (candidateUrl == url) continue
                val fallbackBytes = tryFetch(candidateUrl)
                if (fallbackBytes != null) return fallbackBytes
            }
        }

        return null
    }

    private suspend fun tryFetch(targetUrl: String): ByteArray? {
        return try {
            val response = client.get(targetUrl) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                header("Accept", "image/*,*/*")
            }
            if (response.status.isSuccess()) {
                response.readRawBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            println("[ImageFetcher] Error fetching $targetUrl: ${e.message}")
            null
        }
    }
}

