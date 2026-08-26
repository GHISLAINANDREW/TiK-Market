package com.tik_market.db

import com.tik_market.data.models.Product
import com.tik_market.data.models.toProduct
import com.tik_market.api.dto.ApiProduct
import com.tik_market.api.dto.ApiConversation
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.datetime.Clock

object LocalCache {
    private val db = DatabaseHolder.getDatabase()
    private val queries = db.tikMarketDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    fun saveProducts(products: List<Product>) {
        queries.transaction {
            queries.deleteProducts()
            products.forEach { p ->
                queries.insertProduct(
                    ProductEntity(
                        id = p.id,
                        shopId = p.shopId,
                        shopName = p.shopName,
                        shopLocation = p.shopLocation,
                        vendorId = p.vendorId,
                        vendorPhone = p.vendorPhone ?: "",
                        title = p.title,
                        description = p.description,
                        price = p.price,
                        comparePrice = p.comparePrice,
                        category = p.category,
                        images = json.encodeToString(p.images),
                        stock = p.stock.toLong(),
                        unit = p.unit,
                        rating = p.rating.toDouble(),
                        totalReviews = p.totalReviews.toLong(),
                        totalSales = p.totalSales.toLong(),
                        isVerified = p.shopVerified,
                        isStory = p.isStory,
                        userPurchaseCount = p.userPurchaseCount.toLong(),
                        cachedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun getProducts(): List<Product> {
        return queries.selectAllProducts().executeAsList().map { entity ->
            Product(
                id = entity.id,
                shopId = entity.shopId,
                shopName = entity.shopName,
                shopLocation = entity.shopLocation,
                vendorId = entity.vendorId,
                vendorPhone = entity.vendorPhone,
                title = entity.title,
                description = entity.description,
                price = entity.price,
                comparePrice = entity.comparePrice,
                category = entity.category,
                images = try { json.decodeFromString<List<String>>(entity.images) } catch (_: Exception) { emptyList() },
                stock = entity.stock.toInt(),
                unit = entity.unit,
                rating = entity.rating.toFloat(),
                totalReviews = entity.totalReviews.toInt(),
                totalSales = entity.totalSales.toInt(),
                shopVerified = entity.isVerified,
                isStory = entity.isStory,
                userPurchaseCount = entity.userPurchaseCount.toInt()
            )
        }
    }

    fun saveCategories(categories: List<String>) {
        queries.transaction {
            queries.deleteCategories()
            categories.forEach { cat ->
                queries.insertCategory(
                    CategoryEntity(
                        name = cat,
                        cachedAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun getCategories(): List<String> {
        return queries.selectAllCategories().executeAsList().map { it.name }
    }
}
