package com.dschangmarket.utils

expect fun getCurrentLocationName(onResult: (String) -> Unit)

/**
 * Gets the current GPS coordinates. Calls onResult with lat, lng (or null if unavailable).
 */
expect fun getCurrentLocationLatLng(onResult: (lat: Double?, lng: Double?) -> Unit)
