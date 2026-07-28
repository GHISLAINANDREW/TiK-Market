package com.dschangmarket

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.dschangmarket.api.TokenStorage
import com.dschangmarket.utils.NotificationUtils
import com.dschangmarket.utils.handleLocationPermissionResult

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
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val type = intent?.getStringExtra("notif_type") ?: intent?.getStringExtra("type")
        val relatedId = intent?.getIntExtra("notif_id", -1).takeIf { it != -1 } 
            ?: intent?.getIntExtra("related_id", -1).takeIf { it != -1 }
            ?: -1
        
        if (type != null && relatedId != -1) {
            // Store parameter for App.kt to consume
            com.dschangmarket.utils.setStartupParameter("notif_type", type)
            com.dschangmarket.utils.setStartupParameter("notif_id", relatedId.toString())
            // Trigger signal
            com.dschangmarket.utils.NotificationUtils.onNotificationClicked()
        }
    }

    @Deprecated("Deprecated in Activity, but needed for API < 30 compatibility")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Handle location permission result
        handleLocationPermissionResult(grantResults)
    }
}
