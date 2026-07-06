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

actual fun playChatSound() {
    try {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    } catch (_: Exception) {}
}

private var mediaPlayer: MediaPlayer? = null

actual fun playAudio(url: String) {
    try {
        mediaPlayer?.release()
        mediaPlayer = null

        val dataSource = if (url.startsWith("data:")) {
            // Write data URL to a temp file and play it
            val base64 = url.substringAfter(",")
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val ext = if (url.contains("audio/mp4")) ".mp4" else if (url.contains("audio/webm")) ".webm" else ".bin"
            val tempFile = File.createTempFile("audio_", ext)
            tempFile.writeBytes(bytes)
            tempFile.absolutePath
        } else {
            url
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(dataSource)
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ ->
                    mediaPlayer?.release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                release()
                mediaPlayer = null
            }
        }
    } catch (_: Exception) {}
}

private var mediaRecorder: MediaRecorder? = null
private var audioFile: File? = null
private var startTime: Long = 0

actual fun startVoiceRecording() {
    val activity = AndroidChatContext.currentActivity ?: return

    // Check RECORD_AUDIO permission - if not granted, request it (async)
    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            200
        )
        return // The user will need to re-press after granting
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
            // Return a proper data URL so the API handles it the same as web
            onResult("data:audio/mp4;base64,$base64", duration)
        } else {
            onResult(null, 0)
        }

        // Cleanup temp file
        try { audioFile?.delete() } catch (_: Exception) {}
        audioFile = null
    } catch (e: Exception) {
        onResult(null, 0)
    }
}

actual fun pickImage(onResult: (String?) -> Unit) {
    // No-op: use rememberImagePickerLauncher composable instead
    onResult(null)
}

actual fun takePhoto(onResult: (String?) -> Unit) {
    // No-op: use rememberTakePhotoLauncher composable instead
    onResult(null)
}

actual fun pickFile(onResult: (String?) -> Unit) {
    // No-op: use rememberPickFileLauncher composable instead
    onResult(null)
}

actual fun openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AndroidChatContext.currentActivity?.startActivity(intent)
    } catch (_: Exception) {}
}
