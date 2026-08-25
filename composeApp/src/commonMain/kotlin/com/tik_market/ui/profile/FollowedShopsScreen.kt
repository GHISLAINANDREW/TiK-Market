package com.tik_market.ui.profile

import com.tik_market.api.*
import com.tik_market.api.dto.*

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.*
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowedShopsScreen(
    onBack: () -> Unit,
    onShopClick: (Int) -> Unit
) {
    var shops by remember { mutableStateOf<List<ApiFavoriteShop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val ts = LocalAppStrings.current

    suspend fun loadShops() {
        try {
            shops = ApiClient.fetchFavoriteShops()
        } catch (_: Exception) {}
        isLoading = false
    }

    LaunchedEffect(Unit) { loadShops() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(ts.followedShops, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else if (shops.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Store, null, Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text(ts.noFollowedShops, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shops) { shop ->
                    FollowedShopCard(
                        shop = shop,
                        onClick = { onShopClick(shop.shopId) },
                        onUnfollow = {
                            scope.launch {
                                try {
                                    val resp = ApiClient.removeFavoriteShop(shop.shopId)
                                    snackbarHostState.showSnackbar(ts.unfollowedMsg.format(shop.name))
                                    loadShops()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(ts.unfollowError.format(e.message?.take(50) ?: ts.failed))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowedShopCard(shop: ApiFavoriteShop, onClick: () -> Unit, onUnfollow: () -> Unit = {}) {
    val ts = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Store, null, tint = Green)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (shop.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Verified, null, Modifier.size(16.dp), tint = Color(0xFF1890FF))
                    }
                }
                Text(shop.category, fontSize = 12.sp, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ts.productsCount.format(shop.productCount), fontSize = 11.sp, color = TextTertiary)
                    Text(" • ", color = DividerGray)
                    Text(ts.salesCount.format(shop.totalSales), fontSize = 11.sp, color = TextTertiary)
                }
            }
            // Bouton se désabonner
            OutlinedButton(
                onClick = onUnfollow,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.RemoveCircleOutline, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(ts.unsubscribe, fontSize = 10.sp)
            }
        }
    }
}
