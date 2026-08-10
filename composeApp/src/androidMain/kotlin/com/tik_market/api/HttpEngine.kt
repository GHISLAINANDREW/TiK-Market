package com.tik_market.api

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android HTTP engine using java.net.HttpURLConnection.
 */
actual object HttpEngine {
    actual suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method.uppercase()
        // Render free tier "Cold Start" can take up to 30 seconds.
        // We must wait enough on the first attempt before falling back.
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.doInput = true

        // Set headers
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("X-Requested-With", "XMLHttpRequest")
        
        if (!headers.containsKey("User-Agent")) {
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        }

        // Write body for POST/PUT
        if (body != null) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in (200..299)) connection.inputStream else connection.errorStream
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        val response = reader.readText()
        reader.close()
        connection.disconnect()

        if (responseCode in 200..299) {
            response
        } else {
            // Try to extract error message from JSON response
            try {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val errorObj = json.decodeFromString<Map<String, String>>(response)
                throw Exception(errorObj["error"] ?: "HTTP $responseCode")
            } catch (_: Exception) {
                if (response.isNotBlank()) throw Exception(response)
                else throw Exception("HTTP $responseCode")
            }
        }
    }
}
