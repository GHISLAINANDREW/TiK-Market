package com.tik_market.cache

import com.tik_market.data.models.Product
import com.tik_market.db.LocalCache as DbLocalCache

actual object AppCache {
    actual fun saveProducts(products: List<Product>) = DbLocalCache.saveProducts(products)
    actual fun getProducts(): List<Product> = DbLocalCache.getProducts()
    actual fun saveCategories(categories: List<String>) = DbLocalCache.saveCategories(categories)
    actual fun getCategories(): List<String> = DbLocalCache.getCategories()
}
