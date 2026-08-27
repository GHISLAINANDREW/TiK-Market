package com.tik_market.ui.home

import androidx.compose.runtime.*
import com.tik_market.api.ApiClient
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.data.models.Product
import com.tik_market.ui.story.StoryItem
import com.tik_market.cache.AppCache
import com.tik_market.utils.safeApiCall
import kotlinx.coroutines.*

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val stories: List<StoryItem> = emptyList(),
    val heroItems: List<ApiHeroItem> = emptyList(),
    val wishlistIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val scope: CoroutineScope,
    private val initialProducts: List<Product> = emptyList(),
    private val initialCategories: List<String> = emptyList(),
    private val initialWishlist: Set<Int> = emptySet(),
    private val onCacheData: (List<Product>, List<String>, Set<Int>) -> Unit = { _, _, _ -> }
) {
    var state by mutableStateOf(
        HomeUiState(
            products = initialProducts,
            categories = initialCategories,
            wishlistIds = initialWishlist,
            isLoading = initialProducts.isEmpty()
        )
    )
        private set

    // Filtres
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf<String?>(null)
    var minPrice by mutableStateOf("")
    var maxPrice by mutableStateOf("")
    var sortBy by mutableStateOf("newest")
    
    // Ville et localisation
    var userLocationName by mutableStateOf<String?>(null)
    var marketName by mutableStateOf("TiK-Market")

    fun onSearchQueryChange(query: String) {
        searchQuery = query
    }

    fun onCategoryChange(category: String?) {
        selectedCategory = category
    }

    fun setLocation(city: String?) {
        userLocationName = city
        marketName = com.tik_market.utils.marketNameForCity(city)
    }

    fun refresh(isLoggedIn: Boolean) {
        scope.launch {
            state = state.copy(isRefreshing = true)
            loadAll(isLoggedIn, force = true)
            state = state.copy(isRefreshing = false)
        }
    }

    fun loadAll(isLoggedIn: Boolean, force: Boolean = false) {
        scope.launch {
            // 1. Chargement immédiat du cache
            val cachedProducts = AppCache.getProducts()
            val cachedCategories = AppCache.getCategories()
            
            if (cachedProducts.isNotEmpty() || cachedCategories.isNotEmpty()) {
                state = state.copy(
                    products = cachedProducts,
                    categories = cachedCategories,
                    isLoading = false
                )
            } else if (force) {
                state = state.copy(isLoading = true)
            }
            
            // 2. Récupération réseau en arrière-plan
            val productsJob = launch { loadProducts(isLoggedIn) }
            val storiesJob = launch { loadStories() }
            val wishlistJob = launch { if (isLoggedIn) loadWishlist() }
            val heroJob = launch { try { val items = ApiClient.fetchHeroItems(); state = state.copy(heroItems = items) } catch (_: Exception) {} }
            val categoriesJob = launch { 
                try { 
                    val cats = ApiClient.fetchCategories()
                    state = state.copy(categories = cats)
                    AppCache.saveCategories(cats)
                    onCacheData(state.products, state.categories, state.wishlistIds)
                } catch (_: Exception) {}
            }
            
            joinAll(productsJob, storiesJob, wishlistJob, heroJob, categoriesJob)
            state = state.copy(isLoading = false)
        }
    }

    suspend fun loadProducts(isLoggedIn: Boolean) {
        val minP = minPrice.toDoubleOrNull()
        val maxP = maxPrice.toDoubleOrNull()
        val cityFilter = if (isLoggedIn) userLocationName else null
        
        val result = safeApiCall {
            ApiClient.fetchProducts(
                search = searchQuery.ifBlank { null },
                category = selectedCategory,
                minPrice = minP,
                maxPrice = maxP,
                sortBy = sortBy,
                location = cityFilter
            )
        }
        
        if (result.isSuccess) {
            val products = result.getOrDefault(emptyList()).map { it.toProduct() }.filter { !it.isStory }
            state = state.copy(products = products)
            
            // Sauvegarder dans le cache local
            if (searchQuery.isBlank() && selectedCategory == null) {
                AppCache.saveProducts(products)
            }
            
            onCacheData(state.products, state.categories, state.wishlistIds)
        } else {
            val err = (result as? com.tik_market.utils.ApiResult.Error)?.message
            state = state.copy(error = err)
        }
    }

    suspend fun loadStories() {
        try {
            val apiStories = ApiClient.fetchStories(replies = true)
            val cleanBase = ApiClient.baseUrl.trimEnd('/')
            
            val stories = apiStories.map { apiStory ->
                fun cleanUrl(url: String?): String? {
                    if (url == null || url.isBlank()) return null
                    if (url.startsWith("http")) return url
                    return "$cleanBase/${url.trimStart('/', '\\').replace("\\", "/")}"
                }

                val finalMediaUrl = if (apiStory.mediaUrl.startsWith("http") || apiStory.mediaType == "text") 
                    apiStory.mediaUrl else "$cleanBase/${apiStory.mediaUrl.trimStart('/', '\\').replace("\\", "/")}"

                StoryItem(
                    title = apiStory.shopName.ifBlank { apiStory.userName },
                    subtitle = "",
                    imageUrl = finalMediaUrl,
                    storyId = apiStory.id,
                    shopId = apiStory.shopId,
                    mediaType = apiStory.mediaType,
                    caption = apiStory.caption,
                    userId = apiStory.userId,
                    userAvatar = cleanUrl(apiStory.userAvatar),
                    shopLogo = cleanUrl(apiStory.shopLogo)
                )
            }
            state = state.copy(stories = stories)
        } catch (_: Exception) {}
    }

    suspend fun loadWishlist() {
        val result = safeApiCall { ApiClient.fetchWishlist() }
        if (result.isSuccess) {
            val ids = result.getOrDefault(emptyList()).map { it.id }.toSet()
            state = state.copy(wishlistIds = ids)
            onCacheData(state.products, state.categories, state.wishlistIds)
        }
    }

    fun toggleFavorite(productId: Int) {
        val isFav = productId in state.wishlistIds
        val newWishlist = if (isFav) state.wishlistIds - productId else state.wishlistIds + productId
        state = state.copy(wishlistIds = newWishlist)
        onCacheData(state.products, state.categories, state.wishlistIds)
        
        scope.launch {
            safeApiCall {
                if (isFav) ApiClient.removeFromWishlist(productId) else ApiClient.addToWishlist(productId)
            }
        }
    }
    
    fun clearError() {
        state = state.copy(error = null)
    }
}
