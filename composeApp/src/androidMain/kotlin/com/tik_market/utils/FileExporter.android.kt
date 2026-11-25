package com.tik_market.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    val context = LocalContext.current
    return { filename, content ->
        try {
            Toast.makeText(context, "⏳ Préparation du PDF...", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).post {
                try {
                    WebView.enableSlowWholeDocumentDraw()

                    val jobName = filename.substringBeforeLast(".")
                    val webView = WebView(context)

                    webView.settings.apply {
                        javaScriptEnabled = true
                        loadsImagesAutomatically = true
                        domStorageEnabled = true
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            try {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                                printManager.print(
                                    jobName,
                                    printAdapter,
                                    PrintAttributes.Builder()
                                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                        .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
                                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                                        .build()
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "❌ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            Toast.makeText(context, "❌ Erreur : $description", Toast.LENGTH_SHORT).show()
                        }
                    }

                    webView.loadDataWithBaseURL("about:blank", content, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "❌ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
