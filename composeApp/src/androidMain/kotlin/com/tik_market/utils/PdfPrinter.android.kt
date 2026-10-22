package com.tik_market.utils

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit {
    val context = LocalContext.current
    return { htmlContent, jobName ->
        val webView = WebView(context)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
