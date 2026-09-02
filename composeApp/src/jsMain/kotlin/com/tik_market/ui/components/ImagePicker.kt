package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberMediaPickerLauncher(
    allowVideo: Boolean,
    maxDurationSeconds: Int,
    videoOnly: Boolean,
    maxDimension: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    return {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = if (allowVideo || videoOnly) "image/*,video/*" else "image/*"
        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                if (file.type.startsWith("video/")) {
                    val video = document.createElement("video") as HTMLVideoElement
                    video.preload = "metadata"
                    video.onloadedmetadata = {
                        window.asDynamic().URL.revokeObjectURL(video.src)
                        val duration = video.duration
                        val reader = FileReader()
                        reader.onload = {
                            onResult(MediaPickResult(
                                dataUrl = reader.result.toString(),
                                fileName = file.name,
                                mimeType = file.type,
                                durationSeconds = duration
                            ))
                        }
                        reader.readAsDataURL(file)
                    }
                    video.src = window.asDynamic().URL.createObjectURL(file) as String
                } else {
                    val reader = FileReader()
                    reader.onload = {
                        onResult(MediaPickResult(
                            dataUrl = reader.result.toString(),
                            fileName = file.name,
                            mimeType = file.type,
                            durationSeconds = 0.0
                        ))
                    }
                    reader.readAsDataURL(file)
                }
            } else {
                onResult(null)
            }
        }
        input.click()
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    maxDimension: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    return rememberMediaPickerLauncher(allowVideo = false, maxDimension = maxDimension, onResult = onResult)
}

@Composable
actual fun rememberTakePhotoLauncher(
    maxDimension: Int,
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            try {
                val result = suspendCoroutine<String?> { cont ->
                    com.tik_market.ui.chat.takePhoto { url -> cont.resume(url) }
                }
                onResult(result)
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }
}

@Composable
actual fun rememberTakeVideoLauncher(
    maxDurationSeconds: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    // On web, fall back to the media picker (video only) since there is no
    // direct camera capture API available in the browser.
    return rememberMediaPickerLauncher(
        allowVideo = true,
        maxDurationSeconds = maxDurationSeconds,
        videoOnly = true,
        onResult = onResult
    )
}

@Composable
actual fun rememberPickFileLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            try {
                val result = suspendCoroutine<String?> { cont ->
                    com.tik_market.ui.chat.pickFile { url -> cont.resume(url) }
                }
                onResult(result)
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }
}

actual suspend fun fetchImageBytes(url: String): ByteArray? {
    val dataUrl = fetchImageAsDataUrl(url) ?: return null
    val base64 = dataUrl.substringAfter(",")
    val binaryStr = window.atob(base64.trim())
    if (binaryStr.isEmpty()) return null
    return ByteArray(binaryStr.length) { i -> binaryStr[i].code.toByte() }
}

actual fun decodeBytesToBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val skiaImage = org.jetbrains.skia.Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (_: Exception) { null }
}

actual suspend fun fetchImageAsBitmap(url: String): ImageBitmap? {
    val dataUrl = fetchImageAsDataUrl(url) ?: return null
    return decodeDataUrlToImageBitmap(dataUrl)
}

actual suspend fun fetchImageAsDataUrl(url: String): String? {
    return try {
        kotlin.coroutines.suspendCoroutine { cont ->
            window.fetch(url).then({ response ->
                if (!response.ok) throw Exception("HTTP ${response.status}")
                response.blob().then({ blob ->
                    val reader = FileReader()
                    reader.onload = { cont.resume(reader.result.toString()) }
                    reader.onerror = { cont.resume(null) }
                    reader.readAsDataURL(blob)
                })
            }).catch({
                cont.resume(null)
            })
        }
    } catch (_: Exception) {
        null
    }
}

actual fun decodeDataUrlToImageBitmap(dataUrl: String): ImageBitmap? {
    return try {
        if (!dataUrl.contains(",")) return null
        val base64 = dataUrl.substringAfter(",")
        val binaryStr = window.atob(base64.trim())
        if (binaryStr.isEmpty()) return null
        
        val bytes = ByteArray(binaryStr.length) { i -> binaryStr[i].code.toByte() }
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
