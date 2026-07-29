package com.dschangmarket.ui.vendor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import com.dschangmarket.ui.components.loadImageFromUrl
import com.dschangmarket.ui.components.decodeDataUrlToImageBitmap
import com.dschangmarket.ui.components.rememberImagePickerLauncher
import com.dschangmarket.data.models.SampleData
import com.dschangmarket.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    onSave: (ProductForm) -> Unit,
    editProduct: ProductForm? = null
) {
    var form by remember { mutableStateOf(editProduct ?: ProductForm()) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val categories = SampleData.categories.map { it.name }
    val isEditing = editProduct != null

    BoxWithConstraints {
        val isCompact = maxWidth < 480.dp

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (isEditing) "Modifier le produit" else "Nouveau produit", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            actions = {
                TextButton(onClick = {
                    saving = true
                    onSave(form)
                }, enabled = !saving && form.title.isNotBlank() && form.price.isNotBlank()) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Publier", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Green, titleContentColor = Color.White, actionIconContentColor = Color.White)
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5)).verticalScroll(rememberScrollState())
        ) {
            // Multiple Image Picker
            Column(Modifier.padding(16.dp)) {
                Text("Photos du produit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Existing Images
                    form.imageUrls.forEach { url ->
                        Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))) {
                            var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                            LaunchedEffect(url) { bitmap = loadImageFromUrl(url) }
                            if (bitmap != null) {
                                Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            IconButton(
                                onClick = { form = form.copy(imageUrls = form.imageUrls - url) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    // New Images
                    form.newImages.forEach { img ->
                        Box(Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))) {
                            val bitmap = decodeDataUrlToImageBitmap(img.dataUrl)
                            if (bitmap != null) {
                                Image(bitmap, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            IconButton(
                                onClick = { form = form.copy(newImages = form.newImages - img) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    // Add Button
                    val picker = rememberImagePickerLauncher { res ->
                        if (res != null) {
                            form = form.copy(newImages = form.newImages + NewImageData(res.dataUrl, res.fileName))
                        }
                    }
                    Box(
                        Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clickable { picker() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, null, tint = Green)
                            Text("Ajouter", fontSize = 10.sp, color = Green)
                        }
                    }
                }
            }

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text("Informations produit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(value = form.title, onValueChange = { form = form.copy(title = it) },
                        label = { Text("Titre du produit *") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(value = form.description, onValueChange = { form = form.copy(description = it) },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    Spacer(Modifier.height(12.dp))

                    if (isCompact) {
                        OutlinedTextField(value = form.price, onValueChange = { form = form.copy(price = it) },
                            label = { Text("Prix (FCFA) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp), leadingIcon = { Text("F", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = form.comparePrice, onValueChange = { form = form.copy(comparePrice = it) },
                            label = { Text("Ancien prix") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = form.price, onValueChange = { form = form.copy(price = it) },
                                label = { Text("Prix (FCFA) *") }, modifier = Modifier.weight(1f), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp), leadingIcon = { Text("F", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            OutlinedTextField(value = form.comparePrice, onValueChange = { form = form.copy(comparePrice = it) },
                                label = { Text("Ancien prix") }, modifier = Modifier.weight(1f), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (isCompact) {
                        OutlinedTextField(value = form.stock, onValueChange = { form = form.copy(stock = it) },
                            label = { Text("Stock") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = form.unit, onValueChange = { form = form.copy(unit = it) },
                            label = { Text("Unité") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = form.stock, onValueChange = { form = form.copy(stock = it) },
                                label = { Text("Stock") }, modifier = Modifier.weight(1f), singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                            OutlinedTextField(value = form.unit, onValueChange = { form = form.copy(unit = it) },
                                label = { Text("Unité") }, modifier = Modifier.weight(1f), singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                        OutlinedTextField(
                            value = form.category, onValueChange = {},
                            readOnly = true, label = { Text("Catégorie") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green))
                        ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { form = form.copy(category = cat); showCategoryMenu = false })
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Publier en story", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Le produit apparaîtra dans la section story de l'accueil", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = form.isStory,
                            onCheckedChange = { form = form.copy(isStory = it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Green)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
    } // BoxWithConstraints
}
