package com.tik_market.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tik_market.utils.ConnectionQuality

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
 *
 * @param quality Current connection quality level — the capture thread adapts
 *   frame rate and JPEG compression accordingly.
 */
@Composable
expect fun CameraPreviewWithFrames(
    modifier: Modifier,
    captureEnabled: Boolean,
    quality: ConnectionQuality = ConnectionQuality.GOOD,
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
