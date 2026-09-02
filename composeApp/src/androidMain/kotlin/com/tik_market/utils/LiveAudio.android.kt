package com.tik_market.utils

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import androidx.core.app.ActivityCompat
import com.tik_market.AndroidChatContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private const val CHUNK_MS = 2000L // 2 seconds per audio chunk

// ── Streamer side: capture ──
private val capturing = AtomicBoolean(false)
private var captureThread: Thread? = null

actual fun startLiveAudioCapture(onChunk: (String) -> Unit) {
    val activity = AndroidChatContext.currentActivity ?: return

    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            500
        )
        return
    }

    if (capturing.getAndSet(true)) return

    captureThread = thread(name = "live-audio-capture", isDaemon = true) {
        while (capturing.get()) {
            val file = File.createTempFile("live_audio_", ".mp4")
            var recorder: MediaRecorder? = null
            try {
                recorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioChannels(1)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(64000)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                // Record for CHUNK_MS.
                val start = System.currentTimeMillis()
                while (capturing.get() && System.currentTimeMillis() - start < CHUNK_MS) {
                    Thread.sleep(100)
                }
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
                recorder = null

                val bytes = file.readBytes()
                if (bytes.isNotEmpty()) {
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    onChunk(b64)
                }
            } catch (e: Exception) {
                // Ignore and continue.
            } finally {
                try { recorder?.release() } catch (_: Exception) {}
                try { file.delete() } catch (_: Exception) {}
            }
        }
    }
}

actual fun stopLiveAudioCapture() {
    capturing.set(false)
    captureThread?.interrupt()
    captureThread = null
}

// ── Spectator side: playback ──
private var player: MediaPlayer? = null
private var pendingQueue = ArrayDeque<String>()

actual fun playLiveAudioChunk(base64: String) {
    try {
        val activity = AndroidChatContext.currentActivity ?: return
        // If a player is already playing, queue the chunk to play after.
        if (player?.isPlaying == true) {
            pendingQueue.addLast(base64)
            return
        }
        playChunk(base64)
    } catch (_: Exception) {}
}

private fun playChunk(base64: String) {
    try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val tempFile = File.createTempFile("live_audio_play_", ".mp4")
        tempFile.writeBytes(bytes)

        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(tempFile.absolutePath)
            setOnPreparedListener { start() }
            setOnCompletionListener {
                release()
                player = null
                try { tempFile.delete() } catch (_: Exception) {}
                // Play next queued chunk if any.
                val next = pendingQueue.removeFirstOrNull()
                if (next != null) playChunk(next)
            }
            setOnErrorListener { _, _, _ ->
                release()
                player = null
                try { tempFile.delete() } catch (_: Exception) {}
                val next = pendingQueue.removeFirstOrNull()
                if (next != null) playChunk(next)
                true
            }
            prepareAsync()
        }
    } catch (_: Exception) {}
}
