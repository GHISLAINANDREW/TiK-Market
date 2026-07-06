package com.dschangmarket.ui.onboarding

expect object OnboardingManager {
    fun isFirstLaunch(): Boolean
    fun markOnboardingComplete()
}
