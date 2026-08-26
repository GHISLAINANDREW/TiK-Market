package com.tik_market.api

import com.tik_market.api.dto.ApiMessage
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

object ChatWebSocketClient {
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val _incomingMessages = MutableSharedFlow<ApiMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val wsScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun connect() {
        val token = ApiClient.getToken() ?: return
        if (session != null && session?.isActive == true) return

        wsScope.launch {
            try {
                val host = ApiClient.baseUrl.replace("https://", "").replace("http://", "").removeSuffix("/api")
                val protocol = if (ApiClient.baseUrl.startsWith("https")) "wss" else "ws"
                
                client.webSocket(
                    method = io.ktor.http.HttpMethod.Get,
                    host = host,
                    path = "/ws/chat?token=$token"
                ) {
                    session = this
                    println("[WS] Connected to chat")
                    
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            try {
                                val text = frame.readText()
                                val msg = Json.decodeFromString<ApiMessage>(text)
                                _incomingMessages.emit(msg)
                            } catch (e: Exception) {
                                println("[WS] Error decoding message: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("[WS] Connection failed: ${e.message}")
                delay(5000)
                connect() // Auto-reconnect
            }
        }
    }

    fun disconnect() {
        wsScope.launch {
            session?.close()
            session = null
        }
    }
}
