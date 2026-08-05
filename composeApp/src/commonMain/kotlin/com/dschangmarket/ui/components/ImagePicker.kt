package com.dschangmarket.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.theme.*

data class MediaPickResult(
    val dataUrl: String,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    val durationSeconds: Double = 0.0
)

/**
 * Converts a base64 data URL string (e.g. "data:image/png;base64,...") to an ImageBitmap.
 * Returns null if decoding fails.
 */
expect fun decodeDataUrlToImageBitmap(dataUrl: String): ImageBitmap?

/**
 * Platform-specific image picker launcher for use with Compose.
 * Returns a lambda that, when invoked, opens the device photo gallery.
 * The result is delivered via [onResult] callback (null means cancelled/error).
 *
 * - WasmJs: uses hidden <input type="file"> + FileReader
 * - Android: uses ActivityResultContracts.GetContent()
 */
@Composable
expect fun rememberImagePickerLauncher(
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit

/**
 * Platform-specific launcher to take a photo using the device camera.
 * Returns a lambda to launch the camera. Result is a base64 data URL.
 */
@Composable
expect fun rememberTakePhotoLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit

/**
 * Platform-specific launcher to pick any file from the device.
 * Returns a lambda to launch the picker. Result is a base64 data URL.
 */
@Composable
expect fun rememberPickFileLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit

@Composable
expect fun rememberMediaPickerLauncher(
    allowVideo: Boolean = false,
    maxDurationSeconds: Int = 0,
    videoOnly: Boolean = false,
    onResult: (result: MediaPickResult?) -> Unit
): () -> Unit

@Composable
fun MediaPicker(
    currentUrl: String = "",
    onMediaPicked: (MediaPickResult) -> Unit,
    label: String = "Média (Photo/Vidéo)",
    allowVideo: Boolean = true,
    maxDurationSeconds: Int = 30,
    modifier: Modifier = Modifier
) {
    var previewUrl by remember { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVideoPreview by remember { mutableStateOf(false) }

    LaunchedEffect(previewUrl) {
        val url = previewUrl
        if (url.isNotBlank()) {
            if (url.startsWith("data:video") || url.endsWith(".mp4")) {
                isVideoPreview = true
                previewBitmap = null
            } else {
                isVideoPreview = false
                if (url.startsWith("data:")) {
                    previewBitmap = decodeDataUrlToImageBitmap(url)
                } else if (url.startsWith("http")) {
                    try {
                        previewBitmap = loadImageFromUrl(url)
                    } catch (_: Exception) {
                        previewBitmap = null
                    }
                }
            }
        }
    }

    fun handleResult(result: MediaPickResult?) {
        isLoading = false
        if (result != null) {
            if (maxDurationSeconds > 0 && result.durationSeconds > maxDurationSeconds) {
                errorMessage = "Vidéo trop longue (max ${maxDurationSeconds}s)"
            } else {
                previewUrl = result.dataUrl
                onMediaPicked(result)
                errorMessage = null
            }
        }
    }

    // Photo and video pickers launched separately with a single MIME each:
    // on many Android devices (Android < 13 / Tecno / Infinix) a combined
    // "images and videos" picker only shows photos, so we force one type
    // per tile to guarantee videos are selectable.
    val launchPhoto = rememberMediaPickerLauncher(
        allowVideo = false,
        maxDurationSeconds = maxDurationSeconds
    ) { result -> handleResult(result) }

    val launchVideo = rememberMediaPickerLauncher(
        allowVideo = true,
        maxDurationSeconds = maxDurationSeconds,
        videoOnly = true
    ) { result -> handleResult(result) }

    val pickerContent: @Composable () -> Unit = {
        when {
            isLoading -> {
                CircularProgressIndicator(color = Green, modifier = Modifier.size(32.dp))
            }
            isVideoPreview -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(48.dp), tint = Green)
                    Text("Vidéo sélectionnée", color = Green, fontWeight = FontWeight.Bold)
                    Text("Touchez pour changer", fontSize = 11.sp, color = Color.Gray)
                }
            }
            previewBitmap != null -> {
                Image(
                    bitmap = previewBitmap!!,
                    contentDescription = "Aperçu",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            errorMessage != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 32.sp)
                    Text(errorMessage!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(40.dp), tint = Green.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 14.sp, color = Green, fontWeight = FontWeight.Medium)
                    Text("Touchez pour sélectionner ${if(allowVideo) "photo ou vidéo" else "une photo"}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }

    if (allowVideo && !isVideoPreview && previewBitmap == null && errorMessage == null) {
        // Two tiles: Photo and Vidéo, each with its own dedicated picker.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = { isLoading = true; errorMessage = null; launchPhoto() },
                modifier = modifier.weight(1f).height(140.dp),
                shape = RoundedCornerShape(16.dp),
                color = GreenSurface
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(36.dp), tint = Green.copy(alpha = 0.6f))
                    Spacer(Modifier.height(6.dp))
                    Text("Photo", fontSize = 14.sp, color = Green, fontWeight = FontWeight.Medium)
                    Text("Touchez pour choisir", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Surface(
                onClick = { isLoading = true; errorMessage = null; launchVideo() },
                modifier = modifier.weight(1f).height(140.dp),
                shape = RoundedCornerShape(16.dp),
                color = GreenSurface
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(36.dp), tint = Green.copy(alpha = 0.6f))
                    Spacer(Modifier.height(6.dp))
                    Text("Vidéo", fontSize = 14.sp, color = Green, fontWeight = FontWeight.Medium)
                    Text("Touchez pour choisir", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    } else {
        Surface(
            onClick = {
                isLoading = true
                errorMessage = null
                if (allowVideo && !isVideoPreview) launchPhoto() else if (allowVideo) launchVideo() else launchPhoto()
            },
            modifier = modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(16.dp),
            color = GreenSurface
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                pickerContent()
            }
        }
    }
}
