package com.tik_market.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.tik_market.AndroidChatContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

// ── Pending camera permission callback ──
private var pendingCameraCallback: ((Boolean) -> Unit)? = null
private const val CAMERA_PERMISSION_REQ = 400

/**
 * Called from MainActivity.onRequestPermissionsResult.
 * Returns true if the request code was handled.
 */
fun handleCameraPermissionResult(grantResults: IntArray): Boolean {
    val cb = pendingCameraCallback ?: return false
    pendingCameraCallback = null
    val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    cb(granted)
    return true
}

/**
 * Requests the CAMERA runtime permission (Android 6+). Calls onResult(true/false).
 */
fun requestCameraPermission(onResult: (Boolean) -> Unit) {
    val activity = AndroidChatContext.currentActivity ?: run { onResult(false); return }
    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED
    ) {
        onResult(true)
        return
    }
    pendingCameraCallback = onResult
    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQ)
}

// ── Per-SurfaceView camera + capture state ──
private val cameraRefs = ConcurrentHashMap<SurfaceView, android.hardware.Camera>()
private val captureFlags = ConcurrentHashMap<SurfaceView, AtomicBoolean>()
private val captureThreads = ConcurrentHashMap<SurfaceView, Thread>()

@Composable
actual fun CameraPreview(modifier: Modifier) {
    CameraPreviewWithFrames(modifier, captureEnabled = false, onFrame = {})
}

@Composable
actual fun CameraPreviewWithFrames(
    modifier: Modifier,
    captureEnabled: Boolean,
    onFrame: (String) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        requestCameraPermission { granted ->
                            if (!granted) return@requestCameraPermission
                            try {
                                val cam = android.hardware.Camera.open()
                                cameraRefs[this@apply] = cam
                                cam.setPreviewDisplay(holder)
                                cam.startPreview()
                            } catch (_: Exception) {
                                try { cameraRefs.remove(this@apply)?.release() } catch (_: Exception) {}
                            }
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // Default preview size is fine.
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        stopFrameCapture(this@apply)
                        try { cameraRefs.remove(this@apply)?.release() } catch (_: Exception) {}
                    }
                })
            }
        },
        modifier = modifier,
        update = { sv ->
            if (captureEnabled) {
                startFrameCapture(sv, onFrame)
            } else {
                stopFrameCapture(sv)
            }
        }
    )
}

private fun startFrameCapture(sv: SurfaceView, onFrame: (String) -> Unit) {
    if (captureThreads.containsKey(sv)) return
    val cam = cameraRefs[sv] ?: return
    val flag = AtomicBoolean(true)
    captureFlags[sv] = flag

    val t = thread(name = "live-frame-capture", isDaemon = true) {
        var lastMs = 0L
        while (flag.get()) {
            val now = System.currentTimeMillis()
            if (now - lastMs >= 1000) { // ~1 fps
                lastMs = now
                try {
                    val jpeg = captureJpeg(cam)
                    if (jpeg != null) {
                        val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
                        onFrame(b64)
                    }
                } catch (_: Exception) {}
            }
            try { Thread.sleep(200) } catch (_: InterruptedException) { break }
        }
    }
    captureThreads[sv] = t
}

private fun stopFrameCapture(sv: SurfaceView) {
    captureFlags.remove(sv)?.set(false)
    captureThreads.remove(sv)?.interrupt()
}

private fun captureJpeg(camera: android.hardware.Camera): ByteArray? {
    var result: ByteArray? = null
    val latch = CountDownLatch(1)
    try {
        camera.takePicture(null, null, object : android.hardware.Camera.PictureCallback {
            override fun onPictureTaken(data: ByteArray?, camera: android.hardware.Camera) {
                result = data
                try { camera.startPreview() } catch (_: Exception) {}
                latch.countDown()
            }
        })
        latch.await(2, TimeUnit.SECONDS)
    } catch (_: Exception) {
        try { camera.startPreview() } catch (_: Exception) {}
    }
    return result
}
