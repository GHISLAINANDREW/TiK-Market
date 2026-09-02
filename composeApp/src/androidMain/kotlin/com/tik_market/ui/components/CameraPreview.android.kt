package com.tik_market.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.tik_market.AndroidChatContext
import com.tik_market.utils.ConnectionQuality
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

private const val TAG = "CamPreview"

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

// ── Single shared camera + per-SurfaceView capture state ──
// Only ONE camera is ever opened (multiple SurfaceViews can appear transiently
// during navigation/recomposition; opening one camera per SurfaceView breaks the
// shared camera device when one of them is released).
private val cameraLock = Any()
private var globalCamera: android.hardware.Camera? = null
private var cameraFacing = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
private val activeSurfaces = ConcurrentHashMap<SurfaceView, SurfaceHolder>()
private val captureFlags = ConcurrentHashMap<SurfaceView, AtomicBoolean>()
private val captureThreads = ConcurrentHashMap<SurfaceView, Thread>()
/** Shared quality level updated by the streaming screen. The capture thread reads it each frame. */
private val currentQuality = AtomicReference(ConnectionQuality.GOOD)

/** Switches between back and front camera (no-op if only one camera exists). */
actual fun platformSwitchCamera() {
    synchronized(cameraLock) {
        val count = android.hardware.Camera.getNumberOfCameras()
        if (count < 2) {
            Log.w(TAG, "switchCamera: only $count camera(s) available")
            return
        }
        val newFacing = if (cameraFacing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK)
            android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
        else
            android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
        cameraFacing = newFacing
        Log.i(TAG, "switchCamera -> facing=$newFacing")
        try { globalCamera?.release() } catch (_: Exception) {}
        globalCamera = null
        activeSurfaces.values.firstOrNull()?.let { ensureCameraOpen(it) }
    }
}

/** Computes the display/JPEG rotation for the given camera id (API 1). */
private fun computeRotation(cameraId: Int): Int {
    val info = android.hardware.Camera.CameraInfo()
    android.hardware.Camera.getCameraInfo(cameraId, info)
    val rotation = AndroidChatContext.currentActivity?.windowManager?.defaultDisplay?.rotation
        ?: Surface.ROTATION_0
    val degrees = when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
    return if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
        (360 - (info.orientation + degrees) % 360) % 360
    } else {
        (info.orientation - degrees + 360) % 360
    }
}

/**
 * Picks a small picture size (<= 640x480) for live frames. Keeps each JPEG
 * small (~50-100KB) so the frame-capture thread does not exhaust the heap.
 */
private fun setSmallPictureSize(params: android.hardware.Camera.Parameters) {
    val sizes = params.supportedPictureSizes ?: return
    val target = sizes
        .filter { it.width <= 640 && it.height <= 480 }
        .maxByOrNull { it.width * it.height }
    if (target != null) {
        params.setPictureSize(target.width, target.height)
        Log.i(TAG, "picture size set to ${target.width}x${target.height}")
    } else {
        Log.w(TAG, "no small picture size available, using default")
    }
}

@Composable
actual fun CameraPreview(modifier: Modifier) {
    CameraPreviewWithFrames(modifier, captureEnabled = false, onFrame = {})
}

@Composable
actual fun CameraPreviewWithFrames(
    modifier: Modifier,
    captureEnabled: Boolean,
    quality: ConnectionQuality,
    onFrame: (String) -> Unit
) {
    // Update the shared quality ref so the capture thread picks it up.
    LaunchedEffect(quality) { currentQuality.set(quality) }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        Log.i(TAG, "surfaceCreated sv=$this@apply")
                        activeSurfaces[this@apply] = holder
                        ensureCameraOpen(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        // Default preview size is fine.
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        Log.i(TAG, "surfaceDestroyed sv=$this@apply")
                        stopFrameCapture(this@apply)
                        activeSurfaces.remove(this@apply)
                        if (activeSurfaces.isEmpty()) closeCamera()
                    }
                })
            }
        },
        modifier = modifier,
        update = { sv ->
            if (captureEnabled) {
                Log.i(TAG, "update: captureEnabled=true, hasThread=${captureThreads.containsKey(sv)}, hasCam=${globalCamera != null}")
                startFrameCapture(sv, onFrame)
            } else {
                Log.i(TAG, "update: captureEnabled=false")
                stopFrameCapture(sv)
            }
        }
    )
}

private fun ensureCameraOpen(holder: SurfaceHolder) {
    requestCameraPermission { granted ->
        Log.i(TAG, "camera permission granted=$granted")
        if (!granted) return@requestCameraPermission
        synchronized(cameraLock) {
            if (globalCamera != null) {
                Log.i(TAG, "camera already open, reusing")
                return@synchronized
            }
            try {
                val cam = android.hardware.Camera.open(cameraFacing)
                globalCamera = cam
                Log.i(TAG, "Camera.open OK facing=$cameraFacing")
                // Portrait preview + JPEG rotation (fixes upside-down preview).
                val rot = computeRotation(cameraFacing)
                cam.setDisplayOrientation(rot)
                try {
                    val params = cam.parameters
                    params.setRotation(rot)
                    // Use a small picture size for live frames to keep memory + upload
                    // payload low (full-res JPEGs ~2MB/frame quickly exhaust the heap).
                    setSmallPictureSize(params)
                    cam.parameters = params
                } catch (_: Exception) {}
                cam.setPreviewDisplay(holder)
                cam.startPreview()
                Log.i(TAG, "startPreview OK rotation=$rot")
            } catch (e: Exception) {
                Log.e(TAG, "Camera.open/preview FAILED: ${e.message}")
                globalCamera = null
            }
        }
    }
}

private fun closeCamera() {
    synchronized(cameraLock) {
        Log.i(TAG, "closeCamera (no active surfaces)")
        try { globalCamera?.release() } catch (_: Exception) {}
        globalCamera = null
    }
}

private fun startFrameCapture(sv: SurfaceView, onFrame: (String) -> Unit) {
    if (captureThreads.containsKey(sv)) return
    val flag = AtomicBoolean(true)
    captureFlags[sv] = flag
    Log.i(TAG, "startFrameCapture: thread starting")

    val t = thread(name = "live-frame-capture", isDaemon = true) {
        var lastMs = 0L
        while (flag.get()) {
            val now = System.currentTimeMillis()
            val quality = currentQuality.get()
            val intervalMs = quality.captureIntervalMs
            if (now - lastMs >= intervalMs) {
                lastMs = now
                val cam = synchronized(cameraLock) { globalCamera }
                if (cam == null) {
                    Log.w(TAG, "capture: no camera available")
                } else {
                    // Adapt JPEG quality before capture.
                    try {
                        val params = cam.parameters
                        params.jpegQuality = quality.jpegQuality
                        cam.parameters = params
                    } catch (_: Exception) {}

                    try {
                        val jpeg = captureJpeg(cam)
                        if (jpeg != null) {
                            val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
                            Log.i(TAG, "frame captured ${jpeg.size} bytes [${quality.label}]")
                            onFrame(b64)
                        } else {
                            Log.w(TAG, "captureJpeg returned null (timeout?)")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "captureJpeg exception: ${e.message}")
                    }
                }
            }
            try { Thread.sleep(200) } catch (_: InterruptedException) { break }
        }
        Log.i(TAG, "capture thread exiting")
    }
    captureThreads[sv] = t
}

private fun stopFrameCapture(sv: SurfaceView) {
    Log.i(TAG, "stopFrameCapture")
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
                Log.i(TAG, "onPictureTaken: ${data?.size ?: 0} bytes")
                try { camera.startPreview() } catch (e: Exception) { Log.e(TAG, "restart preview failed: ${e.message}") }
                latch.countDown()
            }
        })
        val ok = latch.await(2, TimeUnit.SECONDS)
        if (!ok) Log.w(TAG, "takePicture callback timeout")
    } catch (e: Exception) {
        Log.e(TAG, "takePicture threw: ${e.message}")
        try { camera.startPreview() } catch (_: Exception) {}
    }
    return result
}