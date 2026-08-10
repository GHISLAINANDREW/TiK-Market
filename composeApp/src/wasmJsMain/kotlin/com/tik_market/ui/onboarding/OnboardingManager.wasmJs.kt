package com.tik_market.ui.onboarding

import kotlinx.browser.localStorage

actual object OnboardingManager {
    private val key = "dschangmarket_onboarding_done"

    actual fun isFirstLaunch(): Boolean {
        return localStorage.getItem(key) != "true"
    }

    actual fun markOnboardingComplete() {
        localStorage.setItem(key, "true")
    }
}
