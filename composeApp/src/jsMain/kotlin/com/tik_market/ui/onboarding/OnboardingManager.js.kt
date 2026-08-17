package com.tik_market.ui.onboarding

import kotlinx.browser.localStorage

actual object OnboardingManager {
    private const val KEY = "onboarding_complete"

    actual fun isFirstLaunch(): Boolean {
        return localStorage.getItem(KEY) != "true"
    }

    actual fun markOnboardingComplete() {
        localStorage.setItem(KEY, "true")
    }
}
