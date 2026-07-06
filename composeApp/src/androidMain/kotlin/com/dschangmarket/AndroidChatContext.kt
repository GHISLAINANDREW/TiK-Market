package com.dschangmarket

import android.app.Activity

/**
 * Global reference to the current Android Activity for use in ChatUtils actual functions.
 * Set in MainActivity.onCreate() and cleared on destroy.
 */
object AndroidChatContext {
    var currentActivity: Activity? = null
}
