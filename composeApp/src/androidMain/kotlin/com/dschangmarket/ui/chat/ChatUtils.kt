package com.dschangmarket.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import androidx.core.app.ActivityCompat
import com.dschangmarket.AndroidChatContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.*

actual fun playChatSound() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    } catch (_: Exception) {}
}

private var mediaPlayer: MediaPlayer? = null
private var progressJob: Job? = null

actual fun stopAudio() {
    try {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    } catch (_: Exception) {}
}

actual fun playAudio(url: String, onProgress: (Float) -> Unit, onCompletion: () -> Unit) {
    try {
        val activity = AndroidChatContext.currentActivity ?: return
        mediaPlayer?.release()
        mediaPlayer = null
        progressJob?.cancel()
        progressJob = null

        if (url.isBlank()) {
            showToast("Fichier audio introuvable")
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (url.startsWith("data:")) {
                    val base64 = url.substringAfter(",")
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val tempFile = File.createTempFile("audio_", ".mp4")
                    tempFile.writeBytes(bytes)
                    setDataSource(tempFile.absolutePath)
                } else {
                    val headers = mutableMapOf<String, String>()
                    headers["bypass-tunnel-reminder"] = "true"
                    setDataSource(activity, Uri.parse(url), headers)
                }

                setOnPreparedListener { 
                    start() 
                    // Start progress tracking
                    progressJob = CoroutineScope(Dispatchers.Main).launch {
                        while (isActive && isPlaying) {
                            val current = currentPosition.toFloat()
                            val total = duration.toFloat()
                            if (total > 0) onProgress(current / total)
                            delay(100)
                        }
                    }
                }

                setOnErrorListener { _, what, extra ->
                    showToast("Erreur de lecture audio ($what)")
                    onCompletion()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    true
                }

                setOnCompletionListener {
                    onProgress(1f)
                    onCompletion()
                    progressJob?.cancel()
                    mediaPlayer?.release()
                    mediaPlayer = null
                }

                prepareAsync()
            } catch (e: Exception) {
                showToast("Impossible de lire ce message")
                onCompletion()
                release()
                mediaPlayer = null
            }
        }
    } catch (e: Exception) {
        onCompletion()
    }
}

/** Show a short toast message to the user */
private fun showToast(message: String) {
    try {
        val activity = AndroidChatContext.currentActivity ?: return
        android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

private var mediaRecorder: MediaRecorder? = null
private var audioFile: File? = null
private var startTime: Long = 0

actual fun startVoiceRecording() {
    val activity = AndroidChatContext.currentActivity ?: return

    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            200
        )
        return
    }

    try {
        audioFile = File.createTempFile("voice_", ".mp4")
        startTime = System.currentTimeMillis()
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(64000)
            setOutputFile(audioFile?.absolutePath)
            prepare()
            start()
        }
    } catch (e: Exception) {
        mediaRecorder = null
    }
}

actual fun stopVoiceRecording(onResult: (String?, Int) -> Unit) {
    try {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (_: Exception) {}
            release()
        }
        mediaRecorder = null

        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val bytes = audioFile?.readBytes()
        if (bytes != null) {
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            onResult("data:audio/mp4;base64,$base64", duration)
        } else {
            onResult(null, 0)
        }

        try { audioFile?.delete() } catch (_: Exception) {}
        audioFile = null
    } catch (e: Exception) {
        onResult(null, 0)
    }
}

actual fun pickImage(onResult: (String?) -> Unit) {
    onResult(null)
}

actual fun takePhoto(onResult: (String?) -> Unit) {
    onResult(null)
}

actual fun pickFile(onResult: (String?) -> Unit) {
    onResult(null)
}

actual fun openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AndroidChatContext.currentActivity?.startActivity(intent)
    } catch (_: Exception) {}
}
