package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString

/**
 * Extension functions for ApiClient related to Live Shopping.
 */

suspend fun ApiClient.fetchLiveStreams(): List<ApiLiveStream> {
    return try {
        safeRequest<ApiLiveStreamsResponse>("GET", "/live/list.php").streams
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.fetchLiveComments(streamId: Int): List<ApiLiveComment> {
    return try {
        safeRequest<List<ApiLiveComment>>("GET", "/live/comments.php?stream_id=$streamId")
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.postLiveComment(streamId: Int, text: String): Boolean {
    return try {
        post("/live/comment.php", """{"stream_id":$streamId,"text":"$text"}""")
        true
    } catch (_: Exception) {
        false
    }
}

suspend fun ApiClient.startLiveStream(title: String, pinnedProductId: Int?): ApiStartLiveResponse {
    val body = json.encodeToString(ApiStartLiveBody.serializer(), ApiStartLiveBody(title, pinnedProductId))
    return safeRequest("POST", "/live/start.php", body)
}

suspend fun ApiClient.stopLiveStream(streamId: Int): Boolean {
    return try {
        post("/live/stop.php", """{"stream_id":$streamId}""")
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Uploads a JPEG frame (base64) for a live stream. Used by the streamer to
 * broadcast their camera feed to spectators (frame-based streaming).
 */
suspend fun ApiClient.uploadLiveFrame(streamId: Int, frameBase64: String): Boolean {
    return try {
        post("/live/frame.php?stream_id=$streamId", """{"frame":"$frameBase64"}""")
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Fetches the latest broadcast frame for a live stream. Returns the base64 JPEG
 * or null if no frame is available yet.
 */
suspend fun ApiClient.fetchLiveFrame(streamId: Int): String? {
    return try {
        val resp = safeRequest<ApiLiveFrameResponse>("GET", "/live/frame.php?stream_id=$streamId")
        resp.frame
    } catch (_: Exception) {
        null
    }
}

/**
 * Uploads an audio chunk (base64 AAC/MP4) for a live stream. Used by the
 * streamer to broadcast their voice to spectators.
 */
suspend fun ApiClient.uploadLiveAudio(streamId: Int, seq: Int, audioBase64: String): Boolean {
    return try {
        post("/live/audio.php?stream_id=$streamId", """{"seq":$seq,"audio":"$audioBase64"}""")
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Fetches audio chunks after a given sequence number for a live stream.
 * Used by spectators to hear the streamer's voice.
 */
suspend fun ApiClient.fetchLiveAudio(streamId: Int, afterSeq: Int): List<ApiLiveAudioChunk> {
    return try {
        safeRequest<ApiLiveAudioResponse>("GET", "/live/audio.php?stream_id=$streamId&after_seq=$afterSeq").chunks
    } catch (_: Exception) {
        emptyList()
    }
}

// ── Reels ──

suspend fun ApiClient.fetchReels(): List<ApiReel> {
    return try {
        safeRequest<ApiReelsResponse>("GET", "/reels/list.php").reels
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.likeReel(reelId: Int): Boolean {
    return try {
        post("/reels/like.php", """{"reel_id":$reelId}""")
        true
    } catch (_: Exception) {
        false
    }
}

suspend fun ApiClient.createReel(shopId: Int, videoUrl: String, description: String, productId: Int?): ApiCreateReelResponse {
    val body = json.encodeToString(ApiCreateReelBody.serializer(), ApiCreateReelBody(shopId, videoUrl, description, productId))
    return safeRequest("POST", "/reels/create.php", body)
}

suspend fun ApiClient.fetchReelComments(reelId: Int): List<ApiReelComment> {
    return try {
        safeRequest<List<ApiReelComment>>("GET", "/reels/comments.php?reel_id=$reelId")
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.postReelComment(reelId: Int, text: String): Boolean {
    return try {
        post("/reels/comment.php", """{"reel_id":$reelId,"text":"$text"}""")
        true
    } catch (_: Exception) {
        false
    }
}

suspend fun ApiClient.deleteReel(reelId: Int): Boolean {
    return try {
        post("/reels/delete.php", """{"reel_id":$reelId}""")
        true
    } catch (_: Exception) {
        false
    }
}
