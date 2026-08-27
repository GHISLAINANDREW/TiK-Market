package com.tik_market.cache

import com.tik_market.data.models.Product
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val json = Json { ignoreUnknownKeys = true }

actual object AppCache {
    actual fun saveProducts(products: List<Product>) {
        try {
            val data = json.encodeToString(products)
            LocalCache.putJson("products", data, 60)
        } catch (_: Exception) {}
    }

    actual fun getProducts(): List<Product> {
        return try {
            val raw = LocalCache.getJson("products") ?: return emptyList()
            json.decodeFromString<List<Product>>(raw)
        } catch (_: Exception) { emptyList() }
    }

    actual fun saveCategories(categories: List<String>) {
        try {
            val data = json.encodeToString(categories)
            LocalCache.putJson("categories", data, 60)
        } catch (_: Exception) {}
    }

    actual fun getCategories(): List<String> {
        return try {
            val raw = LocalCache.getJson("categories") ?: return emptyList()
            json.decodeFromString<List<String>>(raw)
        } catch (_: Exception) { emptyList() }
    }
}
