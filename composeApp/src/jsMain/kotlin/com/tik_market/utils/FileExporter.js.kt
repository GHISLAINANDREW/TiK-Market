package com.tik_market.utils

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    return { filename, content ->
        try {
            val iframe = document.createElement("iframe") as HTMLIFrameElement
            iframe.setAttribute("style", "position:fixed;right:0;bottom:0;width:0;height:0;border:0;")
            document.body?.appendChild(iframe)

            val doc = iframe.contentWindow?.document
            doc?.open()
            doc?.write(content)
            doc?.close()

            window.setTimeout({
                iframe.contentWindow?.focus()
                iframe.contentWindow?.print()
                window.setTimeout({
                    document.body?.removeChild(iframe)
                }, 2000)
            }, 500)
            true
        } catch (e: Exception) {
            println("[FileExporter] Error: ${e.message}")
            false
        }
    }
}
