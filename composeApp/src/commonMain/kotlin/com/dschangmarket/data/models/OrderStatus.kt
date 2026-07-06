package com.dschangmarket.data.models

enum class OrderStatus(val label: String) {
    PENDING("En attente"),
    CONFIRMED("Confirmée"),
    PREPARING("En préparation"),
    DELIVERING("En livraison"),
    DELIVERED("Livrée"),
    CANCELLED("Annulée")
}
