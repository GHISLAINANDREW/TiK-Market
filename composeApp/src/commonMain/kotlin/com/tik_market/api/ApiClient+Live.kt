package com.tik_market.api

import com.tik_market.api.dto.*

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
