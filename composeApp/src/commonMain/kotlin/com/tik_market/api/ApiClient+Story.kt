package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString

/**
 * Extension functions for ApiClient related to Stories and Hero Section.
 */

suspend fun ApiClient.fetchStories(replies: Boolean = false): List<ApiStory> {
    val path = buildUrl(Endpoints.STORIES, mapOf("replies" to if (replies) "1" else "0"))
    return safeRequest<ApiStoriesResponse>("GET", path).stories
}

suspend fun ApiClient.fetchShopStories(shopId: Int, replies: Boolean = false): List<ApiStory> {
    val path = buildUrl(Endpoints.STORIES, mapOf(
        "shop_id" to shopId.toString(),
        "replies" to if (replies) "1" else "0"
    ))
    return safeRequest<ApiStoriesResponse>("GET", path).stories
}

suspend fun ApiClient.createStory(shopId: Int, mediaUrl: String, mediaType: String = "image", caption: String? = null, duration: Int = 0): ApiStory {
    val body = json.encodeToString(ApiCreateStoryBody(shopId, mediaUrl, mediaType, caption, duration))
    return safeRequest("POST", Endpoints.STORIES, body)
}

suspend fun ApiClient.deleteStory(storyId: Int) {
    delete("${Endpoints.STORIES}?id=$storyId")
}

suspend fun ApiClient.replyToStory(storyId: Int, text: String): ApiStoryReplyResponse {
    val body = json.encodeToString(ApiStoryReplyBody(text))
    return safeRequest("POST", "${Endpoints.STORIES}?reply=$storyId", body)
}

suspend fun ApiClient.fetchHeroItems(): List<ApiHeroItem> {
    return try {
        safeRequest<List<ApiHeroItem>>("GET", Endpoints.HERO)
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.createHeroItem(body: ApiCreateHeroBody): ApiHeroItem {
    return safeRequest("POST", Endpoints.HERO, json.encodeToString(body))
}

suspend fun ApiClient.deleteHeroItem(id: Int) {
    delete("${Endpoints.HERO}?id=$id")
}
