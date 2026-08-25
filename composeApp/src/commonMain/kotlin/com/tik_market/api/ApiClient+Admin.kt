package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Administrative operations for Tik-Market API.
 */

suspend fun ApiClient.fetchOnlineUsers(): ApiOnlineUsersResponse {
    return safeRequest<ApiOnlineUsersResponse>("GET", "/admin/online.php")
}

suspend fun ApiClient.fetchAdminUsers(): List<ApiAdminUser> {
    return safeRequest<ApiAdminUsersResponse>("GET", ApiClient.Endpoints.ADMIN_USERS).users
}

suspend fun ApiClient.fetchAdminShops(): List<ApiAdminShop> {
    return try {
        safeRequest<ApiAdminShopsResponse>("GET", ApiClient.Endpoints.ADMIN_SHOPS).shops
    } catch (e: Exception) {
        try {
            safeRequest<List<ApiAdminShop>>("GET", ApiClient.Endpoints.ADMIN_SHOPS)
        } catch (e2: Exception) {
            emptyList()
        }
    }
}

suspend fun ApiClient.addUser(name: String, email: String, phone: String, password: String, role: String = "buyer", managedCity: String? = null) {
    val body = buildJsonObject {
        put("name", name)
        put("email", email)
        put("phone", phone)
        put("password", password)
        put("role", role)
        managedCity?.let { put("managed_city", it) }
    }.toString()
    post(ApiClient.Endpoints.ADMIN_USERS, body)
}

suspend fun ApiClient.deleteUser(userId: Int) {
    delete("${ApiClient.Endpoints.ADMIN_USERS}?id=$userId")
}

suspend fun ApiClient.banUser(userId: Int, status: String = "banned") {
    put("${ApiClient.Endpoints.ADMIN_USERS}?id=$userId&status=$status", "")
}

suspend fun ApiClient.toggleShopVerification(shopId: Int, verified: Boolean) {
    put("${ApiClient.Endpoints.ADMIN_SHOPS}?id=$shopId&verified=$verified", "")
}

suspend fun ApiClient.deleteShop(shopId: Int) {
    delete("${ApiClient.Endpoints.ADMIN_SHOPS}?id=$shopId")
}

suspend fun ApiClient.banShop(shopId: Int, status: String = "banned") {
    put("${ApiClient.Endpoints.ADMIN_SHOPS}?id=$shopId&status=$status", "")
}

suspend fun ApiClient.promoteShop(shopId: Int, featured: Boolean = true) {
    put("${ApiClient.Endpoints.ADMIN_SHOPS}?id=$shopId&featured=$featured", "")
}

suspend fun ApiClient.sendSystemNotification(title: String, message: String) {
    val body = buildJsonObject {
        put("title", title)
        put("message", message)
    }.toString()
    post(ApiClient.Endpoints.NOTIFICATIONS, body)
}

suspend fun ApiClient.sendIndividualNotification(userId: Int, title: String, message: String): Boolean {
    val body = buildJsonObject {
        put("user_id", userId)
        put("title", title)
        put("message", message)
    }.toString()
    val respStr = post(ApiClient.Endpoints.NOTIFICATIONS, body)
    val resp = json.parseToJsonElement(respStr).jsonObject
    return resp["success"]?.jsonPrimitive?.booleanOrNull ?: false
}

suspend fun ApiClient.fetchAdminDashboard(city: String? = null): ApiAdminDashboardResponse {
    val path = buildUrl("/admin/dashboard.php", mapOf("city" to city))
    return safeRequest<ApiAdminDashboardResponse>("GET", path)
}

suspend fun ApiClient.fetchSuperAdminData(): ApiSuperAdminResponse {
    return safeRequest("GET", ApiClient.Endpoints.SUPER_ADMIN)
}

suspend fun ApiClient.updateReportStatus(reportId: Int, status: String): Boolean {
    val body = buildJsonObject {
        put("action", "update_report_status")
        put("report_id", reportId)
        put("status", status)
    }.toString()
    val resp = post(ApiClient.Endpoints.SUPER_ADMIN, body)
    return json.parseToJsonElement(resp).jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
}

suspend fun ApiClient.broadcastSystemMessage(title: String, message: String): Boolean {
    val body = buildJsonObject {
        put("action", "broadcast_system")
        put("title", title)
        put("message", message)
    }.toString()
    val resp = post(ApiClient.Endpoints.SUPER_ADMIN, body)
    return json.parseToJsonElement(resp).jsonObject["success"]?.jsonPrimitive?.booleanOrNull ?: false
}
