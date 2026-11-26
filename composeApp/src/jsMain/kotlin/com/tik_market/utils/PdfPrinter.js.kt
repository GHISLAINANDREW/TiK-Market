package com.tik_market.utils

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement

@Composable
actual fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit {
    return { htmlContent, jobName ->
        try {
            val iframe = document.createElement("iframe") as HTMLIFrameElement
            iframe.setAttribute("style", "position:fixed;right:0;bottom:0;width:0;height:0;border:0;")
            document.body?.appendChild(iframe)

            val doc = iframe.contentWindow?.document
            doc?.open()
            doc?.write(htmlContent)
            doc?.close()

            window.setTimeout({
                iframe.contentWindow?.focus()
                iframe.contentWindow?.print()
                window.setTimeout({
                    document.body?.removeChild(iframe)
                }, 2000)
            }, 500)
        } catch (e: Exception) {
            println("[PdfPrinter] Error: ${e.message}")
        }
    }
}
