package com.tik_market.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Green
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import com.tik_market.utils.getCurrentLocationLatLng
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsListScreen(
    onBack: () -> Unit,
    city: String? = null,
    onShopClick: (ApiShop) -> Unit
) {
    var shops by remember { mutableStateOf<List<ApiShop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var nearbyOnly by remember { mutableStateOf(false) }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    val s = LocalAppStrings.current

    LaunchedEffect(city) {
        try {
            val apiShops = ApiClient.fetchShops(location = city)
            if (apiShops.isNotEmpty()) {
                shops = apiShops
            } else if (city == null) {
                shops = com.tik_market.data.models.SampleData.shops.map { s ->
                    ApiShop(
                        id = s.id.toIntOrNull() ?: 0,
                        name = s.name,
                        category = s.category,
                        location = s.location,
                        isVerified = s.isVerified,
                        latitude = 5.4627 + (s.id.toIntOrNull() ?: 0) * 0.001,
                        longitude = 10.0533 + (s.id.toIntOrNull() ?: 0) * 0.001
                    )
                }
            } else {
                shops = emptyList()
            }
        } catch (e: Exception) {
            if (city == null) {
                shops = com.tik_market.data.models.SampleData.shops.map { s ->
                    ApiShop(
                        id = s.id.toIntOrNull() ?: 0,
                        name = s.name,
                        category = s.category,
                        location = s.location,
                        isVerified = s.isVerified,
                        latitude = 5.4627 + (s.id.toIntOrNull() ?: 0) * 0.001,
                        longitude = 10.0533 + (s.id.toIntOrNull() ?: 0) * 0.001
                    )
                }
            } else {
                shops = emptyList()
            }
        }
        isLoading = false
    }

    // Filter and sort shops for 5m - 400m proximity when nearbyOnly is true
    val displayedShops = remember(shops, nearbyOnly, userLat, userLng) {
        if (!nearbyOnly || userLat == null || userLng == null) {
            shops
        } else {
            shops.mapNotNull { shop ->
                if (shop.latitude != 0.0 && shop.longitude != 0.0) {
                    val dist = calculateDistanceMeters(userLat!!, userLng!!, shop.latitude, shop.longitude)
                    if (dist in 5.0..400.0) shop to dist else null
                } else null
            }.sortedBy { it.second }.map { it.first }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (city.isNullOrBlank()) s.allShops else s.shopsIn.format(city), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
            // Proximity Filter Toggle Bar
            Surface(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, null, tint = Green, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Boutiques à proximité (5m - 400m)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Basé sur le GPS de votre téléphone", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Switch(
                        checked = nearbyOnly,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                getCurrentLocationLatLng { lat, lng ->
                                    userLat = lat
                                    userLng = lng
                                    nearbyOnly = true
                                }
                            } else {
                                nearbyOnly = false
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Green)
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            } else if (displayedShops.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (nearbyOnly) "Aucune boutique trouvée entre 5m et 400m de vous." else s.noShopsFound, color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedShops) { shop ->
                        ShopItem(shop = shop, userLat = userLat, userLng = userLng, onClick = { onShopClick(shop) })
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItem(shop: ApiShop, userLat: Double?, userLng: Double?, onClick: () -> Unit) {
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val s = LocalAppStrings.current

    val distanceText = remember(userLat, userLng, shop.latitude, shop.longitude) {
        if (userLat != null && userLng != null && shop.latitude != 0.0 && shop.longitude != 0.0) {
            val dist = calculateDistanceMeters(userLat, userLng, shop.latitude, shop.longitude)
            if (dist < 1000) "${dist.toInt()} m" else "${kotlin.math.round((dist / 1000) * 10) / 10.0} km"
        } else null
    }
    
    LaunchedEffect(shop.logo) {
        if (!shop.logo.isNullOrBlank()) {
            val cleanBase = ApiClient.baseUrl.trimEnd('/')
            val cleanPath = shop.logo.trimStart('/', '\\').replace("\\", "/")
            val finalUrl = if (shop.logo.startsWith("http")) shop.logo else "$cleanBase/$cleanPath"
            try {
                logoBitmap = loadImageFromUrl(finalUrl)
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = Green.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap!!,
                            contentDescription = shop.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Store, null, tint = Green)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(shop.category, color = Color.Gray, fontSize = 13.sp)
                if (distanceText != null) {
                    Spacer(Modifier.height(2.dp))
                    Text("📍 à $distanceText", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Green)
                }
            }
            if (shop.isVerified) {
                Surface(
                    color = Green.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        s.verified,
                        color = Green,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
    val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.asin(kotlin.math.sqrt(a))
    return r * c
}
