package com.tik_market.ui.chat

expect fun playChatSound()

expect fun playAudio(url: String, onProgress: (Float) -> Unit = {}, onCompletion: () -> Unit = {})
expect fun stopAudio()

expect fun startVoiceRecording()

expect fun stopVoiceRecording(onResult: (String?, Int) -> Unit) // returns base64 and duration

expect fun pickImage(onResult: (String?) -> Unit)

expect fun takePhoto(onResult: (String?) -> Unit)

expect fun pickFile(onResult: (String?) -> Unit)

/** Opens a URL (e.g. Google Maps link) in the default browser. */
expect fun openUrl(url: String)
