package com.tik_market

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.tik_market.api.TokenStorage
import com.tik_market.ui.components.handleCameraPermissionResult
import com.tik_market.utils.NotificationUtils
import com.tik_market.utils.handleLocationPermissionResult

class MainActivity : ComponentActivity() {
    companion object {
        private const val NOTIF_PERMISSION_REQ = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidChatContext.currentActivity = this
        
        TokenStorage.init(this)
        NotificationUtils.init(this)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_PERMISSION_REQ)
            }
        }

        setContent {
            App(onExit = { finish() })
        }

        // Handle intent if activity is already running
        handleIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Crucial: updates activity intent so new notification extras are read
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        
        val type = intent.getStringExtra("notif_type") 
            ?: intent.getStringExtra("type") 
            ?: if (intent.hasExtra("notif_id") || intent.hasExtra("sender_id") || intent.hasExtra("user_id") || intent.hasExtra("related_id")) "message" else null
        
        val relatedIdStr = intent.getStringExtra("notif_id") 
            ?: intent.getIntExtra("notif_id", -1).takeIf { it != -1 }?.toString()
            ?: intent.getStringExtra("sender_id") 
            ?: intent.getIntExtra("sender_id", -1).takeIf { it != -1 }?.toString()
            ?: intent.getStringExtra("user_id") 
            ?: intent.getIntExtra("user_id", -1).takeIf { it != -1 }?.toString()
            ?: intent.getStringExtra("related_id")
            ?: intent.getIntExtra("related_id", -1).takeIf { it != -1 }?.toString()

        // If neither type nor relatedId is present, this is a normal app launch from home screen. Ignore!
        if (type == null && relatedIdStr == null) return

        val finalType = type ?: "message"
        val finalId = relatedIdStr ?: "0"

        com.tik_market.utils.setStartupParameter("notif_type", finalType)
        com.tik_market.utils.setStartupParameter("notif_id", finalId)
        com.tik_market.utils.NotificationUtils.onNotificationClicked()
    }

    @Deprecated("Deprecated in Activity, but needed for API < 30 compatibility")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Handle location permission result
        handleLocationPermissionResult(grantResults)
        // Handle camera permission result
        handleCameraPermissionResult(grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val text = results[0]
                com.tik_market.ui.chat.handleSpeechResult(text)
            }
        }
    }
}
