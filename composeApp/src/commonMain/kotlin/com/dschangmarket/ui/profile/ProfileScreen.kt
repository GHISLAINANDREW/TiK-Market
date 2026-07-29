package com.dschangmarket.ui.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.data.models.Order
import com.dschangmarket.data.models.OrderStatus
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.rememberImagePickerLauncher
import com.dschangmarket.ui.components.decodeDataUrlToImageBitmap
import com.dschangmarket.ui.components.loadImageFromUrl
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isLoggedIn: Boolean = false,
    userName: String = "",
    userRole: String = "buyer",
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isUpdatingAvatar by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Confirm logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.Red) },
            title = { Text("Se déconnecter") },
            text = { Text("Voulez-vous vraiment vous déconnecter ?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("Se déconnecter", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    val currentUser = ApiClient.getCurrentUser()

    // Attempt to load existing avatar if any
    LaunchedEffect(currentUser?.avatar) {
        val avatarUrl = currentUser?.avatar ?: ""
        if (avatarUrl.isNotBlank()) {
            val cleanBase = ApiClient.baseUrl.trimEnd('/')
            val cleanPath = avatarUrl.trimStart('/', '\\').replace("\\", "/")
            val finalUrl = if (avatarUrl.startsWith("http")) avatarUrl else "$cleanBase/$cleanPath"
            
            try {
                val bitmap = loadImageFromUrl(finalUrl)
                if (bitmap != null) {
                    avatarBitmap = bitmap
                }
            } catch (_: Exception) {}
        }
    }

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            scope.launch {
                try {
                    isUpdatingAvatar = true
                    val imageUrl = ApiClient.uploadImage(result.dataUrl, result.fileName)
                    ApiClient.updateUserAvatar(imageUrl)
                    avatarBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
                    snackbarHostState.showSnackbar("✅ Photo de profil mise à jour")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("❌ Erreur: ${e.message?.take(60) ?: "Échec de la mise à jour"}")
                } finally {
                    isUpdatingAvatar = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mon Compte", fontWeight = FontWeight.SemiBold, color = Color.White) },
                actions = {
                    if (isLoggedIn) IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())) {
            if (isLoggedIn) {
                // Header profil connecté
                Surface(Modifier.fillMaxWidth(), color = Green) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { if (!isUpdatingAvatar) imagePicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(bitmap = avatarBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Person, null, Modifier.size(50.dp), tint = Color.White)
                            }
                            
                            // Overlay camera icon
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                Surface(
                                    modifier = Modifier.size(28.dp).offset(x = (-2).dp, y = (-2).dp),
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isUpdatingAvatar) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Green)
                                        } else {
                                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp), tint = Green)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(userName.ifEmpty { "Utilisateur" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(userName.lowercase().replace(" ", ".") + "@gmail.com", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        
                        if (userRole == "vendor") {
                            Spacer(Modifier.height(12.dp))
                            Surface(shape = RoundedCornerShape(20.dp), color = Amber) {
                                Text("PRO VENDEUR", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GreenDark, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // Section Commandes (Alibaba style)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp,
                    onClick = onOrdersClick // Whole section is clickable
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Mes Commandes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = onOrdersClick) {
                                Text("Voir tout", color = Color.Gray, fontSize = 13.sp)
                                Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = Color.Gray)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OrderActionItem(Icons.Default.Payment, "À payer", "10")
                            OrderActionItem(Icons.Default.Inventory, "À expédier", "1")
                            OrderActionItem(Icons.Default.LocalShipping, "À recevoir", "2")
                            OrderActionItem(Icons.Default.RateReview, "Avis", "34")
                        }
                    }
                }

                // Section Portefeuille (New)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                            Text("Mon Portefeuille", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Solde: ${walletBalance.toInt()} FCFA • ${walletPoints} pts", color = Orange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Niveau ${walletTier.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Section Services & Outils (Alibaba Grid style)
                Text("Mes Services", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp
                ) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            ServiceGridItem(Icons.Default.Favorite, "Favoris", Green) { onWishlistClick() }
                            ServiceGridItem(Icons.Default.Storefront, "Suivis", Color(0xFF1565C0)) { onFollowedShopsClick() }
                            ServiceGridItem(Icons.Default.Chat, "Messages", Green) { onMessagesClick() }
                            ServiceGridItem(Icons.Default.ConfirmationNumber, "Coupons", Amber) { onLoyaltyClick() }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth()) {
                            ServiceGridItem(Icons.Default.Notifications, "Notifications", Orange) { onNotifPrefsClick() }
                            ServiceGridItem(Icons.Default.Group, "Groupés", Green) { onGroupBuysClick() }
                            ServiceGridItem(Icons.Default.Map, "Boutiques", Color(0xFFE91E63)) { onShopsMapClick() }
                            ServiceGridItem(Icons.Default.SupportAgent, "Aide", Color(0xFF00BCD4)) { }
                            ServiceGridItem(Icons.Default.Settings, "Paramètres", Color.Gray) { onSettingsClick() }
                        }
                    }
                }

                // Section Spéciale Vendeur/Admin
                if (userRole == "vendor" || userRole == "admin") {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp
                    ) {
                        Column {
                            if (userRole == "vendor") {
                                ProfileMenuItem(Icons.Default.Storefront, "Espace Vendeur", "Gérez vos produits et ventes", Green) { onVendorDashboardClick() }
                            }
                            if (userRole == "admin") {
                                if (userRole == "vendor") HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = Color(0xFFF5F5F5))
                                ProfileMenuItem(Icons.Default.AdminPanelSettings, "Administration", "Gérer la plateforme", BlueAccent) { onAdminDashboardClick() }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Déconnexion
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 0.5.dp
                ) {
                    ProfileMenuItem(Icons.AutoMirrored.Filled.Logout, "Se déconnecter", "", Color.Red) { onLogout() }
                }

            } else {
                // Utilisateur non connecté
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = Green)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Connectez-vous", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Pour accéder à vos commandes, favoris et paramètres", color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            Text("Se connecter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Créer un compte", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
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
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(24.dp), tint = Green)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Green)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = if (tint != Color.Unspecified) tint else Green)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (tint != Color.Unspecified) tint else Color.Unspecified)
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
        }
    }
}
