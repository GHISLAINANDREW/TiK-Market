package com.dschangmarket.api

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// ── JS interop: HTTP fetch via callbacks ────────────────────────

/**
 * Makes an HTTP request using fetch(), calls onSuccess(responseText) or onError(message).
 */
@JsFun("""(url, method, headersStr, body, onSuccess, onError) => {
    try {
        console.log('[HttpEngine]', method, url, body ? 'body:' + body.substring(0,80) : 'no body');
        const headers = headersStr ? JSON.parse(headersStr) : {};
        const init = { method: method, headers: headers };
        if (body != null) init.body = body;
        fetch(url, init)
            .then(r => {
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
                console.log('[HttpEngine] fetch failed:', e.message || String(e));
                onError(e.message ? e.message : String(e));
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
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
)

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

        return suspendCoroutine { cont ->
            nativeFetch(
                url, method, headersJson, body,
                onSuccess = { cont.resume(it) },
                onError = { cont.resumeWithException(Exception(it)) }
            )
        }
    }
}
