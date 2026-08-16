package com.tik_market.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Orange
import com.tik_market.utils.LocalAppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    language: String = "fr",
    onSelectLanguage: (String) -> Unit = {},
    onAboutClick: () -> Unit = {},
    onLegalClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onDownloadApk: () -> Unit = {}
) {
    val s = LocalAppStrings.current
    var showLangDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.settings, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // Langue
            Surface(
                onClick = { showLangDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text("${s.language} : ${if (language == "fr") s.french else s.english}", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
            if (showLangDialog) {
                AlertDialog(
                    onDismissRequest = { showLangDialog = false },
                    title = { Text(s.language) },
                    text = {
                        Column {
                            listOf("fr", "en").forEach { code ->
                                val label = if (code == "fr") s.french else s.english
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSelectLanguage(code)
                                            showLangDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    if (language == code) {
                                        Icon(Icons.Default.Check, null, tint = Orange)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLangDialog = false }) { Text(s.cancel) }
                    }
                )
            }
            // Mode sombre (toggle)
            Surface(
                onClick = onToggleDarkMode,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text(s.darkMode, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(checkedTrackColor = Orange)
                    )
                }
            }
            // Notifications
            Surface(
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text(s.notifications, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
            // Mentions Légales
            Surface(
                onClick = onLegalClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text("Mentions Légales", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
            // Conditions d'Utilisation
            Surface(
                onClick = onTermsClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text("Conditions d'Utilisation", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Téléchargements
            Text("Téléchargements", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            // APK Android
            Surface(
                onClick = onDownloadApk,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Android, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("APK Android", fontSize = 15.sp)
                        Text("v1.0.0 — Installer l'APK", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
            // iOS (placeholder)
            Surface(
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneIphone, null, Modifier.size(22.dp), tint = Color.LightGray)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("App iOS", fontSize = 15.sp, color = Color.LightGray)
                        Text("Bientôt disponible", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // À propos
            Surface(
                onClick = onAboutClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, Modifier.size(22.dp), tint = Orange)
                    Spacer(Modifier.width(12.dp))
                    Text("À propos", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Color.LightGray)
                }
            }
        }
    }
}
