package com.tik_market.utils

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.tik_market.AndroidChatContext
import java.util.Locale

// ── Pending permission callback ──
private var pendingLocationCallback: ((Location?) -> Unit)? = null
private const val LOCATION_PERMISSION_REQ = 300

/**
 * Called from MainActivity.onRequestPermissionsResult.
 * Returns true if the request code was handled.
 */
fun handleLocationPermissionResult(grantResults: IntArray): Boolean {
    val cb = pendingLocationCallback ?: return false
    pendingLocationCallback = null
    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        requestFreshLocation(cb)
    } else {
        cb(null)
    }
    return true
}

/**
 * Helper: tries getLastKnownLocation first, then requests a fresh fix with a timeout.
 * Calls onResult(location) with a valid Location or null.
 */
private fun requestFreshLocation(onResult: (Location?) -> Unit) {
    val activity = AndroidChatContext.currentActivity ?: run { onResult(null); return }
    
    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) {
        // Store callback and request permission
        pendingLocationCallback = onResult
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ)
        return
    }

    try {
        val locationManager = activity.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { onResult(null); return }
        }

        // 1) Try last known (instant)
        val lastKnown = locationManager.getLastKnownLocation(provider)
        if (lastKnown != null) {
            onResult(lastKnown)
            return
        }

        // 2) Request a fresh fix with timeout
        val handler = Handler(Looper.getMainLooper())
        var fixed = false

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (!fixed) {
                    fixed = true
                    handler.removeCallbacksAndMessages(null)
                    locationManager.removeUpdates(this)
                    onResult(loc)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())

        // Timeout after 12 seconds
        handler.postDelayed({
            if (!fixed) {
                fixed = true
                locationManager.removeUpdates(listener)
                onResult(null)
            }
        }, 12000L)

    } catch (_: Exception) {
        onResult(null)
    }
}

actual fun getCurrentLocationName(onResult: (String) -> Unit) {
    requestFreshLocation { location ->
        if (location == null) {
            onResult("Dschang")
            return@requestFreshLocation
        }
        val activity = AndroidChatContext.currentActivity
        if (activity == null) { onResult("Dschang"); return@requestFreshLocation }
        try {
            val geocoder = Geocoder(activity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val addr = addresses[0]
                val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: ""
                onResult(locality.ifBlank { "${location.latitude}, ${location.longitude}" })
            } else {
                onResult("${location.latitude}, ${location.longitude}")
            }
        } catch (_: Exception) {
            onResult("${location.latitude}, ${location.longitude}")
        }
    }
}

actual fun getCurrentLocationLatLng(onResult: (lat: Double?, lng: Double?) -> Unit) {
    requestFreshLocation { location ->
        if (location != null) {
            onResult(location.latitude, location.longitude)
        } else {
            onResult(null, null)
        }
    }
}

actual fun getPlaceName(lat: Double, lng: Double, onResult: (String) -> Unit) {
    val activity = AndroidChatContext.currentActivity ?: run { onResult("${lat.toString().take(8)}, ${lng.toString().take(8)}"); return }
    try {
        val geocoder = Geocoder(activity, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (addresses != null && addresses.isNotEmpty()) {
            val addr = addresses[0]
            val locality = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.thoroughfare ?: ""
            onResult(locality.ifBlank { "${lat.toString().take(8)}, ${lng.toString().take(8)}" })
        } else {
            onResult("${lat.toString().take(8)}, ${lng.toString().take(8)}")
        }
    } catch (_: Exception) {
        onResult("${lat.toString().take(8)}, ${lng.toString().take(8)}")
    }
}
