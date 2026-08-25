package com.tik_market.utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Villes couvertes par l'application, avec les coordonnées GPS du centre-ville.
 */
data class AppCity(
    val name: String,
    val lat: Double,
    val lng: Double,
    val marketName: String? = null
)

val appCities: List<AppCity> = listOf(
    AppCity("Dschang", 5.4440, 10.0533, "DschangMarket"),
    AppCity("Bafoussam", 5.4482, 10.4171, "Fu'sapMarket"),
    AppCity("Douala", 4.0511, 9.7679, "DoualaMarket"),
    AppCity("Yaoundé", 3.8480, 11.5021, "YaoundeMarket"),
    AppCity("Bamenda", 5.9631, 10.1591, "BamendaMarket"),
    AppCity("Garoua", 9.3019, 13.3977, "GarouaMarket"),
    AppCity("Maroua", 10.5973, 14.3157, "MarouaMarket"),
    AppCity("Ngaoundéré", 7.3276, 13.5847, "AdamaouaMarket"),
    AppCity("Kribi", 2.9506, 9.9120, "KribiMarket"),
    AppCity("Limbé", 4.0242, 9.2202, "LimbeMarket"),
    AppCity("Buea", 4.1541, 9.2311, "BueaMarket"),
    AppCity("Bertoua", 4.5772, 13.6846, "BertouaMarket"),
    AppCity("Ebolowa", 2.9234, 11.1554, "EbolowaMarket"),
    AppCity("Foumban", 5.7276, 10.8901, "FoumbanMarket"),
    AppCity("Bangangté", 5.1439, 10.5255, "BangangteMarket"),
    AppCity("Nkongsamba", 4.9547, 9.9367, "NkongsambaMarket"),
    AppCity("Mbouda", 5.6267, 10.2520, "MboudaMarket"),
    AppCity("Edéa", 3.8016, 10.1246, "EdeaMarket")
)

/** Rayon (km) en dessous duquel on considère que l'utilisateur est « dans » la ville de l'app. */
const val NEARBY_CITY_RADIUS_KM = 20.0

/** Distance (km) entre deux points GPS (formule de Haversine). */
fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
        sin(dLng / 2) * sin(dLng / 2)
    return 2 * earthRadiusKm * asin(sqrt(a))
}

/**
 * Retourne la ville de l'app la plus proche de la position donnée.
 */
fun findNearbyAppCity(
    lat: Double,
    lng: Double,
    maxRadiusKm: Double = NEARBY_CITY_RADIUS_KM
): AppCity? {
    var nearest: AppCity? = null
    var nearestDist = Double.MAX_VALUE
    for (city in appCities) {
        val d = distanceKm(lat, lng, city.lat, city.lng)
        if (d < nearestDist) {
            nearestDist = d
            nearest = city
        }
    }
    return if (nearest != null && nearestDist <= maxRadiusKm) nearest else null
}

/** Nom du marché affiché pour une ville de l'app (ou "TiK-Market" par défaut). */
fun marketNameForCity(cityName: String?): String {
    if (cityName == null) return "TiK-Market"
    val match = appCities.firstOrNull { it.name.equals(cityName, ignoreCase = true) || cityName.contains(it.name, ignoreCase = true) }
    return match?.marketName ?: "TiK-Market"
}
