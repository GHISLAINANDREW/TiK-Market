package com.tik_market.ui.vendor

import com.tik_market.api.*
import com.tik_market.api.dto.*

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
import com.tik_market.theme.*
import com.tik_market.ui.components.rememberImagePickerLauncher
import com.tik_market.ui.components.decodeDataUrlToImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tik_market.utils.getCurrentLocationLatLng
import com.tik_market.utils.getCurrentLocationName
import com.tik_market.utils.getPlaceName
import com.tik_market.ui.components.loadImageFromUrl
import androidx.compose.ui.text.style.TextAlign
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.delay
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
    var showLocationPicker by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var shopImageBase64 by remember { mutableStateOf<String?>(null) }
    var shopImageFileName by remember { mutableStateOf("shop_profile.jpg") }
    var shopImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val scope = rememberCoroutineScope()
    val ts = LocalAppStrings.current

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            shopImageBase64 = result.dataUrl
            shopImageFileName = result.fileName
            shopImageBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(ts.createMyShop, fontWeight = FontWeight.SemiBold) },
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
                        Text(ts.addShopPhoto, color = Green, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text(ts.shopInfo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(value = name, onValueChange = { name = it; errorMessage = null },
                        label = { Text(ts.shopNameRequired) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Store, null, tint = Green) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text(ts.description) }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = phone, onValueChange = { phone = it; errorMessage = null },
                        label = { Text(ts.shopPhoneRequired) }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.Phone, null, tint = Green) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = location, onValueChange = { location = it; errorMessage = null },
                            label = { Text(ts.locationRequiredField) }, modifier = Modifier.weight(1f),
                            singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Green) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        
                        Spacer(Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { showLocationPicker = true },
                            modifier = Modifier.padding(top = 8.dp).background(Green.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Map, ts.chooseOnMap, tint = Green)
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Text(ts.suggestions, fontSize = 11.sp, color = Color.Gray)
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
                    Text(ts.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = category, onValueChange = {},
                            readOnly = true, label = { Text(ts.categoryRequired) },
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
                        name.isBlank() -> errorMessage = ts.errShopName
                        phone.isBlank() -> errorMessage = ts.errPhoneField
                        location.isBlank() -> errorMessage = ts.errLocationField
                        category.isBlank() -> errorMessage = ts.errCategoryField
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
                                    errorMessage = e.message ?: ts.errGeneric
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
                    Text(ts.createMyShop, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onDismiss = { showLocationPicker = false },
            onLocationSelected = { address, _, _ ->
                location = address
                showLocationPicker = false
            }
        )
    }
}

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (String, Double?, Double?) -> Unit
) {
    val ts = LocalAppStrings.current
    var lat by remember { mutableStateOf(5.4627) } // Dschang approx
    var lng by remember { mutableStateOf(10.0533) }
    var zoom by remember { mutableStateOf(15) }
    var isLoading by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf(ts.loading) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        getCurrentLocationLatLng { lLat, lLng ->
            if (lLat != null && lLng != null) {
                lat = lLat
                lng = lLng
            }
            getPlaceName(lat, lng) { name ->
                address = name
                isLoading = false
            }
        }
    }

    // Effect to update address when lat/lng changes (with debounce)
    LaunchedEffect(lat, lng) {
        delay(800)
        getPlaceName(lat, lng) { name ->
            address = name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ts.chooseOnMap) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    // Static Map
                    val mapUrl = "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lng&zoom=$zoom&size=600x400&markers=$lat,$lng"
                    var mapBitmap by remember(lat, lng, zoom) { mutableStateOf<ImageBitmap?>(null) }
                    
                    LaunchedEffect(lat, lng, zoom) {
                        try {
                            mapBitmap = loadImageFromUrl(mapUrl)
                        } catch (_: Exception) {}
                    }
                    
                    if (mapBitmap != null) {
                        Image(mapBitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        // Central Pin (Fixed in center of map)
                        Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(32.dp).align(Alignment.Center).offset(y = (-16).dp))
                    } else {
                        CircularProgressIndicator(color = Green)
                    }
                    
                    // Zoom controls
                    Column(Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                        IconButton(onClick = { if (zoom < 19) zoom++ }, modifier = Modifier.background(Color.White.copy(0.7f), CircleShape).size(32.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        IconButton(onClick = { if (zoom > 10) zoom-- }, modifier = Modifier.background(Color.White.copy(0.7f), CircleShape).size(32.dp)) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Text(address, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
                
                Spacer(Modifier.height(16.dp))
                // D-Pad for moving (since we don't have interactive touch map)
                val moveStep = 0.002 / (zoom - 13).coerceAtLeast(1)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { lat += moveStep }) { Icon(Icons.Default.ArrowUpward, null) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { lng -= moveStep }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        Spacer(Modifier.width(16.dp))
                        IconButton(onClick = { lng += moveStep }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                    }
                    IconButton(onClick = { lat -= moveStep }) { Icon(Icons.Default.ArrowDownward, null) }
                }
                
                Button(
                    onClick = {
                        isLoading = true
                        getCurrentLocationLatLng { lLat, lLng ->
                            if (lLat != null && lLng != null) {
                                lat = lLat
                                lng = lLng
                                getPlaceName(lat, lng) { name ->
                                    address = name
                                    isLoading = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green.copy(alpha = 0.1f), contentColor = Green),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ts.myPosition, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onLocationSelected(address, lat, lng) }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text(ts.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(ts.cancel) }
        }
    )
}
