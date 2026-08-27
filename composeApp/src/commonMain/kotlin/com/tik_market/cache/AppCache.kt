package com.tik_market.cache

import com.tik_market.data.models.Product

expect object AppCache {
    fun saveProducts(products: List<Product>)
    fun getProducts(): List<Product>
    fun saveCategories(categories: List<String>)
    fun getCategories(): List<String>
}
