package com.dschangmarket.utils

import kotlin.math.roundToInt

object FormatUtils {
    fun formatPrice(price: Double, currency: String = "FCFA"): String {
        val formatted = price.roundToInt().toString()
            .reversed()
            .chunked(3)
            .joinToString(" ")
            .reversed()
        return "$formatted $currency"
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()}m"
        } else {
            "${(meters / 1000).toString().take(4)}km"
        }
    }
}
