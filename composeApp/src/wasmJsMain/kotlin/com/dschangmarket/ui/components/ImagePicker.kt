package com.dschangmarket.ui.components

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
@JsFun("""(onResult, onError) => {
    try {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.onchange = () => {
            try {
                const file = input.files[0];
                if (!file) { onError('Aucun fichier sélectionné'); return; }
                const reader = new FileReader();
                reader.onload = () => {
                    onResult(JSON.stringify({
                        dataUrl: reader.result,
                        fileName: file.name
                    }));
                };
                reader.onerror = () => onError('Erreur de lecture du fichier');
                reader.readAsDataURL(file);
            } catch (e) {
                onError(String(e));
            }
        };
        input.click();
    } catch (e) {
        onError(String(e));
    }
}""")
private external fun nativePickImage(
    onResult: (String) -> Unit,
    onError: (String) -> Unit
)

/**
 * Parses JSON string into an object and gets a property by key.
 */
@JsFun("(json, key) => { const obj = JSON.parse(json); return obj[key] || null; }")
private external fun getJsonProperty(json: String, key: String): String?

// ── Suspend function (kept for potential direct use) ────────────

private suspend fun pickImageFromGallery(): ImagePickResult? {
    return try {
        val json = suspendCoroutine<String> { cont ->
            nativePickImage(
                onResult = { cont.resume(it) },
                onError = { cont.resumeWithException(Exception(it)) }
            )
        }
        val dataUrl = getJsonProperty(json, "dataUrl") ?: return null
        val fileName = getJsonProperty(json, "fileName") ?: "photo.jpg"
        ImagePickResult(dataUrl = dataUrl, fileName = fileName)
    } catch (_: Exception) {
        null // User cancelled or error
    }
}

// ── Composable launcher (used by common ImagePicker) ────────────

@Composable
actual fun rememberImagePickerLauncher(
    onResult: (result: ImagePickResult?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val result = pickImageFromGallery()
            onResult(result)
        }
    }
}

// ── Camera / Photo launcher (wasmJs: uses capture input) ─────────

@Composable
actual fun rememberTakePhotoLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            try {
                val result = suspendCoroutine<String?> { cont ->
                    com.dschangmarket.ui.chat.takePhoto { url -> cont.resume(url) }
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
                    com.dschangmarket.ui.chat.pickFile { url -> cont.resume(url) }
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
