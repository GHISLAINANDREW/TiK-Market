package com.tik_market.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.dto.ApiHeroItem
import com.tik_market.theme.GreenDark
import com.tik_market.theme.LocalCityColors
import com.tik_market.theme.Orange
import com.tik_market.ui.components.loadImageFromUrl
import kotlinx.coroutines.delay

@Composable
fun HomeHero(
    heroItems: List<ApiHeroItem>,
    screenWidth: Dp,
    cityName: String?
) {
    if (heroItems.isNotEmpty()) {
        DynamicHeroSection(heroItems, screenWidth)
    } else {
        AnimatedHeroSection(screenWidth, cityName)
    }
}

@Composable
fun DynamicHeroSection(items: List<ApiHeroItem>, screenWidth: Dp) {
    var index by remember { mutableStateOf(0) }
    val cityColors = LocalCityColors.current
    
    LaunchedEffect(items) {
        while (items.isNotEmpty()) {
            delay(5000)
            index = (index + 1) % items.size
        }
    }
    
    val heroHeight = if (screenWidth < 480.dp) 200.dp else if (screenWidth < 900.dp) 280.dp else 350.dp
    
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(heroHeight)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .shadow(4.dp)
    ) {
        AnimatedContent(
            targetState = items[index],
            transitionSpec = {
                (fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 1.1f, animationSpec = tween(1200))) togetherWith
                (fadeOut(animationSpec = tween(1200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(1200)))
            }
        ) { item ->
            Box(Modifier.fillMaxSize()) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.imageUrl) {
                    bitmap = loadImageFromUrl(item.imageUrl)
                }
                
                if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(cityColors.gradient))
                
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Orange.copy(alpha = 0.5f), GreenDark.copy(alpha = 0.7f)))))
                
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f))
                    )
                    Text(
                        text = item.subtitle,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (item.shopName != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        color = Orange,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "TOP BOUTIQUE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedHeroSection(screenWidth: Dp, cityName: String?) {
    val cityColors = LocalCityColors.current
    val items = when {
        cityName?.contains("Bafoussam", ignoreCase = true) == true -> listOf(
            Triple("Marché de Bafoussam", "Le cœur du commerce à l'Ouest", "https://images.unsplash.com/photo-1542831371-29b0f74f9713?w=800&q=80"),
            Triple("Produits du terroir", "Frais et naturels de Fousap", "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=800&q=80")
        )
        cityName?.contains("Douala", ignoreCase = true) == true -> listOf(
            Triple("Douala Shopping", "Les meilleures boutiques du Littoral", "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=800&q=80"),
            Triple("Sawa Market", "Fraîcheur marine et mode urbaine", "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80")
        )
        cityName?.contains("Yaoundé", ignoreCase = true) == true -> listOf(
            Triple("Yaoundé Direct", "La capitale à portée de main", "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=800&q=80"),
            Triple("Ongola Market", "Qualité et prestige réunis", "https://images.unsplash.com/photo-1441984904996-e0b6ba687e12?w=800&q=80")
        )
        else -> listOf(
            Triple("Will \u0026 Fils", "Volaille fraîche livrée à domicile", "https://images.unsplash.com/photo-1587593810167-a84920ea0781?w=800&q=80"),
            Triple("Mode \u0026 Élégance", "Pagne Wax de qualité", "https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80"),
            Triple("Saveurs locales", "Fruits et légumes du terroir", "https://images.unsplash.com/photo-1610348725531-843dff563e2c?w=800&q=80"),
            Triple("Tech \u0026 Gadgets", "Smartphones et accessoires", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&q=80")
        )
    }
    
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            index = (index + 1) % items.size
        }
    }
    
    val heroHeight = if (screenWidth < 480.dp) 200.dp else if (screenWidth < 900.dp) 280.dp else 350.dp
    
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(heroHeight)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .shadow(4.dp)
    ) {
        AnimatedContent(
            targetState = items[index],
            transitionSpec = {
                (fadeIn(animationSpec = tween(1200)) + scaleIn(initialScale = 1.1f, animationSpec = tween(1200))) togetherWith
                (fadeOut(animationSpec = tween(1200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(1200)))
            }
        ) { item ->
            Box(Modifier.fillMaxSize()) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(item.third) { bitmap = loadImageFromUrl(item.third) }
                
                if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize().background(cityColors.gradient))
                
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Orange.copy(alpha = 0.5f), GreenDark.copy(alpha = 0.7f)))))
                
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(
                        text = item.first,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, offset = androidx.compose.ui.geometry.Offset(2f, 2f), blurRadius = 4f))
                    )
                    Text(
                        text = item.second,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (item.first.contains("Will")) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        color = Orange,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "TOP VENDEUR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
