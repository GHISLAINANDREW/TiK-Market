package com.tik_market.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.data.models.Product
import com.tik_market.data.models.SampleData
import com.tik_market.data.models.OrderStatus
import com.tik_market.theme.LocalCityColors
import com.tik_market.theme.Orange
import com.tik_market.theme.RedAccent

import com.tik_market.utils.FormatUtils

@Composable
fun PriceDisplay(price: Double, comparePrice: Double? = null, fontSize: androidx.compose.ui.unit.TextUnit = 18.sp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(FormatUtils.formatPrice(price), fontSize = fontSize, fontWeight = FontWeight.Bold, color = Orange)
        if (comparePrice != null && comparePrice > price) {
            Spacer(Modifier.width(4.dp))
            Text(FormatUtils.formatPrice(comparePrice),
                fontSize = (fontSize.value * 0.7).sp,
                color = Color.Gray,
                textDecoration = TextDecoration.LineThrough
            )
        }
    }
}

@Composable
fun RatingBar(rating: Float, reviews: Int = 0) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("★", color = Color(0xFFFFB300), fontSize = 14.sp)
        Spacer(Modifier.width(2.dp))
        Text("${rating}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (reviews > 0) {
            Text(" ($reviews)", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StockBadge(inStock: Boolean, stock: Int = 0) {
    if (inStock) {
        Text("✓ En stock ($stock)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    } else {
        Text("Rupture de stock", fontSize = 12.sp, color = RedAccent, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DiscountBadge(percent: Int) {
    if (percent > 0) {
        Surface(color = RedAccent, shape = RoundedCornerShape(4.dp)) {
            Text("-$percent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (action != null) {
            TextButton(onClick = onAction) { Text(action, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 350f)
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.95f)
    ) {
        Card(
            onClick = onClick,
            modifier = modifier.scale(cardScale).shadow(if (isHovered) 8.dp else 2.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            interactionSource = interactionSource
        ) {
            Column {
                // 1. Image Section (Alibaba Style: Clean, high aspect ratio)
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                var imageLoading by remember { mutableStateOf(false) }
                var retryCount by remember { mutableStateOf(0) }

                LaunchedEffect(product.images.firstOrNull(), retryCount) {
                    val imgUrl = product.images.firstOrNull()
                    if (!imgUrl.isNullOrBlank()) {
                        imageLoading = true
                        imageBitmap = loadImageFromUrl(imgUrl)
                        imageLoading = false
                    }
                }

                val primary = MaterialTheme.colorScheme.primary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.9f) // Slightly taller for premium look
                        .background(Color(0xFFF8F8F8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageLoading) {
                        CircularProgressIndicator(color = primary.copy(alpha = 0.3f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        // Retry button if failed
                        IconButton(onClick = { retryCount++ }) {
                            Icon(Icons.Default.Refresh, null, tint = Color.LightGray)
                        }
                    }

                    // Top Right: Favorite
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp).clickable { onToggleFavorite() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    null,
                                    tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Left: Sales Badge
                    if (product.totalSales > 10) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Vendu ${product.totalSales}+",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // 2. Info Section
                Column(modifier = Modifier.padding(10.dp)) {
                    // Price (Primary focus in Alibaba model)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            FormatUtils.formatPrice(product.price),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Orange
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("CFA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Orange, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    
                    if (product.comparePrice != null && product.comparePrice > product.price) {
                        Text(
                            FormatUtils.formatPrice(product.comparePrice),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        product.title, 
                        fontWeight = FontWeight.Medium, 
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Shop & Trust signals
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product.shopName,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (product.shopVerified) {
                            Icon(Icons.Default.Verified, null, Modifier.size(14.dp), tint = primary)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Action button (Quick add to cart)
                        Surface(
                            onClick = onAddToCart,
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = Orange.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddShoppingCart, null, Modifier.size(14.dp), tint = Orange)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shimmer Loading Skeleton ──

/**
 * A shimmer gradient brush that animates from left to right.
 * Use as: `Modifier.background(shimmerBrush())`
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 60f)
    )
}

/**
 * A single shimmer placeholder card matching the ProductCard layout.
 */
@Composable
fun ShimmerProductCard(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Image placeholder
            Box(
                Modifier.fillMaxWidth().height(150.dp).background(brush)
            )
            // Text placeholders
            Column(Modifier.padding(6.dp)) {
                Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String = "") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = Color.LightGray, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun OrderProgressBar(currentStatus: String, modifier: Modifier = Modifier) {
    val orderStatus = OrderStatus.fromCode(currentStatus)
    val steps = listOf(
        OrderStatus.PENDING to "Payé",
        OrderStatus.CONFIRMED to "Confirmé",
        OrderStatus.PREPARING to "Préparé",
        OrderStatus.DELIVERING to "Livraison",
        OrderStatus.DELIVERED to "Reçu"
    )
    
    val currentIndex = when (orderStatus) {
        OrderStatus.PENDING -> -1  // Not yet paid
        OrderStatus.CANCELLED -> -1 // Cancelled
        else -> steps.indexOfFirst { it.first == orderStatus }
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { index, pair ->
            val isCompleted = if (orderStatus == OrderStatus.CANCELLED) false else index <= currentIndex
            val isCurrent = index == currentIndex
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (isCompleted) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted && !isCurrent) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                    } else if (isCurrent) {
                        Box(Modifier.size(8.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    pair.second,
                    fontSize = 10.sp,
                    color = if (isCompleted) Color.Black else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
            
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.5f)
                        .offset(y = (-10).dp)
                        .background(if (index < currentIndex) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0))
                )
            }
        }
    }
}
