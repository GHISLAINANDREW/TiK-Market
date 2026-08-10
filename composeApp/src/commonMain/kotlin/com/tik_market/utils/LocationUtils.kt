package com.tik_market.utils

expect fun getCurrentLocationName(onResult: (String) -> Unit)

/**
 * Gets the current GPS coordinates. Calls onResult with lat, lng (or null if unavailable).
 */
expect fun getCurrentLocationLatLng(onResult: (lat: Double?, lng: Double?) -> Unit)

/**
 * Gets a human-readable name for a given location.
 */
expect fun getPlaceName(lat: Double, lng: Double, onResult: (String) -> Unit)
