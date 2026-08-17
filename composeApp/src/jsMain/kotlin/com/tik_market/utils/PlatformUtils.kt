package com.tik_market.utils

import kotlinx.browser.window
import kotlinx.browser.document

private val startupParams = mutableMapOf<String, String?>()

actual fun getStartupParameter(key: String): String? {
    if (startupParams.containsKey(key)) return startupParams[key]
    val search = window.location.search
    if (search.isBlank()) return null
    val params = search.substring(1).split("&")
    for (p in params) {
        val kv = p.split("=")
        if (kv.size == 2 && kv[0] == key) return kv[1]
    }
    return null
}

actual fun setStartupParameter(key: String, value: String?) {
    startupParams[key] = value
}

actual fun copyToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}

actual fun updateUnreadBadge(count: Int) {
    if (count > 0) {
        document.title = "($count) TiK-Market"
    } else {
        document.title = "TiK-Market Place"
    }
}

actual fun setupTabFocusRefresh(callback: () -> Unit) {
    js("""
        window.appStateRefresh = function() {};
        window.dispatchEvent(new CustomEvent('app-focus'));
        window.addEventListener('app-focus', function() {
            if (typeof window._onFocusRefresh === 'function') {
                window._onFocusRefresh();
            }
        });
        window._onFocusRefresh = callback;
    """)
}

actual fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit {
    val observerFunc = js("""
        function(callback) {
            var onlineHandler = function() { callback(1); };
            var offlineHandler = function() { callback(0); };
            window.addEventListener('online', onlineHandler);
            window.addEventListener('offline', offlineHandler);
            setTimeout(function() { callback(navigator.onLine ? 1 : 0); }, 0);
            return function() {
                window.removeEventListener('online', onlineHandler);
                window.removeEventListener('offline', offlineHandler);
            };
        }
    """)
    val cleanup = observerFunc({ status: Int -> onChange(status == 1) })
    return cleanup.unsafeCast<() -> Unit>()
}

actual fun downloadFile(url: String, filename: String) {
    js("""
        var a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    """)
}
