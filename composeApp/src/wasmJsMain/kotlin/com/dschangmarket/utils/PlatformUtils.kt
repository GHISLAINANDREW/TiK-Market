package com.dschangmarket.utils

import kotlinx.browser.window

actual fun getStartupParameter(key: String): String? {
    val search = window.location.search
    if (search.isBlank()) return null
    val params = search.substring(1).split("&")
    for (p in params) {
        val kv = p.split("=")
        if (kv.size == 2 && kv[0] == key) return kv[1]
    }
    return null
}

actual fun copyToClipboard(text: String) {
    window.navigator.clipboard.writeText(text)
}

@JsFun("""(count) => {
    if (count > 0) {
        document.title = "(" + count + ") Dschang Market";
    } else {
        document.title = "Dschang Market Place";
    }
}""")
private external fun jsUpdateBadge(count: Int)

actual fun updateUnreadBadge(count: Int) {
    jsUpdateBadge(count)
}

@JsFun("""() => {
    // Register a global function that the HTML visibility handler calls
    window.appStateRefresh = () => {};
    // But the actual refresh is triggered via a custom event
    window.dispatchEvent(new CustomEvent('app-focus'));
}""")
private external fun jsInitFocusListener()

private var focusCallback: (() -> Unit)? = null

@JsFun("""() => {
    window.addEventListener('app-focus', function() {
        if (typeof window._onFocusRefresh === 'function') {
            window._onFocusRefresh();
        }
    });
}""")
private external fun jsListenFocus()

@JsFun("""(fn) => {
    window._onFocusRefresh = fn;
}""")
private external fun jsSetFocusCallback(fn: () -> Unit)

actual fun setupTabFocusRefresh(callback: () -> Unit) {
    jsInitFocusListener()
    jsListenFocus()
    jsSetFocusCallback(callback)
}

@JsFun("""(callback) => {
    var onlineHandler = function() { callback(true); };
    var offlineHandler = function() { callback(false); };
    window.addEventListener('online', onlineHandler);
    window.addEventListener('offline', offlineHandler);
    // Run once to set initial state
    setTimeout(function() { callback(navigator.onLine); }, 0);
    // Return a cleanup function
    return function() {
        window.removeEventListener('online', onlineHandler);
        window.removeEventListener('offline', offlineHandler);
    };
}""")
private external fun jsObserveConnectivity(callback: (Boolean) -> Unit): () -> Unit

actual fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit {
    return jsObserveConnectivity(onChange)
}

@JsFun("""(url, filename) => {
    var a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}""")
private external fun jsDownloadFile(url: String, filename: String)

actual fun downloadFile(url: String, filename: String) {
    jsDownloadFile(url, filename)
}
