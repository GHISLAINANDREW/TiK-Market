package com.tik_market.utils

import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    val context = LocalContext.current
    return { filename, content ->
        try {
            val pdfFileName = if (filename.endsWith(".pdf", ignoreCase = true)) filename else "${filename.substringBeforeLast(".")}.pdf"
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val pdfFile = File(downloadDir, pdfFileName)

            val webView = WebView(context)
            webView.settings.loadsImagesAutomatically = true
            webView.settings.javaScriptEnabled = true
            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.postDelayed({
                        try {
                            val width = 595
                            val height = 842
                            view.measure(
                                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                            )
                            view.layout(0, 0, width, height)

                            val pdfDocument = PdfDocument()
                            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
                            val page = pdfDocument.startPage(pageInfo)
                            view.draw(page.canvas)
                            pdfDocument.finishPage(page)

                            val fos = FileOutputStream(pdfFile)
                            pdfDocument.writeTo(fos)
                            pdfDocument.close()
                            fos.close()

                            Toast.makeText(context, "✅ PDF enregistré : ${pdfFile.name}", Toast.LENGTH_LONG).show()

                            // Open PDF Intent
                            try {
                                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "❌ Erreur génération PDF : ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, 600)
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
