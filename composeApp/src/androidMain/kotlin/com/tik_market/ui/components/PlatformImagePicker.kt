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
import com.tik_market.utils.UrlUtils
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
private suspend fun contentUriToDataUrl(context: Context, uri: Uri, maxDim: Int = 1600): String? {
    return withContext(Dispatchers.IO) {
        try {
            var mimeType = context.contentResolver.getType(uri)
            val fileName = getFileName(context, uri)
            
            if (mimeType == null && fileName != null) {
                val ext = fileName.substringAfterLast('.', "").lowercase()
                mimeType = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "mp4" -> "video/mp4"
                    "mov" -> "video/quicktime"
                    else -> null
                }
            }
            if (mimeType == null) mimeType = "application/octet-stream"

            // Client-side size check (15MB limit)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 15 * 1024 * 1024) {
                    throw Exception("Fichier trop volumineux (> 15MB)")
                }
            }

            // Compress images
            if (mimeType.startsWith("image/")) {
                val compressed = compressImage(context, uri, maxDim)
                if (compressed != null) return@withContext "data:image/jpeg;base64,$compressed"
            }

            // Non-image or compression failed: read raw bytes.
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            if (bytes.size > 15 * 1024 * 1024) {
                throw Exception("Fichier trop volumineux (> 15MB)")
            }
            
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mimeType;base64,$base64"
        } catch (e: Exception) {
            android.util.Log.e("Picker", "contentUriToDataUrl error: ${e.message}")
            null
        }
    }
}

/**
 * Downscales and compresses an image to a small JPEG data payload.
 */
fun compressImage(context: Context, uri: Uri, maxDim: Int = 1600): String? {
    return try {
        val resolver = context.contentResolver
        // Read bounds first
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

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
            // Pre-check size
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > 50 * 1024 * 1024) {
                    throw Exception("Vidéo trop lourde (> 50MB)")
                }
            }

            val cacheDir = context.cacheDir
            val outputFile = File.createTempFile("compressed_", ".mp4", cacheDir)
            val ok = VideoCompressor.compress(context, uri, outputFile)
            
            if (ok && outputFile.exists() && outputFile.length() > 0) {
                if (outputFile.length() > 15 * 1024 * 1024) {
                    outputFile.delete()
                    throw Exception("Vidéo compressée encore trop lourde (> 15MB)")
                }
                val bytes = outputFile.readBytes()
                outputFile.delete()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:video/mp4;base64,$base64"
            } else {
                outputFile.delete()
                // Fallback: only if it's small
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()
                
                if (bytes.size > 15 * 1024 * 1024) {
                    throw Exception("La vidéo doit être compressée mais le processus a échoué.")
                }
                
                val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mime;base64,$base64"
            }
        } catch (e: Exception) {
            android.util.Log.e("Picker", "Video error: ${e.message}")
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
    maxDimension: Int,
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
                        contentUriToDataUrl(context, uri, maxDimension)
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
    maxDimension: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    return rememberMediaPickerLauncher(allowVideo = false, maxDimension = maxDimension, onResult = onResult)
}

/**
 * Android actual: fetches remote image bytes with auto-retry.
 */
actual suspend fun fetchImageBytes(url: String): ByteArray? {
    var lastError: Exception? = null
    repeat(3) { attempt ->
        try {
            val bytes = ImageFetcher.fetchBytes(url)
            if (bytes != null) return bytes
        } catch (e: Exception) {
            lastError = e
            kotlinx.coroutines.delay(kotlin.time.Duration.parse("${500 * (attempt + 1)}ms"))
        }
    }
    android.util.Log.e("ImageLoader", "Failed to fetch $url after 3 attempts. Last error: ${lastError?.message}")
    return null
}

/** 
 * Decodes a byte array to an ImageBitmap on Android with sampled decoding.
 * This is much faster and uses way less RAM than full decoding for thumbnails.
 */
actual fun decodeBytesToBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val options = BitmapFactory.Options()
        
        // 1. Just decode bounds to check size
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        
        // 2. Calculate sample size (max target size 1200px for safety)
        val targetSize = 1200
        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
        
        // 3. Decode for real
        options.inJustDecodeBounds = false
        options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // Less RAM
        
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

/**
 * Android actual: fetches a remote image URL and returns it as an ImageBitmap.
 */
actual suspend fun fetchImageAsBitmap(url: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("file://")) {
                val path = url.substringAfter("file://")
                val file = java.io.File(path)
                if (!file.exists()) return@withContext null
                val bytes = file.readBytes()
                return@withContext decodeBytesToBitmap(bytes)
            }

            val bytes = fetchImageBytes(url) ?: return@withContext null
            decodeBytesToBitmap(bytes)
        } catch (e: Exception) {
            android.util.Log.e("ImageLoader", "Error fetching $url", e)
            null
        }
    }
}

/**
 * Android actual: fetches a remote image URL and returns it as a data URL string.
 */
actual suspend fun fetchImageAsDataUrl(url: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            if (url.startsWith("file://")) {
                val path = url.substringAfter("file://")
                val file = java.io.File(path)
                if (!file.exists()) return@withContext null
                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                return@withContext "data:image/jpeg;base64,$base64"
            }

            val safeUrl = UrlUtils.resolveSafeUrl(url)
            val connection = URL(safeUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")

            if (connection.responseCode !in 200..299) return@withContext null

            val bytes = connection.inputStream.use { it.readBytes() }
            val contentType = connection.contentType ?: "image/jpeg"
            val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "data:$contentType;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Android actual: uses ActivityResultContracts.TakePicturePreview() to take a photo.
 */
@Composable
actual fun rememberTakePhotoLauncher(
    maxDimension: Int,
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                try {
                    // Resize bitmap if needed
                    val scaled = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                        android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                    } else bitmap

                    val baos = ByteArrayOutputStream()
                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                    if (scaled != bitmap) scaled.recycle()
                    
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
