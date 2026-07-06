package com.dschangmarket.ui.components

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
import com.dschangmarket.data.models.Product
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.Green
import com.dschangmarket.theme.GreenAccent
import com.dschangmarket.theme.GreenAccentSurface
import com.dschangmarket.theme.GreenSurface
import com.dschangmarket.theme.Orange
import com.dschangmarket.theme.RedAccent

import com.dschangmarket.utils.FormatUtils

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
        Text("✓ En stock ($stock)", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Medium)
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
            TextButton(onClick = onAction) { Text(action, color = Green) }
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
    // ── Animation: fade-in au montage ──
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // ── Animation: scale au clic ET au hover (go out) ──
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    // ── Animation: Hover/Click scale "go out" ──
    val cardScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f      // léger enfoncement au clic
            isHovered -> 1.05f      // "go out"
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 350f)
    )

    // ── Animation: elevation ──
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 6.dp else 2.dp,
        animationSpec = tween(180)
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    ) {
        Card(
            onClick = onClick,
            modifier = modifier.scale(cardScale),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(elevation),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            interactionSource = interactionSource
        ) {
            Column {
                // Product image
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                var imageLoading by remember { mutableStateOf(false) }

                LaunchedEffect(product.images.firstOrNull()) {
                    val imgUrl = product.images.firstOrNull()
                    if (!imgUrl.isNullOrBlank()) {
                        imageLoading = true
                        imageBitmap = loadImageFromUrl(imgUrl)
                        imageLoading = false
                    }
                }

                val emoji = SampleData.productEmojis[product.id] ?: "📦"
                val gradient = SampleData.categoryGradients[product.category]
                val productBg = if (gradient != null) Brush.verticalGradient(listOf(Color(gradient.first), Color(gradient.second)))
                               else Brush.verticalGradient(listOf(GreenSurface, GreenSurface))

                // ── Animation: Image Zoom on Hover/Press ──
                val imageScale by animateFloatAsState(
                    targetValue = if (isHovered || isPressed) 1.15f else 1f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(productBg)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), // Clip the zoom inside the box
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap!!,
                            contentDescription = product.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(imageScale),
                            contentScale = ContentScale.Crop
                        )
                    } else if (imageLoading) {
                        Box(modifier = Modifier.fillMaxSize().background(GreenSurface), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green.copy(alpha = 0.5f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Text(emoji, fontSize = 48.sp)
                    }
                    
                    // Alibaba search icon on bottom left of image
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(32.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ImageSearch, null, Modifier.size(18.dp), tint = Color.Black)
                        }
                    }

                    // Favorite heart button
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "Favori",
                                tint = if (isFavorite) Color(0xFFE91E63) else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (product.discountPercent > 0) {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DiscountBadge(product.discountPercent)
                                if (product.discountPercent >= 20) {
                                    Surface(color = Orange, shape = RoundedCornerShape(4.dp)) {
                                        Text("VENTE FLASH", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        product.title, 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 13.sp,
                        color = Color.Black,
                        maxLines = 2, 
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                    
                    Spacer(Modifier.height(2.dp))
                    
                    Text(
                        product.shopName,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriceDisplay(product.price, product.comparePrice, 15.sp)
                        
                        if (product.rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, Modifier.size(10.dp), tint = Color(0xFFFFB300))
                                Text("${product.rating}", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (product.shopVerified) {
                            Surface(
                                color = GreenSurface,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically, 
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Verified, null, Modifier.size(10.dp), tint = Green)
                                    Text("Vérifié", fontWeight = FontWeight.Bold, fontSize = 8.sp, color = Green)
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        
                        Surface(
                            onClick = { onAddToCart() },
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
    val statuses = listOf(
        "pending" to "Payé",
        "confirmed" to "Confirmé",
        "preparing" to "Préparé",
        "delivering" to "Livraison",
        "delivered" to "Reçu"
    )
    
    val currentIndex = when (currentStatus) {
        "pending" -> -1  // Not yet paid
        "cancelled" -> -1 // Cancelled
        else -> statuses.indexOfFirst { it.first == currentStatus }.let { if (it == -1) 0 else it }
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        statuses.forEachIndexed { index, pair ->
            val isCompleted = if (currentStatus == "cancelled") false else index <= currentIndex
            val isCurrent = index == currentIndex
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isCompleted) Green else Color.LightGray.copy(alpha = 0.5f),
                            CircleShape
                        ),
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
                    fontSize = 8.sp,
                    color = if (isCompleted) Green else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
            
            if (index < statuses.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.5f)
                        .offset(y = (-8).dp)
                        .background(if (index < currentIndex) Green else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }
    }
}
