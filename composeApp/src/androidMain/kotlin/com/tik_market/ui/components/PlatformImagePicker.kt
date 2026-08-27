package com.tik_market.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tik_market.utils.VideoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Converts a content:// URI to a base64 data URL string.
 * Images are automatically downscaled and compressed (JPEG) to keep the
 * base64 payload small enough for the upload endpoint. Videos are passed
 * through as-is (they are compressed separately by VideoCompressor).
 */
private suspend fun contentUriToDataUrl(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            var mimeType = context.contentResolver.getType(uri)
            if (mimeType == null) {
                val fileName = getFileName(context, uri)
                val ext = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
                mimeType = mapOf(
                    "pdf" to "application/pdf",
                    "doc" to "application/msword",
                    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "xls" to "application/vnd.ms-excel",
                    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "ppt" to "application/vnd.ms-powerpoint",
                    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "zip" to "application/zip",
                    "rar" to "application/vnd.rar",
                    "gz" to "application/gzip",
                    "txt" to "text/plain",
                    "csv" to "text/csv",
                    "vcf" to "text/vcard",
                    "json" to "application/json",
                    "jpg" to "image/jpeg",
                    "jpeg" to "image/jpeg",
                    "png" to "image/png",
                    "gif" to "image/gif",
                    "mp4" to "video/mp4",
                    "mp3" to "audio/mpeg",
                )[ext] ?: "application/octet-stream"
            }

            // Compress images (photos from phones are 3-8MB; base64 adds +33%).
            if (mimeType.startsWith("image/")) {
                val compressed = compressImage(context, uri)
                if (compressed != null) return@withContext "data:image/jpeg;base64,$compressed"
            }

            // Non-image: read raw bytes.
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val baos = ByteArrayOutputStream()
            inputStream.copyTo(baos)
            inputStream.close()
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mimeType;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Downscales and compresses an image to a small JPEG data payload.
 * Max dimension 1600px, JPEG quality 80 -> typically 100-400 KB.
 */
private fun compressImage(context: Context, uri: Uri): String? {
    return try {
        val resolver = context.contentResolver
        // Read bounds first to avoid loading a huge bitmap into memory.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        val maxDim = 1600
        var sampleSize = 1
        while (srcW / sampleSize > maxDim * 2 || srcH / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        // Scale down to max dimension.
        var outW = bitmap.width
        var outH = bitmap.height
        if (outW > maxDim || outH > maxDim) {
            val scale = maxDim.toFloat() / maxOf(outW, outH)
            outW = (outW * scale).toInt()
            outH = (outH * scale).toInt()
        }
        val scaled = if (outW != bitmap.width || outH != bitmap.height) {
            android.graphics.Bitmap.createScaledBitmap(bitmap, outW, outH, true)
        } else {
            bitmap
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

/**
 * Compresses a video to a smaller H.264 file and returns it as a base64 data URL.
 * Falls back to reading the raw bytes if compression fails, so uploads still work.
 */
private suspend fun compressVideoToDataUrl(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val outputFile = File.createTempFile("compressed_", ".mp4", cacheDir)
            val ok = VideoCompressor.compress(context, uri, outputFile)
            if (ok && outputFile.exists() && outputFile.length() > 0) {
                val bytes = outputFile.readBytes()
                outputFile.delete()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:video/mp4;base64,$base64"
            } else {
                outputFile.delete()
                // Fallback: read raw bytes.
                val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                val base64 = Base64.encodeToString(raw, Base64.NO_WRAP)
                "data:$mime;base64,$base64"
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Resolves a content:// URI to a display-friendly filename.
 */
private fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    return try {
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    } finally {
        cursor.close()
    }
}

/**
 * Android actual: opens a picker showing only the requested media type.
 *
 * IMPORTANT: we deliberately use GetContent with a SINGLE MIME type
 * (image MIME or video MIME) instead of PickVisualMedia.ImageAndVideo,
 * because on Android < 13 and many OEM ROMs (Tecno, Infinix, common in
 * Cameroon) the combined picker only shows photos and hides videos.
 * Launching separate pickers per type guarantees videos are selectable.
 */
@Composable
actual fun rememberMediaPickerLauncher(
    allowVideo: Boolean,
    maxDurationSeconds: Int,
    videoOnly: Boolean,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val fileName = getFileName(context, uri) ?: "file"
                    var duration = 0.0

                    // Compress videos before base64 encoding so the upload succeeds.
                    val dataUrl = if (mimeType.startsWith("video/")) {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        duration = (time?.toLong() ?: 0L) / 1000.0
                        retriever.release()
                        compressVideoToDataUrl(context, uri)
                    } else {
                        contentUriToDataUrl(context, uri)
                    }

                    if (dataUrl != null) {
                        onResult(MediaPickResult(dataUrl, fileName, mimeType, duration))
                    } else {
                        onResult(null)
                    }
                } catch (_: Exception) {
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }
    return remember {
        {
            if (videoOnly) {
                launcher.launch("video/*")
            } else {
                launcher.launch("image/*")
            }
        }
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    return rememberMediaPickerLauncher(allowVideo = false, onResult = onResult)
}

/**
 * Android actual: fetches a remote image URL and returns it as a data URL string.
 * OPTIMIZATION: We try to avoid large base64 strings if possible, but to keep
 * compatibility with the current common logic, we use a more efficient stream reading.
 */
actual suspend fun fetchImageAsDataUrl(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("file://")) {
                val path = url.substringAfter("file://")
                val file = java.io.File(path)
                if (!file.exists()) return@withContext null
                val bytes = file.readBytes()
                val contentType = "image/jpeg" // Fallback
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                return@withContext "data:$contentType;base64,$base64"
            }

            // Force HTTPS and use proxy if URL points to the blocked Render domain
            val renderBase = "https://tik-market.onrender.com"
            val proxyBase = "https://tik-market-proxy.gtankou.workers.dev"
            
            var safeUrl = if (url.startsWith("http://") && (url.contains("loca.lt") || url.contains("ngrok") || url.contains("cloudflare"))) {
                url.replace("http://", "https://")
            } else {
                url
            }

            // Redirect Render and Cloudinary images through the Cloudflare proxy to bypass Orange Cameroon block
            if (safeUrl.contains("onrender.com")) {
                safeUrl = safeUrl.replace(renderBase, proxyBase)
            } else if (safeUrl.contains("res.cloudinary.com")) {
                // Proxy Cloudinary images too if Orange blocks res.cloudinary.com
                safeUrl = "$proxyBase/proxy?url=" + java.net.URLEncoder.encode(safeUrl, "UTF-8")
            }

            val connection = URL(safeUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            connection.setRequestProperty("Accept", "image/*,*/*")

            if (connection.responseCode != 200) {
                android.util.Log.e("ImageLoader", "HTTP ${connection.responseCode} for $safeUrl")
                return@withContext null
            }

            val contentType = connection.contentType ?: "image/jpeg"
            val bytes = connection.inputStream.use { it.readBytes() }
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            "data:$contentType;base64,$base64"
        } catch (e: Exception) {
            android.util.Log.e("ImageLoader", "Error fetching $url", e)
            null
        }
    }
}

/**
 * Android actual: uses ActivityResultContracts.TakePicturePreview() to take a photo.
 */
@Composable
actual fun rememberTakePhotoLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                try {
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                    val bytes = baos.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onResult("data:image/jpeg;base64,$base64")
                } catch (_: Exception) {
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }
    return remember { { launcher.launch(null) } }
}

/**
 * Android actual: uses ActivityResultContracts.GetContent() with MIME wildcard to pick any file.
 */
@Composable
actual fun rememberPickFileLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val dataUrl = contentUriToDataUrl(context, uri)
                    onResult(dataUrl)
                } catch (_: Exception) {
                    onResult(null)
                }
            }
        } else {
            onResult(null)
        }
    }
    return remember { { launcher.launch("*/*") } }
}

/**
 * Decodes a base64 data URL to an ImageBitmap on Android.
 */
actual fun decodeDataUrlToImageBitmap(dataUrl: String): ImageBitmap? {
    return try {
        if (!dataUrl.startsWith("data:image")) return null
        val base64 = dataUrl.substringAfter(",")
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
