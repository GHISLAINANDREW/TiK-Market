package com.tik_market.utils

import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    val context = LocalContext.current
    return { filename, content ->
        try {
            val pdfFileName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "${filename.substringBeforeLast(".")}.pdf"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val pdfFile = File(downloadDir, pdfFileName)

            val webView = WebView(context)
            webView.settings.loadsImagesAutomatically = true
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    try {
                        val width = 595
                        val height = 842
                        webView.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                        )
                        webView.layout(0, 0, width, height)

                        val pdfDocument = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        webView.draw(page.canvas)
                        pdfDocument.finishPage(page)

                        val fos = FileOutputStream(pdfFile)
                        pdfDocument.writeTo(fos)
                        pdfDocument.close()
                        fos.close()

                        Toast.makeText(context, "✅ Fichier PDF enregistré :\nDownloads/$pdfFileName", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "❌ Erreur génération PDF : ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "❌ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
