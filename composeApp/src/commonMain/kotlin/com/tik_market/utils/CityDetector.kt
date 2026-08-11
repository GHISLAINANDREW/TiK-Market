package com.tik_market.utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Villes couvertes par l'application, avec les coordonnées GPS du centre-ville.
 * Utilisées pour détecter si l'utilisateur est proche d'une ville de l'app.
 */
data class AppCity(
    val name: String,
    val lat: Double,
    val lng: Double
)

val appCities: List<AppCity> = listOf(
    AppCity("Dschang", 5.4440, 10.0533),
    AppCity("Bafoussam", 5.4482, 10.4171),
    AppCity("Douala", 4.0511, 9.7679),
    AppCity("Yaoundé", 3.8480, 11.5021),
    AppCity("Bamenda", 5.9631, 10.1591)
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
 * Retourne la ville de l'app la plus proche de la position donnée,
 * si elle est à moins de [maxRadiusKm] km. Sinon null (lieu hors villes de l'app).
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
fun marketNameForCity(cityName: String): String = when {
    cityName.contains("Bafoussam", ignoreCase = true) -> "Fu'sapMarket"
    cityName.contains("Dschang", ignoreCase = true) -> "DschangMarket"
    cityName.contains("Douala", ignoreCase = true) -> "DoualaMarket"
    cityName.contains("Yaoundé", ignoreCase = true) || cityName.contains("Yaounde", ignoreCase = true) -> "YaoundeMarket"
    cityName.contains("Bamenda", ignoreCase = true) -> "BamendaMarket"
    else -> "TiK-Market"
}
