package com.tik_market.ui.components

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import io.ktor.http.*

object ImageFetcher {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 15000
        }
    }

    suspend fun fetchBytes(url: String): ByteArray? {
        return try {
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                header("Accept", "image/*,*/*")
            }
            if (response.status.isSuccess()) {
                response.readRawBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            println("[ImageFetcher] Error fetching $url: ${e.message}")
            null
        }
    }
}
