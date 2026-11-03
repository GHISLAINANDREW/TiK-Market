package com.tik_market.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

private val activeWebViews = mutableSetOf<WebView>()

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    val context = LocalContext.current
    return { filename, content ->
        try {
            Toast.makeText(context, "⏳ Génération du PDF en cours...", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).post {
                try {
                    WebView.enableSlowWholeDocumentDraw()

                    val isPdf = filename.endsWith(".pdf", ignoreCase = true)
                    val pdfFileName = if (isPdf) filename else "${filename.substringBeforeLast(".")}.pdf"

                    val appDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                    if (!appDownloadDir.exists()) appDownloadDir.mkdirs()

                    val tempPdfFile = File(appDownloadDir, pdfFileName)

                    val webView = WebView(context)
                    activeWebViews.add(webView)

                    webView.settings.apply {
                        loadsImagesAutomatically = true
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        allowContentAccess = true
                        allowFileAccess = true
                    }

                    val isProcessed = AtomicBoolean(false)

                    fun processPdfExport() {
                        if (isProcessed.getAndSet(true)) return
                        try {
                            // Dimensions A4 standard à 96 DPI (Web)
                            val htmlWidthPx = 794
                            val htmlPageHeightPx = 1123

                            // Dimensions A4 standard à 72 DPI (Points PDF)
                            val pdfWidthPt = 595
                            val pdfPageHeightPt = 842

                            // Ratio d'échelle pour mapper le layout HTML (794px) sur le PDF (595pt)
                            val scale = pdfWidthPt.toFloat() / htmlWidthPx.toFloat()

                            val density = context.resources.displayMetrics.density
                            val contentHeightPx = (webView.contentHeight * density).toInt()

                            webView.measure(
                                View.MeasureSpec.makeMeasureSpec(htmlWidthPx, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                            )

                            val totalHeightPx = maxOf(webView.measuredHeight, contentHeightPx, htmlPageHeightPx)
                            webView.layout(0, 0, htmlWidthPx, totalHeightPx)

                            val pdfDocument = PdfDocument()
                            val pageCount = ((totalHeightPx + htmlPageHeightPx - 1) / htmlPageHeightPx).coerceAtLeast(1)

                            for (i in 0 until pageCount) {
                                val pageInfo = PdfDocument.PageInfo.Builder(pdfWidthPt, pdfPageHeightPt, i + 1).create()
                                val page = pdfDocument.startPage(pageInfo)
                                val canvas = page.canvas

                                canvas.save()
                                canvas.scale(scale, scale)
                                canvas.translate(0f, -(i * htmlPageHeightPx).toFloat())
                                webView.draw(canvas)
                                canvas.restore()

                                pdfDocument.finishPage(page)
                            }

                            FileOutputStream(tempPdfFile).use { fos ->
                                pdfDocument.writeTo(fos)
                            }
                            pdfDocument.close()

                            val finalFile = savePdfToPublicDownloads(context, tempPdfFile, pdfFileName)

                            Toast.makeText(context, "✅ PDF téléchargé : ${finalFile.name}", Toast.LENGTH_LONG).show()

                            try {
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    finalFile
                                )
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
                        } finally {
                            activeWebViews.remove(webView)
                            try { webView.destroy() } catch (_: Exception) {}
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.postDelayed({
                                processPdfExport()
                            }, 500)
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                            processPdfExport()
                        }
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        processPdfExport()
                    }, 3000)

                    webView.loadDataWithBaseURL("about:blank", content, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "❌ Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
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

private fun savePdfToPublicDownloads(context: Context, sourceFile: File, fileName: String): File {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
        } else {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!publicDir.exists()) publicDir.mkdirs()
            val publicFile = File(publicDir, fileName)
            sourceFile.copyTo(publicFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(publicFile.absolutePath),
                arrayOf("application/pdf"),
                null
            )
            return publicFile
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return sourceFile
}
