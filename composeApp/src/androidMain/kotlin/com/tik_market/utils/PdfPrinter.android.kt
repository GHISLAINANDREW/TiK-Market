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

private val activePrinterWebViews = mutableSetOf<WebView>()

@Composable
actual fun rememberPdfPrinter(): (htmlContent: String, jobName: String) -> Unit {
    val context = LocalContext.current
    return { htmlContent, jobName ->
        try {
            Toast.makeText(context, "⏳ Préparation de l'impression PDF...", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).post {
                try {
                    WebView.enableSlowWholeDocumentDraw()

                    val webView = WebView(context)
                    activePrinterWebViews.add(webView)

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
                                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Erreur d'impression : ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                activePrinterWebViews.remove(webView)
                            }
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            activePrinterWebViews.remove(webView)
                            Toast.makeText(context, "Erreur chargement : $description", Toast.LENGTH_SHORT).show()
                        }
                    }

                    webView.loadDataWithBaseURL("about:blank", htmlContent, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
