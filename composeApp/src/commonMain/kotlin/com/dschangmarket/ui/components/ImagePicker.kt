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

data class ImagePickResult(
    val dataUrl: String,
    val fileName: String
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
    onResult: (result: ImagePickResult?) -> Unit
): () -> Unit

/**
 * Platform-specific camera/photo launcher.
 * Returns a lambda that, when invoked, opens the device camera to take a photo.
 */
@Composable
expect fun rememberTakePhotoLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit

/**
 * Platform-specific file picker launcher.
 * Returns a lambda that, when invoked, opens the device file picker for any file type.
 */
@Composable
expect fun rememberPickFileLauncher(
    onResult: (dataUrl: String?) -> Unit
): () -> Unit

@Composable
fun ImagePicker(
    currentImageUrl: String = "",
    onImagePicked: (ImagePickResult) -> Unit,
    label: String = "Photo du produit",
    modifier: Modifier = Modifier
) {
    var previewUrl by remember { mutableStateOf(currentImageUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Decode the dataUrl to ImageBitmap whenever it changes
    LaunchedEffect(previewUrl) {
        val url = previewUrl
        if (url != null) {
            if (url.startsWith("data:")) {
                previewBitmap = decodeDataUrlToImageBitmap(url)
            } else if (url.isNotBlank() && url.startsWith("http")) {
                // Load image from server URL with optimization + cache
                try {
                    previewBitmap = loadImageFromUrl(url)
                } catch (_: Exception) {
                    previewBitmap = null
                }
            } else {
                previewBitmap = null
            }
        } else {
            previewBitmap = null
        }
    }

    // Platform-specific image picker launcher
    val launchPicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            previewUrl = result.dataUrl
            onImagePicked(result)
            errorMessage = null
        } else {
            errorMessage = null // cancelled, not an error
        }
        isLoading = false
    }

    Surface(
        onClick = {
            isLoading = true
            errorMessage = null
            launchPicker()
        },
        modifier = modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(16.dp),
        color = GreenSurface
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = Green, modifier = Modifier.size(32.dp))
                }
                previewBitmap != null -> {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = "Aperçu du produit",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                previewUrl.isNotBlank() -> {
                    // From server URL, not base64 dataUrl — treat as "has image"
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(80.dp).background(Color.White, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\uD83D\uDDBC\uFE0F", fontSize = 36.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Image existante", fontSize = 13.sp, color = Green, fontWeight = FontWeight.Medium)
                        Text("Touchez pour changer", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                errorMessage != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 32.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(errorMessage!!, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(40.dp), tint = Green.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        Text(label, fontSize = 14.sp, color = Green, fontWeight = FontWeight.Medium)
                        Text("Touchez pour sélectionner", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
