package com.tik_market.utils

/**
 * Live audio capture/playback for live streaming.
 *
 * Streamer side: [startLiveAudioCapture] records the microphone in short
 * chunks and invokes [onChunk] with each chunk's base64 AAC/MP4 payload.
 * [stopLiveAudioCapture] stops recording.
 *
 * Spectator side: [playLiveAudioChunk] plays a single audio chunk (base64).
 */

/** Starts capturing microphone audio in short chunks. */
expect fun startLiveAudioCapture(onChunk: (String) -> Unit)

/** Stops microphone audio capture. */
expect fun stopLiveAudioCapture()

/** Plays a single base64 audio chunk (AAC/MP4). */
expect fun playLiveAudioChunk(base64: String)
