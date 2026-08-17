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
    // Changement du message pour confirmer le mode JS Universel
    println("[Main] Universal JS entry point started v2.3")
    
    // On attend que le DOM soit chargé
    window.onload = {
        ComposeViewport(viewportContainerId = "composeApp") {
            LaunchedEffect(Unit) {
                println("[Main] Compose ready, hiding splash screen")
                val splash = document.getElementById("splash")
                splash?.let { (it as HTMLElement).classList.add("hidden") }
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
