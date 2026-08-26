package com.tik_market.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.window

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect

        // Push a dummy state so the browser back button fires 'popstate'
        // instead of navigating away from the app.
        window.history.pushState(null, "")

        val onPopState: (dynamic) -> Unit = { event ->
            event.preventDefault()
            // Re-push so the user stays in the app
            window.history.pushState(null, "")
            onBack()
        }

        window.addEventListener("popstate", onPopState)

        try {
            awaitCancellation()
        } finally {
            window.removeEventListener("popstate", onPopState)
        }
    }
}
