package com.tik_market.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.data.models.Product
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.ui.home.components.HomeProductGrid

@Composable
fun CityMarketBadge(text: String, color: Color = Orange) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

/**
 * Section commune pour les Ventes Flash
 */
fun LazyListScope.FlashSalesSection(
    products: List<Product>,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    val flashProducts = products.filter { it.comparePrice != null && it.comparePrice!! > it.price }.take(6)
    if (flashProducts.isEmpty()) return

    item {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ventes Flash ⚡", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F))
                    Spacer(Modifier.width(8.dp))
                    Surface(color = Color(0xFFD32F2F), shape = RoundedCornerShape(4.dp)) {
                        Text("02:45:12", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                flashProducts.forEach { product ->
                    ProductCard(
                        product = product,
                        onClick = { onProductClick(product) },
                        onAddToCart = { onAddToCart(product) },
                        modifier = Modifier.width(150.dp),
                        isFavorite = product.id.toIntOrNull() in wishlistIds,
                        onToggleFavorite = { onToggleFavorite(product.id.toIntOrNull() ?: 0) }
                    )
                }
            }
        }
    }
}

/**
 * Template pour Dschang (DschangMarket) - Étudiants, Logement, Occasions
 */
fun LazyListScope.DschangLayout(
    products: List<Product>,
    columns: Int,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(Icons.Default.Home, "Logement", Green) { }
            QuickActionItem(Icons.Default.School, "Cours", Orange) { }
            QuickActionItem(Icons.Default.ShoppingBag, "Occasions", Color.Blue) { }
            QuickActionItem(Icons.Default.Restaurant, "Resto", Color.Red) { }
        }
    }

    FlashSalesSection(products, wishlistIds, onProductClick, onAddToCart, onToggleFavorite)
    
    item {
        SectionHeader("Bons plans étudiants 🎓")
    }

    item {
        HomeProductGrid(
            products = products,
            columns = columns,
            wishlistIds = wishlistIds,
            onProductClick = onProductClick,
            onAddToCart = onAddToCart,
            onToggleFavorite = onToggleFavorite
        )
    }
}

/**
 * Template pour Bafoussam (Fu'sapMarket) - Terroir, Agriculture, Direct
 */
fun LazyListScope.FuSapLayout(
    products: List<Product>,
    columns: Int,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    item {
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Agriculture, null, tint = Color(0xFFE65100), modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Direct de l'Ouest", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("Produits frais du terroir de Fu'sap", fontSize = 12.sp, color = Color(0xFFE65100).copy(alpha = 0.8f))
                }
            }
        }
    }

    FlashSalesSection(products, wishlistIds, onProductClick, onAddToCart, onToggleFavorite)

    item {
        SectionHeader("Récoltes du jour 🌽")
    }

    item {
        HomeProductGrid(
            products = products,
            columns = columns,
            wishlistIds = wishlistIds,
            onProductClick = onProductClick,
            onAddToCart = onAddToCart,
            onToggleFavorite = onToggleFavorite
        )
    }
}

/**
 * Template pour Douala (DoualaMarket) - Tech, Ventes Flash, Business
 */
fun LazyListScope.DoualaLayout(
    products: List<Product>,
    columns: Int,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    FlashSalesSection(products, wishlistIds, onProductClick, onAddToCart, onToggleFavorite)

    item {
        SectionHeader("Tendance à Douala 🏙️")
    }

    item {
        HomeProductGrid(
            products = products,
            columns = columns,
            wishlistIds = wishlistIds,
            onProductClick = onProductClick,
            onAddToCart = onAddToCart,
            onToggleFavorite = onToggleFavorite
        )
    }
}

/**
 * Template pour Yaoundé (YaoundeMarket) - Services, Bureau, Prestige
 */
fun LazyListScope.YaoundeLayout(
    products: List<Product>,
    columns: Int,
    wishlistIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    item {
        SectionHeader("Services Express 🏛️")
    }
    
    item {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionItem(Icons.Default.Business, "Bureautique", Green) { }
            QuickActionItem(Icons.Default.LocalTaxi, "Livraison", Orange) { }
            QuickActionItem(Icons.Default.HomeRepairService, "Réparations", Color.Blue) { }
        }
    }

    FlashSalesSection(products, wishlistIds, onProductClick, onAddToCart, onToggleFavorite)

    item {
        SectionHeader("Le prestige d'Ongola ✨")
    }

    item {
        HomeProductGrid(
            products = products,
            columns = columns,
            wishlistIds = wishlistIds,
            onProductClick = onProductClick,
            onAddToCart = onAddToCart,
            onToggleFavorite = onToggleFavorite
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}
