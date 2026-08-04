package com.dschangmarket.api

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
        connection.connectTimeout = 20000
        // 60s read timeout: media uploads (base64 images/videos) can be large
        // and slow on mobile networks (Orange etc.), 15s caused silent failures.
        connection.readTimeout = 60000
        connection.doInput = true

        // Set headers
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }

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
