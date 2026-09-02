package com.tik_market.utils

// Web (JS) live audio is not supported yet — no-op implementations.
actual fun startLiveAudioCapture(onChunk: (String) -> Unit) {}
actual fun stopLiveAudioCapture() {}
actual fun playLiveAudioChunk(base64: String) {}
