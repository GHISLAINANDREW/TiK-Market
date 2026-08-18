package com.tik_market.utils

import kotlinx.browser.window
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.events.Event

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
    window.asDynamic().appStateRefresh = {}
    window.dispatchEvent(window.asDynamic().eval("new CustomEvent('app-focus')"))
    window.addEventListener("app-focus", { e: Event ->
        val refresh = window.asDynamic()._onFocusRefresh
        if (refresh != null) {
            refresh()
        }
    })
    window.asDynamic()._onFocusRefresh = callback
}

actual fun observeConnectivity(onChange: (Boolean) -> Unit): () -> Unit {
    val onlineHandler = { e: Event -> onChange(true) }
    val offlineHandler = { e: Event -> onChange(false) }
    window.addEventListener("online", onlineHandler)
    window.addEventListener("offline", offlineHandler)
    
    window.setTimeout({
        onChange(window.navigator.onLine)
    }, 0)
    
    return {
        window.removeEventListener("online", onlineHandler)
        window.removeEventListener("offline", offlineHandler)
    }
}

actual fun downloadFile(url: String, filename: String) {
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.setAttribute("download", filename)
    document.body?.appendChild(a)
    a.click()
    document.body?.removeChild(a)
}
