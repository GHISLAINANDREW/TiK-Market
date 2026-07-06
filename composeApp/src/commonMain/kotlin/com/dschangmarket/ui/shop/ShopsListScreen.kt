package com.dschangmarket.ui.shop

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiShop
import com.dschangmarket.theme.BrandTopBarColor
import com.dschangmarket.theme.Green
import com.dschangmarket.ui.components.loadImageFromUrl
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsListScreen(
    onBack: () -> Unit,
    onShopClick: (ApiShop) -> Unit
) {
    var shops by remember { mutableStateOf<List<ApiShop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val apiShops = ApiClient.fetchAllShops()
            if (apiShops.isNotEmpty()) {
                shops = apiShops
            } else {
                // Fallback to sample data if API is empty
                shops = com.dschangmarket.data.models.SampleData.shops.map { s ->
                    ApiShop(
                        id = s.id.toIntOrNull() ?: 0,
                        name = s.name,
                        category = s.category,
                        location = s.location,
                        isVerified = s.isVerified
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback on error
            shops = com.dschangmarket.data.models.SampleData.shops.map { s ->
                ApiShop(
                    id = s.id.toIntOrNull() ?: 0,
                    name = s.name,
                    category = s.category,
                    location = s.location,
                    isVerified = s.isVerified
                )
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Toutes les boutiques", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shops) { shop ->
                    ShopItem(shop = shop, onClick = { onShopClick(shop) })
                }
            }
        }
    }
}

@Composable
fun ShopItem(shop: ApiShop, onClick: () -> Unit) {
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
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
            }
            if (shop.isVerified) {
                Surface(
                    color = Green.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Vérifiée",
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
