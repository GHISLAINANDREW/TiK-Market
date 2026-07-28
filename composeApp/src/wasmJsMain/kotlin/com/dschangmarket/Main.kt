package com.dschangmarket

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("[Main] Wasm entry point started")
    
    // Safety fallback: hide loading screen after 10s even if Compose fails
    window.setTimeout({
        val loadingScreen = document.getElementById("loading-screen")
        if (loadingScreen != null) {
            val element = loadingScreen as HTMLElement
            if (!element.classList.contains("hidden")) {
                println("[Main] Safety timeout: hiding loading screen")
                element.classList.add("hidden")
            }
        }
        null
    }, 10000)

    ComposeViewport(viewportContainerId = "composeApp") {
        LaunchedEffect(Unit) {
            println("[Main] Compose ready, hiding loading screen")
            val loadingScreen = document.getElementById("loading-screen")
            if (loadingScreen != null) {
                (loadingScreen as HTMLElement).classList.add("hidden")
                println("[Main] Loading screen hidden")
            } else {
                println("[Main] ERROR: loading-screen element not found")
            }
        }
        App(onExit = {
            println("[Main] Closing application")
            window.close()
            // Fallback for some browsers that prevent window.close()
            window.location.href = "about:blank"
        })
    }
}
