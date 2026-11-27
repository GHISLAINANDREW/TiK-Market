package com.tik_market.ui.misc

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.*
import com.tik_market.api.dto.*
import com.tik_market.theme.*
import com.tik_market.ui.chat.openUrl
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.utils.LocalAppStrings
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.launch

/**
 * Écran "Carte des boutiques" — affiche une liste avec localisation
 * et un lien vers Google Maps pour chaque boutique.
 */
private fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * (kotlin.math.PI / 180.0)
    val dLng = (lng2 - lng1) * (kotlin.math.PI / 180.0)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(lat1 * (kotlin.math.PI / 180.0)) * kotlin.math.cos(lat2 * (kotlin.math.PI / 180.0)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsMapScreen(
    onBack: () -> Unit,
    onShopClick: (Int) -> Unit
) {
    var shops by remember { mutableStateOf<List<ApiShop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    // Store loaded logos
    var shopLogos by remember { mutableStateOf<Map<Int, ImageBitmap>>(emptyMap()) }
    val scope = rememberCoroutineScope()
    val ts = LocalAppStrings.current

    LaunchedEffect(Unit) {
        com.tik_market.utils.getCurrentLocationLatLng { lat, lng ->
            if (lat != null && lng != null) {
                userLat = lat
                userLng = lng
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val fetched = ApiClient.fetchShops()
            shops = fetched
            // Load logos for all shops
            fetched.forEach { shop ->
                if (shop.logo.isNotBlank()) {
                    try {
                        val cleanBase = ApiClient.baseUrl.trimEnd('/')
                        val cleanPath = shop.logo.trimStart('/', '\\').replace("\\", "/")
                        val url = if (shop.logo.startsWith("http")) shop.logo else "$cleanBase/$cleanPath"
                        val bmp = loadImageFromUrl(url)
                        if (bmp != null) shopLogos = shopLogos + (shop.id to bmp)
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    val filteredShops = remember(shops, searchQuery, userLat, userLng) {
        val base = if (searchQuery.isBlank()) shops
        else shops.filter { it.name.contains(searchQuery, ignoreCase = true) || it.location.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }

        if (userLat != null && userLng != null) {
            base.sortedBy { shop ->
                if (shop.latitude != 0.0 && shop.longitude != 0.0) {
                    calculateDistanceKm(userLat!!, userLng!!, shop.latitude, shop.longitude)
                } else {
                    9999.0
                }
            }
        } else {
            base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.shopsMapTitle, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(ts.searchShop) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )

                // Vue liste des boutiques avec localisation
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredShops.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.StoreMallDirectory, null, Modifier.size(48.dp), tint = Color.LightGray)
                                    Spacer(Modifier.height(12.dp))
                                    Text(ts.noShopsFound, color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    items(filteredShops, key = { it.id }) { shop ->
                        ShopMapCard(
                            shop = shop,
                            logo = shopLogos[shop.id],
                            userLat = userLat,
                            userLng = userLng,
                            onClick = { onShopClick(shop.id) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        // Légende
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = Orange)
                                    Spacer(Modifier.width(8.dp))
                                    Text(ts.shopClickTip, fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Map, null, Modifier.size(16.dp), tint = Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text(ts.mapOpensTip, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopMapCard(
    shop: ApiShop,
    logo: ImageBitmap?,
    userLat: Double? = null,
    userLng: Double? = null,
    onClick: () -> Unit
) {
    val ts = LocalAppStrings.current
    val distText = remember(userLat, userLng, shop.latitude, shop.longitude) {
        if (userLat != null && userLng != null && shop.latitude != 0.0 && shop.longitude != 0.0) {
            val d = calculateDistanceKm(userLat, userLng, shop.latitude, shop.longitude)
            if (d < 1.0) "${(d * 1000).toInt()} m" else "${(d * 10).toInt() / 10.0} km"
        } else null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logo != null) {
                        Image(bitmap = logo, contentDescription = shop.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(shop.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Orange, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(shop.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (shop.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Verified, null, Modifier.size(14.dp), tint = Color(0xFFFFD700))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (shop.category.isNotBlank()) {
                            Text(shop.category, fontSize = 12.sp, color = Color.Gray)
                        }
                        if (distText != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = GreenSurface, shape = RoundedCornerShape(8.dp)) {
                                Text("📍 $distText", fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }

            // Localisation
            if (shop.location.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp), tint = Orange)
                    Spacer(Modifier.width(6.dp))
                    Text(shop.location, fontSize = 13.sp, modifier = Modifier.weight(1f), color = Color.DarkGray)
                    // Boutons pour ouvrir dans Google Maps
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            onClick = { openUrl("https://www.google.com/maps/search/${shop.location.replace(" ", "+")}") },
                            shape = RoundedCornerShape(12.dp),
                            color = Green.copy(alpha = 0.1f)
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, null, Modifier.size(14.dp), tint = Green)
                                Spacer(Modifier.width(4.dp))
                                Text(ts.map, fontSize = 11.sp, color = Green, fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        Surface(
                            onClick = {
                                val navUrl = if (shop.latitude != 0.0 && shop.longitude != 0.0) {
                                    "https://www.google.com/maps/dir/?api=1&destination=${shop.latitude},${shop.longitude}"
                                } else {
                                    "https://www.google.com/maps/dir/?api=1&destination=${shop.location.replace(" ", "+")}"
                                }
                                openUrl(navUrl)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = BlueAccent.copy(alpha = 0.1f)
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Directions, null, Modifier.size(14.dp), tint = BlueAccent)
                                Spacer(Modifier.width(4.dp))
                                Text("Itinéraire", fontSize = 11.sp, color = BlueAccent, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Stats
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${shop.productCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                    Text(ts.products, fontSize = 10.sp, color = Color.Gray)
                }
                if (shop.rating > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${shop.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Orange)
                            Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB300))
                        }
                        Text(ts.rating, fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${shop.totalSales}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0))
                    Text(ts.sales, fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}
