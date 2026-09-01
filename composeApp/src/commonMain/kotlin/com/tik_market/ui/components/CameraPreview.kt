package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific camera preview for Live Streaming.
 */
@Composable
expect fun CameraPreview(modifier: Modifier)

/**
 * Camera preview that also captures JPEG frames (base64) for live broadcasting.
 *
 * When [captureEnabled] is true, the platform implementation periodically
 * captures a frame from the camera and invokes [onFrame] with the base64 JPEG.
 * Used by the streamer to broadcast their feed to spectators.
 */
@Composable
expect fun CameraPreviewWithFrames(
    modifier: Modifier,
    captureEnabled: Boolean,
    onFrame: (String) -> Unit
)

/**
 * Switches between the back and front camera (no-op if only one camera exists).
 * Platform-specific.
 */
fun switchCamera() {
    platformSwitchCamera()
}

internal expect fun platformSwitchCamera()
