package com.dschangmarket.ui.components

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Converts a content:// URI to a base64 data URL string.
 * If getType returns null, detects MIME from file extension.
 */
private suspend fun contentUriToDataUrl(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val baos = ByteArrayOutputStream()
            inputStream.copyTo(baos)
            inputStream.close()
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            // Try to get MIME from ContentResolver, fall back to extension-based detection
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
            "data:$mimeType;base64,$base64"
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
 * Android actual: uses the system Photo Picker (PickVisualMedia) which shows
 * both images and videos in a single gallery. GetContent/OpenDocument only
 * accept one MIME type and many devices hide videos from the picker.
 */
@Composable
actual fun rememberMediaPickerLauncher(
    allowVideo: Boolean,
    maxDurationSeconds: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val dataUrl = contentUriToDataUrl(context, uri)
                    if (dataUrl != null) {
                        val fileName = getFileName(context, uri) ?: "file"
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        var duration = 0.0
                        if (mimeType.startsWith("video/")) {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(context, uri)
                            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            duration = (time?.toLong() ?: 0L) / 1000.0
                            retriever.release()
                        }
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
    val visualMediaType =
        if (allowVideo) ActivityResultContracts.PickVisualMedia.ImageAndVideo
        else ActivityResultContracts.PickVisualMedia.ImageOnly
    return remember { { launcher.launch(androidx.activity.result.PickVisualMediaRequest(visualMediaType)) } }
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
            // Force HTTPS for tunnel domains
            val safeUrl = if (url.startsWith("http://") && (url.contains("loca.lt") || url.contains("ngrok") || url.contains("cloudflare"))) {
                url.replace("http://", "https://")
            } else {
                url
            }

            val connection = URL(safeUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode != 200) return@withContext null

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
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
