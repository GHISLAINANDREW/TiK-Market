package com.tik_market.ui.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiClient
import com.tik_market.theme.*
import com.tik_market.ui.components.decodeDataUrlToImageBitmap
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.ui.components.rememberImagePickerLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit, onProfileUpdated: (com.tik_market.api.ApiUser) -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = ApiClient.getCurrentUser()

    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var phone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var location by remember { mutableStateOf(currentUser?.location ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var avatarUrl by remember { mutableStateOf(currentUser?.avatar ?: "") }
    var coverUrl by remember { mutableStateOf(currentUser?.coverPhoto ?: "") }
    
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var isSaving by remember { mutableStateOf(false) }

    // Load images
    LaunchedEffect(currentUser) {
        if (avatarUrl.isNotBlank()) {
            val fullUrl = if (avatarUrl.startsWith("http")) avatarUrl else "${ApiClient.baseUrl.trimEnd('/')}/${avatarUrl.trimStart('/')}"
            avatarBitmap = loadImageFromUrl(fullUrl)
        }
        if (coverUrl.isNotBlank()) {
            val fullUrl = if (coverUrl.startsWith("http")) coverUrl else "${ApiClient.baseUrl.trimEnd('/')}/${coverUrl.trimStart('/')}"
            coverBitmap = loadImageFromUrl(fullUrl)
        }
    }

    val avatarPicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            scope.launch {
                try {
                    val uploadedUrl = ApiClient.uploadImage(result.dataUrl, result.fileName)
                    avatarUrl = uploadedUrl
                    avatarBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Erreur avatar: ${e.message}")
                }
            }
        }
    }

    val coverPicker = rememberImagePickerLauncher { result ->
        if (result != null) {
            scope.launch {
                try {
                    val uploadedUrl = ApiClient.uploadImage(result.dataUrl, result.fileName)
                    coverUrl = uploadedUrl
                    coverBitmap = decodeDataUrlToImageBitmap(result.dataUrl)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Erreur couverture: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    isSaving = true
                                    val updated = ApiClient.updateUserProfile(
                                        name = name,
                                        phone = phone,
                                        location = location,
                                        avatar = avatarUrl,
                                        coverPhoto = coverUrl,
                                        password = password.ifBlank { null }
                                    )
                                    onProfileUpdated(updated)
                                    snackbarHostState.showSnackbar("✅ Profil mis à jour")
                                    onBack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("❌ Erreur: ${e.message}")
                                } finally {
                                    isSaving = false
                                }
                            }
                        }) {
                            Text("Enregistrer", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(BackgroundGray)
        ) {
            // Header: Cover and Avatar
            Box(Modifier.height(200.dp).fillMaxWidth()) {
                // Cover Photo
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.LightGray)
                        .clickable { coverPicker() }
                ) {
                    if (coverBitmap != null) {
                        Image(coverBitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, Modifier.size(40.dp), tint = Color.White)
                        }
                    }
                    // Overlay change icon
                    Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                // Avatar Photo
                Box(
                    Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 0.dp) // center the avatar on the line
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Color.White)
                        .clickable { avatarPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(avatarBitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, Modifier.size(50.dp), tint = Color.Gray)
                    }
                    // Overlay camera icon
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                        Surface(shape = CircleShape, color = Green, modifier = Modifier.size(28.dp).border(2.dp, Color.White, CircleShape)) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Form Fields
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Informations personnelles", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localisation (Ville)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    placeholder = { Text("ex: Bafoussam") }
                )

                Spacer(Modifier.height(24.dp))
                Text("Sécurité", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Green)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    },
                    placeholder = { Text("Laisser vide pour ne pas changer") }
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSaving = true
                                val updated = ApiClient.updateUserProfile(
                                    name = name,
                                    phone = phone,
                                    location = location,
                                    avatar = avatarUrl,
                                    coverPhoto = coverUrl,
                                    password = password.ifBlank { null }
                                )
                                onProfileUpdated(updated)
                                snackbarHostState.showSnackbar("✅ Profil mis à jour")
                                onBack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Erreur: ${e.message}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Mettre à jour le profil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
