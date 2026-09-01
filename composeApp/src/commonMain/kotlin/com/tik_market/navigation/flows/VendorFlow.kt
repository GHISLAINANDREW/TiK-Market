package com.tik_market.navigation.flows

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.tik_market.api.ApiClient
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.data.models.Product
import com.tik_market.navigation.AppState
import com.tik_market.navigation.NavScreen
import com.tik_market.ui.profile.FollowedShopsScreen
import com.tik_market.ui.vendor.AddProductScreen
import com.tik_market.ui.vendor.CreateShopScreen
import com.tik_market.ui.vendor.ManageOrdersScreen
import com.tik_market.ui.vendor.ManageShopScreen
import com.tik_market.ui.vendor.ProductForm
import com.tik_market.ui.vendor.SubscribersScreen
import com.tik_market.ui.vendor.VendorDashboardScreen
import com.tik_market.ui.vendor.VendorGroupBuysScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VendorFlow(
    appState: AppState,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val showError: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    when (appState.currentScreen) {
        NavScreen.VendorDashboard -> VendorDashboardScreen(
            onBack = { appState.goBack() },
            shopName = appState.vendorShopName,
            onManageShop = {
                scope.launch {
                    val shop = ApiClient.fetchShopByVendor()
                    if (shop != null) {
                        appState.vendorShopName = shop.name
                        appState.navigateTo(NavScreen.ManageShop)
                    } else {
                        appState.navigateTo(NavScreen.CreateShop)
                    }
                }
            },
            onAddProduct = {
                scope.launch {
                    val shop = ApiClient.fetchShopByVendor()
                    if (shop != null) {
                        appState.vendorShopName = shop.name
                        appState.selectedProduct = null
                        appState.navigateTo(NavScreen.AddProduct)
                    } else {
                        appState.navigateTo(NavScreen.CreateShop)
                    }
                }
            },
            onViewOrders = {
                scope.launch {
                    val shop = ApiClient.fetchShopByVendor()
                    if (shop != null) {
                        appState.vendorShopName = shop.name
                        appState.navigateTo(NavScreen.VendorOrders)
                    } else {
                        appState.navigateTo(NavScreen.CreateShop)
                    }
                }
            },
            onGroupBuys = {
                scope.launch {
                    val shop = ApiClient.fetchShopByVendor()
                    if (shop != null) {
                        appState.vendorShopName = shop.name
                        appState.navigateTo(NavScreen.VendorGroupBuys)
                    } else {
                        appState.navigateTo(NavScreen.CreateShop)
                    }
                }
            },
            onSubscribers = { appState.navigateTo(NavScreen.VendorSubscribers) },
            onLiveStreaming = { appState.navigateTo(NavScreen.LiveStreaming) },
            onPublishReel = { appState.navigateTo(NavScreen.CreateReel) }
        )
        NavScreen.ManageShop -> ManageShopScreen(
            onBack = { appState.goBack() },
            shopName = appState.vendorShopName,
            onSaveShop = { _, _, _, _, _ -> /* Géré en interne */ },
            onEditProduct = { id, title, desc, price, compare, cat, stock, unit, img ->
                appState.selectedProduct = Product(
                    id = id.toString(),
                    title = title,
                    description = desc,
                    price = price.toDoubleOrNull() ?: 0.0,
                    comparePrice = compare.toDoubleOrNull(),
                    category = cat,
                    stock = stock.toIntOrNull() ?: 0,
                    unit = unit,
                    images = if (img.isNotBlank()) img.split(",").map { it.trim() }.filter { it.isNotEmpty() } else emptyList()
                )
                appState.navigateTo(NavScreen.AddProduct)
            }
        )
        NavScreen.AddProduct -> AddProductScreen(
            onBack = { appState.goBack() },
            onSave = { form ->
                scope.launch {
                    try {
                        val shop = ApiClient.fetchShopByVendor()
                        if (shop != null) {
                            // Upload new images
                            val newUploadedUrls = form.newImages.map { img ->
                                ApiClient.uploadImage(img.dataUrl, img.fileName)
                            }
                            
                            // Combine with existing ones
                            val allImages = (form.imageUrls + newUploadedUrls).joinToString(",")
                            
                            if (form.productId == 0) {
                                ApiClient.createProduct(
                                    shopId = shop.id,
                                    title = form.title,
                                    description = form.description,
                                    price = form.price.toDoubleOrNull() ?: 0.0,
                                    comparePrice = form.comparePrice.toDoubleOrNull(),
                                    category = form.category,
                                    stock = form.stock.toIntOrNull() ?: 0,
                                    unit = form.unit,
                                    imageUrl = allImages,
                                    isStory = form.isStory
                                )
                            } else {
                                ApiClient.updateProduct(
                                    productId = form.productId,
                                    title = form.title,
                                    description = form.description,
                                    price = form.price.toDoubleOrNull(),
                                    comparePrice = form.comparePrice.toDoubleOrNull(),
                                    category = form.category,
                                    stock = form.stock.toIntOrNull(),
                                    unit = form.unit,
                                    imageUrl = allImages,
                                    isStory = form.isStory
                                )
                            }
                            appState.goBack()
                        } else {
                            showError("Vous devez d'abord créer une boutique")
                        }
                    } catch (e: Exception) {
                        showError(e.message ?: "Erreur lors de l'enregistrement")
                    }
                }
            },
            editProduct = appState.selectedProduct?.let { p ->
                ProductForm(
                    productId = p.id.toIntOrNull() ?: 0,
                    title = p.title,
                    description = p.description,
                    price = p.price.toString(),
                    comparePrice = p.comparePrice?.toString() ?: "",
                    category = p.category,
                    stock = p.stock.toString(),
                    unit = p.unit,
                    imageUrls = p.images,
                    isStory = p.isStory
                )
            }
        )
        NavScreen.VendorOrders -> ManageOrdersScreen(onBack = { appState.goBack() })
        NavScreen.VendorGroupBuys -> VendorGroupBuysScreen(
            onBack = { appState.goBack() },
            shopName = appState.vendorShopName
        )
        NavScreen.VendorSubscribers -> SubscribersScreen(onBack = { appState.goBack() })
        NavScreen.FollowedShops -> FollowedShopsScreen(
            onBack = { appState.goBack() },
            onShopClick = { id ->
                appState.selectedShopId = id
                appState.navigateTo(NavScreen.ShopPage)
            }
        )
        NavScreen.CreateShop -> CreateShopScreen(
            onBack = { appState.goBack() },
            onShopCreated = { name ->
                appState.vendorShopName = name
                appState.goBack()
            }
        )
        else -> {}
    }
}
