package com.tik_market.ui.onboarding

import android.content.Context

actual object OnboardingManager {
    private const val PREFS_NAME = "tikmarket_onboarding"
    private const val KEY_DONE = "onboarding_done"

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun isFirstLaunch(): Boolean {
        return prefs?.getBoolean(KEY_DONE, false) != true
    }

    actual fun markOnboardingComplete() {
        prefs?.edit()?.putBoolean(KEY_DONE, true)?.apply()
    }
}
