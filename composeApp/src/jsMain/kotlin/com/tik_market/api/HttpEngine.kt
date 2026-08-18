package com.tik_market.api

import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val REQUEST_TIMEOUT_MS = 25000
private const val MAX_ATTEMPTS = 2
private const val RETRY_DELAY_MS = 1000L

actual object HttpEngine {
    actual suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): String {
        var lastError: Exception? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                return performFetch(url, method, headers, body)
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_ATTEMPTS && isRetryable(e)) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: Exception("Network error")
    }

    private suspend fun performFetch(url: String, method: String, headers: Map<String, String>, body: String?): String = suspendCoroutine { cont ->
        val controller = window.asDynamic().eval("new AbortController()")
        val timeoutId = window.setTimeout({ controller.abort() }, REQUEST_TIMEOUT_MS)

        val init = js("{}")
        init["method"] = method
        val jsHeaders = js("{}")
        headers.forEach { (k, v) -> jsHeaders[k] = v }
        init["headers"] = jsHeaders
        init["signal"] = controller.signal
        if (body != null) init["body"] = body

        window.fetch(url, init.unsafeCast<org.w3c.fetch.RequestInit>()).then({ response ->
            window.clearTimeout(timeoutId)
            response.text().then({ text ->
                if (response.ok) {
                    cont.resume(text)
                } else {
                    try {
                        val obj = JSON.parse<dynamic>(text)
                        if (obj != null && obj.error != undefined) {
                            cont.resumeWithException(Exception(obj.error.toString()))
                        } else {
                            cont.resumeWithException(Exception("HTTP ${response.status}: ${response.statusText}"))
                        }
                    } catch (e: Exception) {
                        cont.resumeWithException(Exception("HTTP ${response.status}: ${response.statusText}"))
                    }
                }
            }, { err ->
                window.clearTimeout(timeoutId)
                cont.resumeWithException(Exception(err.toString()))
            })
        }, { err ->
            window.clearTimeout(timeoutId)
            val errName = err.asDynamic()?.name ?: ""
            val msg = if (errName == "AbortError") "Timeout: ${REQUEST_TIMEOUT_MS}ms" else err.toString()
            cont.resumeWithException(Exception(msg))
        })
    }

    private fun isRetryable(e: Exception): Boolean {
        val msg = e.message ?: return true
        val lower = msg.lowercase()
        return msg.startsWith("Timeout") ||
            lower.contains("network") ||
            lower.contains("fetch") ||
            lower.contains("failed") ||
            lower.contains("load failed") ||
            lower.contains("cors") ||
            lower.contains("abort") ||
            lower.contains("timeout")
    }
}
