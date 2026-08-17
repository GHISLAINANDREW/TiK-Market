package com.tik_market.ui.misc

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.data.models.Product
import com.tik_market.theme.Orange
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.utils.FormatUtils
import com.tik_market.utils.LocalAppStrings
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    products: List<Product>,
    onBack: () -> Unit,
    onRemoveProduct: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit = {}
) {
    val ts = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.comparatif.format(products.size)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(ts.noProductsCompare, color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Header row with images
                Row(modifier = Modifier.padding(16.dp)) {
                    // Spacer for the attribute labels column
                    Box(Modifier.width(100.dp))
                    
                    products.forEach { product ->
                        CompareProductHeader(
                            product = product,
                            onRemove = { onRemoveProduct(product) },
                            onAddToCart = { onAddToCart(product) },
                            onClick = { onProductClick(product) }
                        )
                    }
                }

                HorizontalDivider()

                // Attributes rows
                val attributes = listOf(
                    ts.price to { p: Product -> FormatUtils.formatPrice(p.price) },
                    ts.category to { p: Product -> p.category },
                    ts.shop to { p: Product -> p.shopName },
                    ts.stock to { p: Product -> if (p.stock > 0) ts.inStock.format(p.stock) else ts.outOfStock },
                    ts.rating to { p: Product -> "★ ${p.rating} (${p.totalReviews})" },
                    ts.unit to { p: Product -> p.unit }
                )

                attributes.forEach { (label, getValue) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.width(100.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        
                        products.forEach { product ->
                            Text(
                                text = getValue(product),
                                modifier = Modifier.width(160.dp).padding(horizontal = 8.dp),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
                
                // Description row (potentially long)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = ts.description,
                        modifier = Modifier.width(100.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    
                    products.forEach { product ->
                        Text(
                            text = product.description,
                            modifier = Modifier.width(160.dp).padding(horizontal = 8.dp),
                            fontSize = 12.sp,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompareProductHeader(
    product: Product,
    onRemove: () -> Unit,
    onAddToCart: () -> Unit,
    onClick: () -> Unit = {}
) {
    val ts = LocalAppStrings.current
    Column(
        modifier = Modifier.width(160.dp).padding(horizontal = 8.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(120.dp)) {
            var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(product.images.firstOrNull()) {
                product.images.firstOrNull()?.let { bitmap = loadImageFromUrl(it) }
            }
            
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("📦", fontSize = 48.sp)
                }
            }
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = product.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(40.dp)
        )
        
        Spacer(Modifier.height(4.dp))
        
        Button(
            onClick = onAddToCart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(ts.add, fontSize = 12.sp)
        }
    }
}
