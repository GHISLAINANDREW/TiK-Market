package com.dschangmarket.ui.vendor

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.rememberImagePickerLauncher
import com.dschangmarket.ui.components.decodeDataUrlToImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch

private val categories = listOf(
    "Alimentation", "Mode", "Électronique", "Artisanat",
    "Boutique", "Services", "Agriculture", "Autres"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateShopScreen(
    onBack: () -> Unit,
    onShopCreated: (shopName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Dschang") }
    var category by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var shopImageBase64 by remember { mutableStateOf<String?>(null) }
    var shopImageFileName by remember { mutableStateOf("shop_profile.jpg") }
    var shopImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val scope = rememberCoroutineScope()

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            shopImageBase64 = result.dataUrl
            shopImageFileName = result.fileName
            shopImageBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Créer ma boutique", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, navigationIconContentColor = Color.White)
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Profile Picture Section
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Green, CircleShape)
                            .clickable { imagePicker() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (shopImageBitmap != null) {
                            Image(
                                bitmap = shopImageBitmap!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = Green, modifier = Modifier.size(32.dp))
                                Text("Photo", fontSize = 10.sp, color = Green)
                            }
                        }
                    }
                    TextButton(onClick = { imagePicker() }) {
                        Text("Ajouter une photo de boutique", color = Green, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text("Informations de la boutique", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = null },
                        label = { Text("Nom de la boutique *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Store, null, tint = Green) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = phone, onValueChange = { phone = it; errorMessage = null },
                        label = { Text("Téléphone *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Phone, null, tint = Green) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = location, onValueChange = { location = it; errorMessage = null },
                        label = { Text("Localisation *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Green) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    
                    Spacer(Modifier.height(8.dp))
                    Text("Suggestions :", fontSize = 11.sp, color = Color.Gray)
                    FlowRow(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Centre-ville", "Campus A", "Campus B", "Foto", "Foréké", "Keleng").forEach { quartier ->
                            FilterChip(
                                selected = location == quartier,
                                onClick = { location = quartier; errorMessage = null },
                                label = { Text(quartier, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenSurface,
                                    selectedLabelColor = Green
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text("Catégorie", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = category, onValueChange = {},
                            readOnly = true, label = { Text("Catégorie *") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; categoryExpanded = false; errorMessage = null })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMessage = "Veuillez saisir le nom de la boutique"
                        phone.isBlank() -> errorMessage = "Veuillez saisir le numéro de téléphone"
                        location.isBlank() -> errorMessage = "Veuillez saisir la localisation"
                        category.isBlank() -> errorMessage = "Veuillez sélectionner une catégorie"
                        else -> {
                            loading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    var imageUrl: String? = null
                                    if (shopImageBase64 != null) {
                                        imageUrl = ApiClient.uploadImage(shopImageBase64!!, shopImageFileName)
                                    }
                                    ApiClient.createShop(name.trim(), description.trim(), phone.trim(), location.trim(), category, imageUrl)
                                    onShopCreated(name.trim())
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Une erreur est survenue"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Créer ma boutique", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
