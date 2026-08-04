package com.dschangmarket.api

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// ── JS interop: HTTP fetch via callbacks ────────────────────────

/**
 * Makes an HTTP request using fetch() with a timeout (AbortController),
 * calls onSuccess(responseText) or onError(message).
 */
@JsFun("""(url, method, headersStr, body, timeoutMs, onSuccess, onError) => {
    try {
        console.log('[HttpEngine]', method, url, body ? 'body:' + body.substring(0,80) : 'no body');
        const headers = headersStr ? JSON.parse(headersStr) : {};
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        const init = { method: method, headers: headers, signal: controller.signal };
        if (body != null) init.body = body;
        fetch(url, init)
            .then(r => {
                clearTimeout(timer);
                console.log('[HttpEngine] response status:', r.status, r.statusText);
                return r.text();
            })
            .then(t => {
                console.log('[HttpEngine] response body:', t.substring(0, 200));
                try {
                    const obj = JSON.parse(t);
                    if (obj && obj.error) {
                        console.log('[HttpEngine] server error:', obj.error);
                        onError(String(obj.error));
                        return;
                    }
                } catch (e) {
                    // not JSON, just return text
                }
                onSuccess(t);
            })
            .catch(e => {
                clearTimeout(timer);
                const msg = (e && e.name === 'AbortError') ? 'Timeout: ' + timeoutMs + 'ms' : (e.message || String(e));
                console.log('[HttpEngine] fetch failed:', msg);
                onError(msg);
            });
    } catch (e) {
        console.log('[HttpEngine] exception:', e.message || String(e));
        onError(e.message ? e.message : String(e));
    }
}""")
private external fun nativeFetch(
    url: String,
    method: String,
    headersStr: String?,
    body: String?,
    timeoutMs: Int,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
)

/** Timeout for a single HTTP request, in milliseconds. */
private const val REQUEST_TIMEOUT_MS = 20000

/** Number of attempts (including the first) before giving up. */
private const val MAX_ATTEMPTS = 2

/** Delay between retries, in milliseconds. */
private const val RETRY_DELAY_MS = 800L

// ── HTTP Engine ─────────────────────────────────────────────────

actual object HttpEngine {
    actual suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): String {
        val headersJson = if (headers.isEmpty()) null else {
            headers.entries.joinToString(",") { (k, v) ->
                "\"${k.replace("\"", "\\\"")}\":\"${v.replace("\"", "\\\"")}\""
            }.let { "{$it}" }
        }

        var lastError: Exception? = null
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                return suspendCoroutine { cont ->
                    nativeFetch(
                        url, method, headersJson, body, REQUEST_TIMEOUT_MS,
                        onSuccess = { cont.resume(it) },
                        onError = { cont.resumeWithException(Exception(it)) }
                    )
                }
            } catch (e: Exception) {
                lastError = e
                // Do not retry server errors (4xx/5xx have already been parsed as onError),
                // only retry transport failures (timeout, network, CORS...).
                if (attempt < MAX_ATTEMPTS && isRetryable(e)) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: Exception("Network error")
    }

    private fun isRetryable(e: Exception): Boolean {
        val msg = e.message ?: return true
        val lower = msg.lowercase()
        // Timeout and network-level failures are retryable; HTTP-level errors are not.
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
