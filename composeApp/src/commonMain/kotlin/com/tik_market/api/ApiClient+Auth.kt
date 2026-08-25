package com.tik_market.api

import com.tik_market.api.dto.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Extension functions for ApiClient related to Authentication.
 */

suspend fun ApiClient.login(email: String, password: String): ApiAuthResponse {
    val body = json.encodeToString(ApiLoginBody(email, password))
    val result = safeRequest<ApiAuthResponse>("POST", ApiClient.Endpoints.LOGIN, body)
    setToken(result.token)
    setCurrentUser(result.user)
    return result
}

suspend fun ApiClient.googleLogin(idToken: String, location: String = ""): ApiAuthResponse {
    val body = buildJsonObject { 
        put("id_token", idToken) 
        if (location.isNotBlank()) put("location", location)
    }.toString()
    val result = safeRequest<ApiAuthResponse>("POST", ApiClient.Endpoints.GOOGLE_LOGIN, body)
    setToken(result.token)
    setCurrentUser(result.user)
    return result
}

suspend fun ApiClient.register(
    name: String,
    email: String,
    phone: String,
    password: String,
    role: String = "buyer"
): ApiAuthResponse {
    val body = json.encodeToString(ApiRegisterBody(name, email, phone, password, role))
    val result = safeRequest<ApiAuthResponse>("POST", ApiClient.Endpoints.REGISTER, body)
    setToken(result.token)
    setCurrentUser(result.user)
    return result
}

suspend fun ApiClient.fetchMe(): ApiUser {
    val user = safeRequest<ApiUserResponse>("GET", ApiClient.Endpoints.ME).user
    setCurrentUser(user)
    return user
}

suspend fun ApiClient.sendOtp(phone: String): ApiSendOtpResponse {
    val body = json.encodeToString(ApiSendOtpBody(phone))
    return safeRequest("POST", ApiClient.Endpoints.OTP_SEND, body)
}

suspend fun ApiClient.verifyOtp(phone: String, code: String): ApiVerifyOtpResponse {
    val body = json.encodeToString(ApiVerifyOtpBody(phone, code))
    val result = safeRequest<ApiVerifyOtpResponse>("POST", ApiClient.Endpoints.OTP_VERIFY, body)
    if (result.success && result.token.isNotBlank()) {
        setToken(result.token)
        setCurrentUser(result.user)
    }
    return result
}
