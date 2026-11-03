package com.tik_market.api

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android HTTP engine using java.net.HttpURLConnection.
 * Optimized for mobile network reliability (Orange, MTN, cellular data)
 * with fast timeouts (6s connect, 15s read) to allow fast failover across base URLs.
 */
actual object HttpEngine {
    actual suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            connection = conn
            conn.requestMethod = method.uppercase()
            conn.connectTimeout = 6000  // 6s connect timeout for fast fallback on mobile networks (Orange/MTN)
            conn.readTimeout = 15000    // 15s read timeout
            conn.instanceFollowRedirects = true
            conn.useCaches = false
            conn.doInput = true

            // Set headers
            headers.forEach { (key, value) -> conn.setRequestProperty(key, value) }
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")

            if (!headers.containsKey("User-Agent")) {
                conn.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
            }

            // Write body for POST/PUT using raw UTF-8 bytes and fixed length mode
            if (body != null) {
                conn.doOutput = true
                val bodyBytes = body.toByteArray(Charsets.UTF_8)
                conn.setFixedLengthStreamingMode(bodyBytes.size)
                conn.outputStream.use { os ->
                    os.write(bodyBytes)
                    os.flush()
                }
            }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            conn.disconnect()

            if (responseCode in 200..299) {
                if (response.contains("Cloudflare Error", ignoreCase = true) ||
                    response.contains("No Workers script", ignoreCase = true) ||
                    (response.contains("\"status\":404") && response.contains("detail", ignoreCase = true)) ||
                    (response.contains("\"status\": 404") && response.contains("detail", ignoreCase = true))
                ) {
                    throw Exception("Cloudflare Worker inaccessible sur $url")
                }
                return@withContext response
            } else {
                try {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val element = json.parseToJsonElement(response)
                    val errorObj = element as? kotlinx.serialization.json.JsonObject
                    val msg = errorObj?.get("error")?.let {
                        if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                    } ?: errorObj?.get("message")?.let {
                        if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                    } ?: errorObj?.get("detail")?.let {
                        if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                    }
                    if (!msg.isNullOrBlank()) {
                        throw Exception(msg)
                    } else {
                        throw Exception("HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    if (e.message != null && !e.message!!.startsWith("HTTP") && !e.message!!.contains("Serializer") && !e.message!!.contains("Json")) {
                        throw e
                    }
                    if (response.contains("<!doctype html>", ignoreCase = true) || response.contains("<html>", ignoreCase = true)) {
                        throw Exception("Service temporairement indisponible (Erreur $responseCode)")
                    }
                    if (response.isNotBlank()) throw Exception(response.take(150))
                    else throw Exception("HTTP $responseCode")
                }
            }
        } catch (e: Exception) {
            try { connection?.disconnect() } catch (_: Exception) {}
            throw e
        }
    }
}

