package com.tik_market.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tik_market.data.models.Product
import com.tik_market.ui.components.ProductCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeProductGrid(
    products: List<Product>,
    columns: Int,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    // Note: Pour simplifier dans KMP, on utilise FlowRow qui s'adapte à la largeur.
    // Le paramètre 'columns' peut servir à ajuster la largeur des cartes si besoin.
    
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        products.forEach { product ->
            val productId = product.id.toIntOrNull()
            val isFav = productId != null && productId in wishlistIds
            
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                onAddToCart = { onAddToCart(product) },
                modifier = Modifier.width(160.dp), // Largeur fixe pour la cohérence en FlowRow
                isFavorite = isFav,
                onToggleFavorite = { if (productId != null) onToggleFavorite(productId) }
            )
        }
    }
}
