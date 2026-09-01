package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

// ── JS interop: file picker via callbacks ───────────────────────

/**
 * Opens a file picker for images, reads the file as data URL, then
 * calls onResult with JSON {dataUrl, fileName} or onError with a message.
 */
@JsFun("""(allowVideo, onResult, onError) => {
    try {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = allowVideo ? 'image/*,video/*' : 'image/*';
        input.onchange = async () => {
            try {
                const file = input.files[0];
                if (!file) { onError('Aucun fichier sélectionné'); return; }
                
                let duration = 0;
                if (file.type.startsWith('video/')) {
                    const video = document.createElement('video');
                    video.preload = 'metadata';
                    video.onloadedmetadata = function() {
                        window.URL.revokeObjectURL(video.src);
                        duration = video.duration;
                        const reader = new FileReader();
                        reader.onload = () => {
                            onResult(JSON.stringify({
                                dataUrl: reader.result,
                                fileName: file.name,
                                mimeType: file.type,
                                durationSeconds: duration
                            }));
                        };
                        reader.readAsDataURL(file);
                    };
                    video.src = URL.createObjectURL(file);
                } else {
                    const reader = new FileReader();
                    reader.onload = () => {
                        onResult(JSON.stringify({
                            dataUrl: reader.result,
                            fileName: file.name,
                            mimeType: file.type,
                            durationSeconds: 0
                        }));
                    };
                    reader.readAsDataURL(file);
                }
            } catch (e) {
                onError(String(e));
            }
        };
        input.click();
    } catch (e) {
        onError(String(e));
    }
}""")
private external fun nativePickMedia(
    allowVideo: Boolean,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
)

@JsFun("(json, key) => { const obj = JSON.parse(json); return obj[key] === undefined ? null : String(obj[key]); }")
private external fun getJsonProperty(json: String, key: String): String?

@JsFun("(json, key) => { const obj = JSON.parse(json); return obj[key] || 0; }")
private external fun getJsonPropertyDouble(json: String, key: String): Double

@Composable
actual fun rememberMediaPickerLauncher(
    allowVideo: Boolean,
    maxDurationSeconds: Int,
    videoOnly: Boolean,
    maxDimension: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    // IMPORTANT: input.click() doit être appelé de manière SYNCHRONE dans le
    // gestionnaire d'événement utilisateur, sinon le navigateur bloque l'ouverture
    // du sélecteur de fichiers (popup bloquée). On appelle donc nativePickMedia
    // directement, sans passer par une coroutine.
    return {
        nativePickMedia(
            allowVideo = allowVideo || videoOnly,
            onResult = { json ->
                val dataUrl = getJsonProperty(json, "dataUrl")
                if (dataUrl == null) {
                    onResult(null)
                    return@nativePickMedia
                }
                val fileName = getJsonProperty(json, "fileName") ?: "file"
                val mimeType = getJsonProperty(json, "mimeType") ?: "image/jpeg"
                val duration = getJsonPropertyDouble(json, "durationSeconds")
                onResult(MediaPickResult(dataUrl = dataUrl, fileName = fileName, mimeType = mimeType, durationSeconds = duration))
            },
            onError = { onResult(null) }
        )
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    maxDimension: Int,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit {
    return rememberMediaPickerLauncher(allowVideo = false, maxDimension = maxDimension, onResult = onResult)
}

// ── Camera / Photo launcher (wasmJs: uses capture input) ─────────

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

// ── File picker launcher (wasmJs: uses general file input) ──────

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

// ── Remote image fetch as dataUrl ───────────────────────────────

/**
 * Fetches a remote URL as a base64 dataUrl using fetch() + FileReader.
 * Uses callback pattern (not suspend) because @JsFun cannot be suspend.
 */
@JsFun("""
    (url, onSuccess, onError) => {
        console.log('[ImageLoader] Fetching image:', url);
        fetch(url)
            .then(r => {
                if (!r.ok) {
                    console.error('[ImageLoader] HTTP error:', r.status, r.statusText, 'for', url);
                    throw new Error('HTTP ' + r.status + ' ' + r.statusText);
                }
                return r.blob();
            })
            .then(blob => {
                const reader = new FileReader();
                reader.onload = () => {
                    console.log('[ImageLoader] Image fetched and read as DataURL (' + reader.result.substring(0, 30) + '...)');
                    onSuccess(reader.result);
                };
                reader.onerror = () => {
                    console.error('[ImageLoader] FileReader error');
                    onError('Erreur de lecture');
                };
                reader.readAsDataURL(blob);
            })
            .catch(e => {
                console.error('[ImageLoader] Fetch failed for:', url, e.message);
                if (e.message.includes('fetch')) {
                    console.warn('[ImageLoader] This might be a CORS issue. Ensure the server allows cross-origin requests.');
                }
                onError(e.message || String(e));
            });
    }
""")
private external fun nativeFetchAsDataUrl(
    url: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
)

actual suspend fun fetchImageBytes(url: String): ByteArray? {
    val dataUrl = fetchImageAsDataUrl(url) ?: return null
    val base64 = dataUrl.substringAfter(",")
    val binaryStr = base64ToBinaryString(base64)
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
            nativeFetchAsDataUrl(
                url,
                onSuccess = { cont.resume(it) },
                onError = { cont.resumeWithException(Exception(it)) }
            )
        }
    } catch (_: Exception) {
        null
    }
}

// ── Data URL → ImageBitmap (wasmJs: via Skiko + JS atob) ───────

/**
 * Decodes a base64 string into a raw binary string via JavaScript's atob().
 * Returns a String where each character's charCodeAt() is a byte value (0-255).
 */
@JsFun("""
    (base64) => {
        try {
            return atob(base64.trim());
        } catch (e) {
            console.error('[ImageLoader] atob failed:', e.message);
            return "";
        }
    }
""")
private external fun base64ToBinaryString(base64: String): String

actual fun decodeDataUrlToImageBitmap(dataUrl: String): ImageBitmap? {
    return try {
        if (!dataUrl.contains(",")) {
            println("[ImageLoader] Invalid data URL (no comma found)")
            return null
        }
        val base64 = dataUrl.substringAfter(",")
        val binaryStr = base64ToBinaryString(base64)
        if (binaryStr.isEmpty()) return null
        
        val bytes = ByteArray(binaryStr.length) { i -> binaryStr[i].code.toByte() }
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        println("[ImageLoader] Decode error: " + e.message)
        null
    }
}
