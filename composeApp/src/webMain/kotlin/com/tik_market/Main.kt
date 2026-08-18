package com.tik_market

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import com.tik_market.navigation.NavScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    println("[Main] Web entry point started v2.4")
    
    window.onload = {
        ComposeViewport(viewportContainerId = "composeApp") {
            LaunchedEffect(Unit) {
                val splash = document.getElementById("splash")
                if (splash != null) {
                    (splash as HTMLElement).classList.add("hidden")
                }
            }
            App(
                onExit = {
                    window.close()
                    window.location.href = "about:blank"
                },
                initialScreen = NavScreen.Home
            )
        }
    }
}
