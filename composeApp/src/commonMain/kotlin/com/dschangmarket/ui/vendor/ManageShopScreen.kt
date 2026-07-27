package com.dschangmarket.ui.vendor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.api.ApiClient
import com.dschangmarket.api.ApiProduct
import com.dschangmarket.api.ApiVendorInteractionsResponse
import com.dschangmarket.api.ApiInteractionUser
import com.dschangmarket.api.ApiProductReview
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.rememberImagePickerLauncher
import com.dschangmarket.ui.components.decodeDataUrlToImageBitmap
import com.dschangmarket.ui.components.loadImageFromUrl
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageShopScreen(
    onBack: () -> Unit,
    shopName: String,
    onSaveShop: (name: String, description: String, phone: String, location: String, category: String) -> Unit,
    onEditProduct: (productId: Int, title: String, description: String, price: String, comparePrice: String, category: String, stock: String, unit: String, imageUrl: String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    refreshSignal: Int = 0
) {
    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

    var products by remember { mutableStateOf<List<ApiProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditForm by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf<Int?>(null) }
    var shopId by remember { mutableStateOf(0) }
    var editName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editLogoUrl by remember { mutableStateOf<String?>(null) }
    var existingLogoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var editSaving by remember { mutableStateOf(false) }
    var editSuccess by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var newImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var newImageBase64 by remember { mutableStateOf<String?>(null) }
    var newImageFileName by remember { mutableStateOf("") }

    val imagePicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            newImageBase64 = result.dataUrl
            newImageFileName = result.fileName
            newImageBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
        }
    }

    // Product interaction dialog (likes, reviews, subscribers)
    var showInteractionDialog by remember { mutableStateOf(false) }
    var selectedProductForInteractions by remember { mutableStateOf<ApiProduct?>(null) }
    var interactionData by remember { mutableStateOf<ApiVendorInteractionsResponse?>(null) }
    var interactionLoading by remember { mutableStateOf(false) }
    var interactionTab by remember { mutableStateOf(0) }

    // Load shop products and details
    LaunchedEffect(refreshSignal) {
        isLoading = true
        try {
            val shop = ApiClient.fetchShopByVendor()
            if (shop != null) {
                shopId = shop.id
                editName = shop.name
                editDescription = shop.description
                editPhone = shop.phone
                editLocation = shop.location
                editCategory = shop.category
                editLogoUrl = shop.logo
                
                // Load logo bitmap if exists
                if (!shop.logo.isNullOrBlank()) {
                    val cleanBase = ApiClient.baseUrl.trimEnd('/')
                    val cleanPath = shop.logo.trimStart('/', '\\').replace("\\", "/")
                    val finalUrl = if (shop.logo.startsWith("http")) shop.logo else "$cleanBase/$cleanPath"
                    existingLogoBitmap = loadImageFromUrl(finalUrl)
                }

                val allProducts = ApiClient.fetchProducts(includeInactive = true)
                products = allProducts.filter { it.shopId == shop.id }
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gérer la boutique", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    TextButton(onClick = { showEditForm = !showEditForm }) {
                        Text(
                            if (showEditForm) "Terminé" else "Modifier",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shop info header
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Store,
                                null,
                                Modifier.size(32.dp),
                                tint = Green
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(shopName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "${products.size} produits",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            if (!showEditForm) {
                                TextButton(onClick = { showEditForm = true }) {
                                    Text("Modifier", color = Green, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Edit shop form
                        if (showEditForm) {
                            Spacer(Modifier.height(16.dp))
                            
                            // Shop Logo Edit
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF0F0F0))
                                        .border(1.dp, Green.copy(alpha = 0.5f), CircleShape)
                                        .clickable { imagePicker() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (newImageBitmap != null) {
                                        Image(bitmap = newImageBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else if (existingLogoBitmap != null) {
                                        Image(bitmap = existingLogoBitmap!!, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else if (!editLogoUrl.isNullOrBlank()) {
                                        // Still loading or failed to load
                                        Box(Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Green)
                                        }
                                    } else {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
                                    }
                                    
                                    // Camera Overlay
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                                        Surface(Modifier.size(24.dp), shape = CircleShape, color = Green, shadowElevation = 2.dp) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(value = editName, onValueChange = { editName = it },
                                label = { Text("Nom de la boutique") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = editDescription, onValueChange = { editDescription = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = editPhone, onValueChange = { editPhone = it },
                                label = { Text("Téléphone") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = editLocation, onValueChange = { editLocation = it },
                                label = { Text("Localisation (Quartier/Rue)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                                    IconButton(onClick = { 
                                        if (editLocation.isNotBlank()) {
                                            val encoded = editLocation.replace(" ", "+")
                                            uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$encoded+Dschang+Cameroun")
                                        }
                                    }) {
                                        Icon(Icons.Default.LocationOn, null, tint = Green)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            
                            // Suggestions de quartiers de Dschang
                            FlowRow(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Centre-ville", "Campus A", "Campus B", "Foto", "Foréké", "Keleng").forEach { quartier ->
                                    FilterChip(
                                        selected = editLocation == quartier,
                                        onClick = { editLocation = quartier },
                                        label = { Text(quartier, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GreenSurface,
                                            selectedLabelColor = Green
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            if (editSuccess != null) {
                                Surface(
                                    Modifier.fillMaxWidth(),
                                    color = GreenSurface,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(editSuccess!!, color = Green, fontSize = 13.sp,
                                        modifier = Modifier.padding(12.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        showEditForm = false
                                        editSuccess = null
                                        // Reload shop data
                                        scope.launch {
                                            try {
                                                val s = ApiClient.fetchShopByVendor()
                                                if (s != null) {
                                                    editName = s.name; editDescription = s.description
                                                    editPhone = s.phone; editLocation = s.location; editCategory = s.category
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Annuler") }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            editSaving = true
                                            editSuccess = null
                                            try {
                                                var finalLogoUrl = editLogoUrl
                                                if (newImageBase64 != null) {
                                                    finalLogoUrl = ApiClient.uploadImage(newImageBase64!!, newImageFileName)
                                                }
                                                
                                                ApiClient.updateShop(
                                                    shopId = shopId,
                                                    name = editName,
                                                    description = editDescription,
                                                    phone = editPhone,
                                                    location = editLocation,
                                                    category = editCategory,
                                                    imageUrl = finalLogoUrl
                                                )
                                                editSuccess = "Boutique mise à jour ✓"
                                                editLogoUrl = finalLogoUrl
                                                newImageBase64 = null
                                                newImageBitmap = null
                                            } catch (e: Exception) {
                                                editSuccess = "Erreur : ${e.message}"
                                            }
                                            editSaving = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                                    enabled = !editSaving && editName.isNotBlank()
                                ) {
                                    if (editSaving) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Enregistrer", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Products header
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Produits",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${products.size} article(s)",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green)
                    }
                }
            } else if (products.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Store,
                                null,
                                Modifier.size(48.dp),
                                tint = Color.LightGray
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aucun produit",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            Text(
                                "Ajoutez votre premier produit !",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            } else {
                // List products with delete buttons
                items(products) { prod ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.clickable {
                            selectedProductForInteractions = prod
                            interactionLoading = true
                            showInteractionDialog = true
                            scope.launch {
                                try {
                                    interactionData = ApiClient.fetchVendorInteractions(prod.id)
                                } catch (_: Exception) {
                                    interactionData = null
                                }
                                interactionLoading = false
                            }
                        }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon placeholder
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(GreenSurface, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    prod.title.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Green
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    prod.title,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${prod.price} FCFA • ${prod.stock} en stock",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            // Delete button
                            if (deleteConfirm == prod.id) {
                                Row {
                                    TextButton(onClick = { deleteConfirm = null }) {
                                        Text("Annuler", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                ApiClient.deleteProduct(prod.id)
                                                products = products.filter { it.id != prod.id }
                                            } catch (_: Exception) { }
                                            deleteConfirm = null
                                        }
                                    }) {
                                        Text("Confirmer", color = RedAccent, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                Row {
                                    // Edit button
                                    IconButton(onClick = {
                                        onEditProduct(
                                            prod.id, prod.title, prod.description,
                                            prod.price.toString(), (prod.comparePrice ?: 0).toString(),
                                            prod.category, prod.stock.toString(), prod.unit, prod.imageUrl
                                        )
                                    }) {
                                        Icon(Icons.Default.Edit, null, Modifier.size(20.dp), tint = Green)
                                    }
                                    // Delete button
                                    IconButton(onClick = { deleteConfirm = prod.id }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            Modifier.size(20.dp),
                                            tint = RedAccent.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // ─── Product Interaction Dialog (Likes, Avis, Abonnés) ───
    if (showInteractionDialog && selectedProductForInteractions != null) {
        AlertDialog(
            onDismissRequest = { showInteractionDialog = false },
            title = {
                Column {
                    Text(selectedProductForInteractions!!.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Interactions clients", fontSize = 12.sp, color = Color.Gray)
                }
            },
            text = {
                Column(modifier = Modifier.width(350.dp).heightIn(max = 400.dp)) {
                    if (interactionLoading) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        // Tabs: Likes | Avis | Abonnés
                        TabRow(selectedTabIndex = interactionTab, containerColor = Color.White, contentColor = Green) {
                            val likesCount = interactionData?.likes?.size ?: 0
                            val reviewsCount = interactionData?.reviews?.size ?: 0
                            val subsCount = interactionData?.subscribers?.size ?: 0
                            Tab(selected = interactionTab == 0, onClick = { interactionTab = 0 }, text = { Text("❤️ $likesCount", fontSize = 12.sp) })
                            Tab(selected = interactionTab == 1, onClick = { interactionTab = 1 }, text = { Text("⭐ $reviewsCount", fontSize = 12.sp) })
                            Tab(selected = interactionTab == 2, onClick = { interactionTab = 2 }, text = { Text("👥 $subsCount", fontSize = 12.sp) })
                        }
                        Spacer(Modifier.height(8.dp))

                        when (interactionTab) {
                            0 -> { // Likes
                                val likes = interactionData?.likes ?: emptyList()
                                if (likes.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.FavoriteBorder, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text("Aucun like", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(likes) { like ->
                                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Green)
                                                    Spacer(Modifier.width(8.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(like.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                        Text(like.email, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Text(like.likedAt.take(10), fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> { // Reviews / Avis
                                val reviews = interactionData?.reviews ?: emptyList()
                                if (reviews.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.StarBorder, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text("Aucun avis", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(reviews) { review ->
                                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                                                Column(Modifier.padding(8.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = Green)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(review.userName, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                        Text("${review.rating}/5", fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                                    }
                                                    if (review.comment.isNotBlank()) {
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(review.comment, fontSize = 12.sp, color = Color.DarkGray)
                                                    }
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(review.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Abonnés (subscribers)
                                val subs = interactionData?.subscribers ?: emptyList()
                                if (subs.isEmpty()) {
                                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.PeopleOutline, null, tint = Color.LightGray, modifier = Modifier.size(32.dp))
                                            Spacer(Modifier.height(4.dp))
                                            Text("Aucun abonné", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(subs) { sub ->
                                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5)) {
                                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp), tint = Green)
                                                    Spacer(Modifier.width(8.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(sub.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                                        Text(sub.email, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                    Text(sub.subscribedAt.take(10), fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInteractionDialog = false }) { Text("Fermer") }
            }
        )
    }
    } // BoxWithConstraints
}
