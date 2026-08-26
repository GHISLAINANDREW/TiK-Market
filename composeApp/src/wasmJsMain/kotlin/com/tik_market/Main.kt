package com.tik_market

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import com.tik_market.api.ApiClient
import com.tik_market.navigation.NavScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("[Main] Wasm entry point started")

    // On web, use relative /api path so the server proxy (Vercel/webpack) forwards
    // requests to the real backend. Absolute URLs bypass the proxy entirely.
    ApiClient.baseUrl = "/api"
    println("[Main] ApiClient.baseUrl = ${ApiClient.baseUrl}")
    
    // Safety fallback: hide loading screen after 10s even if Compose fails
    window.setTimeout({
        val loadingScreen = document.getElementById("splash")
        if (loadingScreen != null) {
            val element = loadingScreen as HTMLElement
            if (!element.classList.contains("hidden")) {
                println("[Main] Safety timeout: hiding splash screen")
                element.classList.add("hidden")
            }
        }
        null
    }, 10000)

    ComposeViewport(viewportContainerId = "composeApp") {
        LaunchedEffect(Unit) {
            println("[Main] Compose ready, hiding splash screen")
            val loadingScreen = document.getElementById("splash")
            if (loadingScreen != null) {
                (loadingScreen as HTMLElement).classList.add("hidden")
                println("[Main] Splash screen hidden")
            } else {
                println("[Main] ERROR: splash element not found")
            }
        }
        App(
            onExit = {
                println("[Main] Closing application")
                window.close()
                // Fallback for some browsers that prevent window.close()
                window.location.href = "about:blank"
            },
            initialScreen = NavScreen.Splash
        )
    }
}
