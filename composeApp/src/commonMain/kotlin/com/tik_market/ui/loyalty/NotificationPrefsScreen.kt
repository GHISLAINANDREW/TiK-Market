package com.tik_market.ui.loyalty

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiNotificationPreferences
import com.tik_market.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPrefsScreen(onBack: () -> Unit) {
    var prefs by remember { mutableStateOf<ApiNotificationPreferences?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
            prefs = ApiClient.fetchNotificationPrefs()
        } catch (_: Exception) { }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Préférences notifications", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Notifications push", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Activez ou désactivez les types de notifications que vous souhaitez recevoir.", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        val current = prefs ?: ApiNotificationPreferences()

                        NotifToggle("Nouveaux produits", "Promotions et nouveaux arrivages", Icons.Default.Store, Green, current.allowProduct) {
                            prefs = current.copy(allowProduct = it)
                        }
                        NotifToggle("Mises à jour commandes", "Statut de vos commandes", Icons.Default.ShoppingCart, Orange, current.allowOrder) {
                            prefs = current.copy(allowOrder = it)
                        }
                        NotifToggle("Offres promotionnelles", "Réductions et offres spéciales", Icons.Default.Sell, Color(0xFFE91E63), current.allowPromo) {
                            prefs = current.copy(allowPromo = it)
                        }
                        NotifToggle("Messages", "Notifications de chat", Icons.Default.Chat, Color(0xFF1565C0), current.allowMessage) {
                            prefs = current.copy(allowMessage = it)
                        }
                        NotifToggle("Système", "Informations générales", Icons.Default.Settings, Color.Gray, current.allowSystem) {
                            prefs = current.copy(allowSystem = it)
                        }

                        HorizontalDivider(color = Color(0xFFF0F0F0), modifier = Modifier.padding(vertical = 8.dp))

                        NotifToggle("Push activé", "Recevoir les notifications même en arrière-plan", Icons.Default.Notifications, Green, current.pushEnabled) {
                            prefs = current.copy(pushEnabled = it)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Bouton sauvegarder
                Button(
                    onClick = {
                        val p = prefs ?: return@Button
                        scope.launch {
                            saving = true
                            try {
                                val ok = ApiClient.updateNotificationPrefs(p)
                                if (ok) snackbar.showSnackbar("Préférences enregistrées")
                                else snackbar.showSnackbar("Erreur lors de la sauvegarde")
                            } catch (e: Exception) {
                                snackbar.showSnackbar(e.message ?: "Erreur")
                            }
                            saving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    enabled = !saving
                ) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text("Enregistrer", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotifToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Green, checkedThumbColor = Color.White)
        )
    }
}
