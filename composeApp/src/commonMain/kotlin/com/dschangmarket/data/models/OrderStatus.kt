package com.dschangmarket.data.models

import com.dschangmarket.utils.AppStrings

enum class OrderStatus(val label: String) {
    PENDING("En attente"),
    CONFIRMED("Confirmée"),
    PREPARING("En préparation"),
    DELIVERING("En livraison"),
    DELIVERED("Livrée"),
    CANCELLED("Annulée");

    fun getLocalizedLabel(strings: AppStrings): String = when (this) {
        PENDING -> strings.orderStatusPending
        CONFIRMED -> strings.orderStatusPaid
        PREPARING -> strings.orderStatusProcessing
        DELIVERING -> strings.orderStatusShipped
        DELIVERED -> strings.orderStatusDelivered
        CANCELLED -> strings.orderStatusCancelled
    }

    companion object {
        fun fromCode(code: String?): OrderStatus = when (code?.lowercase()) {
            "pending" -> PENDING
            "confirmed" -> CONFIRMED
            "preparing" -> PREPARING
            "delivering" -> DELIVERING
            "delivered" -> DELIVERED
            "cancelled" -> CANCELLED
            else -> PENDING
        }
    }
}
