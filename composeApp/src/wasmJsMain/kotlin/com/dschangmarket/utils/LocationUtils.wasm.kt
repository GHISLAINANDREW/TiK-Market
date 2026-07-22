package com.dschangmarket.utils

actual fun getCurrentLocationName(onResult: (String) -> Unit) {
    getCurrentPositionJs { lat, lng ->
        if (lat != null && lng != null) {
            reverseGeocodeJs(lat, lng) { displayName ->
                if (displayName != null) {
                    onResult(displayName)
                } else {
                    onResult("$lat, $lng")
                }
            }
        } else {
            onResult("Dschang")
        }
    }
}

actual fun getCurrentLocationLatLng(onResult: (lat: Double?, lng: Double?) -> Unit) {
    getCurrentPositionJs { latStr, lngStr ->
        val lat = latStr?.toDoubleOrNull()
        val lng = lngStr?.toDoubleOrNull()
        onResult(lat, lng)
    }
}

actual fun getPlaceName(lat: Double, lng: Double, onResult: (String) -> Unit) {
    reverseGeocodeJs(lat.toString(), lng.toString()) { displayName ->
        onResult(displayName ?: "${lat.toString().take(8)}, ${lng.toString().take(8)}")
    }
}

@JsFun("""(callback) => {
    if (!navigator.geolocation) {
        callback(null, null);
        return;
    }
    navigator.geolocation.getCurrentPosition(
        (pos) => callback(pos.coords.latitude.toString(), pos.coords.longitude.toString()),
        () => callback(null, null),
        { enableHighAccuracy: true, timeout: 10000 }
    );
}""")
private external fun getCurrentPositionJs(callback: (String?, String?) -> Unit)

@JsFun("""(lat, lng, callback) => {
    const url = 'https://nominatim.openstreetmap.org/reverse?format=json&lat=' + lat + '&lon=' + lng + '&accept-language=fr';
    fetch(url)
        .then(r => r.json())
        .then(data => {
            if (data && data.display_name) {
                const parts = data.display_name.split(',');
                callback(parts[0].trim());
            } else {
                callback(null);
            }
        })
        .catch(() => callback(null));
}""")
private external fun reverseGeocodeJs(lat: String, lng: String, callback: (String?) -> Unit)
