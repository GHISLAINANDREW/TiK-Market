package com.tik_market.ui.onboarding

expect object OnboardingManager {
    fun isFirstLaunch(): Boolean
    fun markOnboardingComplete()
}
