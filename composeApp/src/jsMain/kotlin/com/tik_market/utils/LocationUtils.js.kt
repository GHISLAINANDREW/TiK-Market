package com.tik_market.utils

import kotlinx.browser.window

actual fun getCurrentLocationName(onResult: (String) -> Unit) {
    getCurrentLocationLatLng { lat, lng ->
        if (lat != null && lng != null) {
            getPlaceName(lat, lng, onResult)
        } else {
            onResult("Inconnu")
        }
    }
}

actual fun getCurrentLocationLatLng(onResult: (Double?, Double?) -> Unit) {
    val geo = window.navigator.asDynamic().geolocation
    if (geo != null) {
        geo.getCurrentPosition(
            { pos: dynamic -> onResult(pos.coords.latitude as Double, pos.coords.longitude as Double) },
            { onResult(null, null) },
            js("{ enableHighAccuracy: true, timeout: 10000 }")
        )
    } else {
        onResult(null, null)
    }
}

actual fun getPlaceName(lat: Double, lng: Double, onResult: (String) -> Unit) {
    val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&accept-language=fr"
    window.fetch(url).then({ response ->
        response.json().then({ data ->
            val display = data.asDynamic().display_name as? String
            if (display != null) {
                onResult(display.split(",")[0].trim())
            } else {
                onResult("Inconnu")
            }
        })
    }).catch({
        onResult("Inconnu")
    })
}
