package com.tik_market.cache

import com.tik_market.data.models.Product
actual object AppCache {
    actual fun saveProducts(products: List<Product>) {}
    actual fun getProducts(): List<Product> = emptyList()
    actual fun saveCategories(categories: List<String>) {}
    actual fun getCategories(): List<String> = emptyList()
}
