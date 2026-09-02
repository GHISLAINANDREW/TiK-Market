package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Miscellaneous extension functions for ApiClient.
 */

suspend fun ApiClient.fetchVendorInteractions(productId: Int): ApiVendorInteractionsResponse {
    return try {
        safeRequest<ApiVendorInteractionsResponse>("GET", "${ApiClient.Endpoints.VENDOR_INTERACTIONS}?product_id=$productId")
    } catch (_: Exception) {
        ApiVendorInteractionsResponse()
    }
}

suspend fun ApiClient.fetchShopSubscribers(shopId: Int): List<ApiInteractionUser> {
    return try {
        val resp = safeRequest<ApiVendorInteractionsResponse>("GET", "${ApiClient.Endpoints.VENDOR_INTERACTIONS}?shop_id=$shopId")
        resp.subscribers
    } catch (_: Exception) {
        emptyList()
    }
}

suspend fun ApiClient.uploadImage(dataUrl: String, fileName: String): String {
    val base64Data = dataUrl.substringAfter(",", dataUrl)
    val body = json.encodeToString(ApiUploadBody(base64Data, fileName))
    return safeRequest<ApiUploadResponse>("POST", ApiClient.Endpoints.UPLOADS, body).imageUrl
}

/**
 * Uploads a media file (base64 data URL) in chunks, reporting progress via
 * onProgress(0.0..1.0). Returns the uploaded URL. This gives a real progress
 * bar for large files (videos) instead of a single blocking POST.
 */
suspend fun ApiClient.uploadImageChunked(
    dataUrl: String,
    fileName: String,
    chunkSize: Int = 512 * 1024, // 512KB per chunk (base64 chars)
    onProgress: (Float) -> Unit = {}
): String {
    val base64Data = dataUrl.substringAfter(",", dataUrl)
    val uploadId = "up_" + kotlin.random.Random.nextLong().toString(36) + "_" + kotlin.random.Random.nextLong().toString(36)
    val totalChunks = maxOf(1, (base64Data.length + chunkSize - 1) / chunkSize)

    for (i in 0 until totalChunks) {
        val start = i * chunkSize
        val end = minOf(start + chunkSize, base64Data.length)
        val chunk = base64Data.substring(start, end)
        val body = json.encodeToString(
            ApiChunkedUploadBody(uploadId, i, totalChunks, chunk)
        )
        safeRequest<ApiChunkedUploadResponse>("POST", "/uploads/chunked.php", body)
        onProgress((i + 1).toFloat() / totalChunks)
    }

    // Finalize
    val finalBody = json.encodeToString(
        ApiChunkedFinalizeBody(uploadId, totalChunks, fileName)
    )
    val resp = safeRequest<ApiUploadResponse>("POST", "/uploads/chunked.php", finalBody)
    onProgress(1f)
    return resp.imageUrl
}

suspend fun ApiClient.submitReport(type: String, targetId: Int, reason: String, comment: String = "") {
    val body = json.encodeToString(ApiReportBody(type, targetId, reason, comment))
    post(ApiClient.Endpoints.REPORTS, body)
}

suspend fun ApiClient.updateUserProfile(
    name: String? = null,
    phone: String? = null,
    location: String? = null,
    avatar: String? = null,
    coverPhoto: String? = null,
    password: String? = null
): ApiUser {
    val body = buildJsonObject {
        name?.let { put("name", it) }
        phone?.let { put("phone", it) }
        location?.let { put("location", it) }
        avatar?.let { put("avatar", it) }
        coverPhoto?.let { put("cover_photo", it) }
        password?.let { put("password", it) }
    }.toString()
    val result = safeRequest<ApiUserResponse>("PUT", ApiClient.Endpoints.ME, body).user
    setCurrentUser(result)
    return result
}

suspend fun ApiClient.updateUserAvatar(imageUrl: String) {
    updateUserProfile(avatar = imageUrl)
}

suspend fun ApiClient.fetchShopByVendor(): ApiShop? {
    val user = getCurrentUser() ?: return null
    return try {
        safeRequest<ApiShopResponse>("GET", "${ApiClient.Endpoints.SHOPS}?vendor_id=${user.id}").shop
    } catch (_: Exception) {
        null
    }
}

suspend fun ApiClient.fetchVendorStats(): ApiVendorStatsResponse {
    return try {
        safeRequest("GET", ApiClient.Endpoints.VENDOR_STATS)
    } catch (_: Exception) {
        ApiVendorStatsResponse()
    }
}
