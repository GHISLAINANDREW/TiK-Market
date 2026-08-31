package com.tik_market.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.theme.*
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.utils.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isLoggedIn: Boolean = false,
    userName: String = "",
    userRole: String = "buyer",
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onMessagesClick: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onVendorDashboardClick: () -> Unit,
    onAdminDashboardClick: () -> Unit = {},
    onLoyaltyClick: () -> Unit = {},
    onFollowedShopsClick: () -> Unit = {},
    onNotifPrefsClick: () -> Unit = {},
    onGroupBuysClick: () -> Unit = {},
    onShopsMapClick: () -> Unit = {},
    walletBalance: Double = 0.0,
    walletPoints: Int = 0,
    walletTier: String = "bronze",
    onLogout: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val s = LocalAppStrings.current
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentUser = ApiClient.getCurrentUser()

    // Confirm logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
            title = { Text(s.logoutConfirmTitle) },
            text = { Text(s.logoutConfirmText) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text(s.logoutConfirmTitle, color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(s.cancel)
                }
            }
        )
    }

    // Load images
    LaunchedEffect(currentUser?.avatar, currentUser?.coverPhoto) {
        currentUser?.avatar?.let { url -> if (url.isNotBlank()) avatarBitmap = loadImageFromUrl(url) }
        currentUser?.coverPhoto?.let { url -> if (url.isNotBlank()) coverBitmap = loadImageFromUrl(url) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(s.myAccount, fontWeight = FontWeight.SemiBold, color = Color.White) },
                actions = {
                    if (isLoggedIn) IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalCityColors.current.topBar)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
            if (isLoggedIn) {
                // Header profil connecté avec Photo de Couverture
                Box(Modifier.fillMaxWidth().height(220.dp)) {
                    // Photo de couverture
                    Box(Modifier.fillMaxWidth().height(160.dp).background(MaterialTheme.colorScheme.primary)) {
                        if (coverBitmap != null) {
                            Image(coverBitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        // Gradient overlay for readability
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                    }
                    
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                .background(Color.White)
                                .clickable { onEditProfileClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(bitmap = avatarBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Person, null, Modifier.size(50.dp), tint = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(userName.ifEmpty { s.defaultUser }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, 
                            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)))
                        Text(currentUser?.email ?: "", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp,
                            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 2f)))
                        
                        Spacer(Modifier.height(8.dp))
                        // Bouton éditer
                        Surface(
                            onClick = onEditProfileClick,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, null, Modifier.size(14.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(s.editProfile, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── Points & Balance Quick Access (Outside the box) ──
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-20).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PointsCard(walletPoints.toString(), s.points, onLoyaltyClick)
                    PointsCard("${walletBalance.toInt()} F", s.cashback, onLoyaltyClick)
                    PointsCard(walletTier.uppercase(), s.level, onLoyaltyClick)
                }

                Column(Modifier.offset(y = (-20).dp)) {
                    // Section Commandes (Alibaba style)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp,
                        onClick = onOrdersClick
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(s.myOrders, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.seeAll, color = Color.Gray, fontSize = 13.sp)
                                    Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = Color.Gray)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                OrderActionItem(Icons.Default.Payment, s.toPay, "0")
                                OrderActionItem(Icons.Default.Inventory, s.toShip, "0")
                                OrderActionItem(Icons.Default.LocalShipping, s.toReceive, "0")
                                OrderActionItem(Icons.Default.RateReview, s.reviews, "0")
                            }
                        }
                    }

                    // Section Portefeuille
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        Row(Modifier.padding(16.dp).clickable { onLoyaltyClick() }, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(Orange.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = Orange)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.myWallet, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${s.balance}: ${walletBalance.toInt()} FCFA • ${walletPoints} pts", color = Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.Gray)
                        }
                    }

                    // Section Services & Outils
                    Text(s.myServices, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                ServiceGridItem(Icons.Default.Favorite, s.myFavorites, Green) { onWishlistClick() }
                                ServiceGridItem(Icons.Default.Storefront, s.followed, Color(0xFF1565C0)) { onFollowedShopsClick() }
                                ServiceGridItem(Icons.Default.Chat, s.messages, Green) { onMessagesClick() }
                                ServiceGridItem(Icons.Default.ConfirmationNumber, s.coupons, Amber) { onLoyaltyClick() }
                            }
                            Spacer(Modifier.height(20.dp))
                            Row(Modifier.fillMaxWidth()) {
                                ServiceGridItem(Icons.Default.Notifications, s.notifications, Orange) { onNotifPrefsClick() }
                                ServiceGridItem(Icons.Default.Group, s.groupBuys, Green) { onGroupBuysClick() }
                                ServiceGridItem(Icons.Default.Map, s.shops, Color(0xFFE91E63)) { onShopsMapClick() }
                                ServiceGridItem(Icons.Default.Settings, s.settings, Color.Gray) { onSettingsClick() }
                            }
                        }
                    }

                    // Section Spéciale Vendeur/Admin
                    if (userRole == "vendor" || userRole == "admin" || userRole == "super_admin") {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 0.5.dp
                        ) {
                            Column {
                                if (userRole == "vendor") {
                                    ProfileMenuItem(Icons.Default.Storefront, s.vendorSpace, s.vendorSpaceSubtitle, Green) { onVendorDashboardClick() }
                                }
                                if (userRole == "admin" || userRole == "super_admin") {
                                    if (userRole == "vendor") HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFF5F5F5))
                                    ProfileMenuItem(Icons.Default.AdminPanelSettings, s.admin, s.adminSubtitle, BlueAccent) { onAdminDashboardClick() }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        ProfileMenuItem(Icons.AutoMirrored.Filled.Logout, s.logoutConfirmTitle, "", Color.Red) { showLogoutDialog = true }
                    }
                }
            } else {
                // Utilisateur non connecté
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(s.connect, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(s.loginRequiredHint, color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(s.loginBtn, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RowScope.PointsCard(value: String, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.weight(1f).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun OrderActionItem(icon: ImageVector, label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(icon, null, Modifier.size(28.dp), tint = Color(0xFF424242))
            if (count != "0") {
                Surface(
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier.offset(x = 8.dp, y = (-4).dp)
                ) {
                    Text(count, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun RowScope.ServiceGridItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(45.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = color)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (tint != Color.Unspecified) tint else primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (tint != Color.Unspecified) tint else Color.Unspecified)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
        }
    }
}
