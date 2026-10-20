package com.tik_market.ui.vendor

import com.tik_market.api.*
import com.tik_market.api.dto.*

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Green
import com.tik_market.theme.Orange
import com.tik_market.ui.components.decodeDataUrlToImageBitmap
import com.tik_market.ui.components.rememberImagePickerLauncher
import com.tik_market.utils.getCurrentLocationLatLng
import com.tik_market.utils.rememberGoogleAuthManager
import kotlinx.coroutines.launch

private val categories = listOf(
    "Alimentation", "Mode", "Électronique", "Artisanat",
    "Boutique", "Services", "Agriculture", "Autres"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorRegistrationWizardScreen(
    onBack: () -> Unit,
    onLoginSuccess: (token: String, name: String, role: String) -> Unit,
    onRegistrationComplete: (shopName: String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Personal/Google, 2: Shop & Immutable GPS, 3: KYC & Payment (Skippable)
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    // Step 1: Personal Info
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Step 2: Shop Info & GPS
    var shopName by remember { mutableStateOf("") }
    var shopDescription by remember { mutableStateOf("") }
    var shopPhone by remember { mutableStateOf("") }
    var shopCategory by remember { mutableStateOf("Alimentation") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var locationCaptured by remember { mutableStateOf(false) }
    var shopLogoBase64 by remember { mutableStateOf<String?>(null) }
    var shopLogoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Step 3: KYC (5 photo fields) & Merchant Payment
    var cniFrontLoaded by remember { mutableStateOf(false) }
    var cniBackLoaded by remember { mutableStateOf(false) }
    var interiorLoaded by remember { mutableStateOf(false) }
    var exteriorLoaded by remember { mutableStateOf(false) }
    var signPlateLoaded by remember { mutableStateOf(false) }

    var cniFrontBase64 by remember { mutableStateOf<String?>(null) }
    var cniBackBase64 by remember { mutableStateOf<String?>(null) }
    var interiorBase64 by remember { mutableStateOf<String?>(null) }
    var exteriorBase64 by remember { mutableStateOf<String?>(null) }
    var signBase64 by remember { mutableStateOf<String?>(null) }

    var niuNumber by remember { mutableStateOf("") }
    var merchantAccountName by remember { mutableStateOf("") }
    var merchantAccountNumber by remember { mutableStateOf("") }

    val logoPicker = rememberImagePickerLauncher { res ->
        if (res != null) {
            shopLogoBase64 = res.dataUrl
            shopLogoBitmap = decodeDataUrlToImageBitmap(res.dataUrl)
        }
    }

    val googleAuthManager = rememberGoogleAuthManager()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inscription Partenaire — Étape $step / 3", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandTopBarColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Orange,
                trackColor = Color.LightGray
            )

            Spacer(Modifier.height(24.dp))

            errorMessage?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Text(it, color = Color(0xFFC62828), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                }
            }

            successMessage?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(it, color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                }
            }

            AnimatedContent(targetState = step) { currentStep ->
                when (currentStep) {
                    1 -> {
                        Column {
                            Text("Informations Personnelles", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                            Spacer(Modifier.height(6.dp))
                            Text("Créez votre compte personnel ou connectez-vous instantanément avec Google.", fontSize = 13.sp, color = Color.Gray)
                            
                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            val gData = googleAuthManager.signIn()
                                            if (gData != null && gData.idToken != null) {
                                                val res = ApiClient.googleLogin(gData.idToken, "Cameroun")
                                                onLoginSuccess(res.token, res.user.name, res.user.role)
                                                step = 2
                                            } else {
                                                errorMessage = "Connexion Google annulée ou échouée"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Erreur Google Sign-In"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Icon(Icons.Default.Email, null, tint = Color(0xFF4285F4))
                                Spacer(Modifier.width(12.dp))
                                Text("Continuer avec Google", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 15.sp)
                            }

                            Spacer(Modifier.height(20.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                                Text("  ou inscription classique  ", color = Color.Gray, fontSize = 12.sp)
                                Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                            }
                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = email, onValueChange = { email = it },
                                label = { Text("Adresse email") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phone, onValueChange = { phone = it },
                                label = { Text("Numéro de téléphone") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = password, onValueChange = { password = it },
                                label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                                    }
                                }
                            )

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    when {
                                        name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() -> errorMessage = "Veuillez remplir tous les champs"
                                        else -> {
                                            isLoading = true
                                            errorMessage = null
                                            scope.launch {
                                                try {
                                                    val res = ApiClient.register(name.trim(), email.trim(), phone.trim(), password, "vendor")
                                                    onLoginSuccess(res.token, res.user.name, res.user.role)
                                                    step = 2
                                                } catch (e: Exception) {
                                                    errorMessage = e.message ?: "Erreur d'inscription"
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Orange)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Suivant : Créer la boutique", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    2 -> {
                        Column {
                            Text("Informations de la Boutique", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                            Spacer(Modifier.height(6.dp))
                            Text("Définissez votre commerce. Attention : la position GPS est définitive et non modifiable une fois créée.", fontSize = 13.sp, color = Color.Red)

                            Spacer(Modifier.height(20.dp))

                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.dp, Orange, CircleShape)
                                        .clickable { logoPicker() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (shopLogoBitmap != null) {
                                        Image(shopLogoBitmap!!, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Store, null, tint = Orange, modifier = Modifier.size(36.dp))
                                    }
                                }
                            }
                            TextButton(onClick = { logoPicker() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Ajouter le logo de la boutique", color = Orange)
                            }

                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = shopName, onValueChange = { shopName = it },
                                label = { Text("Nom de la boutique") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = shopDescription, onValueChange = { shopDescription = it },
                                label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = shopPhone, onValueChange = { shopPhone = it },
                                label = { Text("Téléphone professionnel de la boutique") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                                OutlinedTextField(
                                    value = shopCategory, onValueChange = {},
                                    readOnly = true, label = { Text("Catégorie") },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable), singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(text = { Text(cat) }, onClick = { shopCategory = cat; categoryExpanded = false })
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFEBEE),
                                border = BorderStroke(1.dp, Color(0xFFEF9A9A))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFC62828))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Position GPS Immuable", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 15.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Vous devez être **physiquement dans votre boutique** pour capturer sa position exacte. Une fois enregistrée, cette position GPS ne pourra plus jamais être modifiée.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB71C1C),
                                        lineHeight = 18.sp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            getCurrentLocationLatLng { lat, lng ->
                                                if (lat != null && lng != null) {
                                                    latitude = lat
                                                    longitude = lng
                                                    locationCaptured = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (locationCaptured) Green else Color(0xFFC62828)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.MyLocation, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (locationCaptured) "Position GPS capturée sur place ✅" else "Capturer ma position GPS actuelle")
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    when {
                                        shopName.isBlank() || shopPhone.isBlank() -> errorMessage = "Veuillez remplir le nom et le téléphone de la boutique"
                                        !locationCaptured -> errorMessage = "Veuillez capturer votre position GPS sur place"
                                        else -> step = 3
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Orange)
                            ) {
                                Text("Suivant : KYC & Paiement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                            }
                        }
                    }
                    3 -> {
                        Column {
                            Text("Vérification KYC & Paiement (Optionnel)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                            Spacer(Modifier.height(6.dp))
                            Text("Ces informations certifient votre boutique. Vous pouvez remplir cette étape maintenant ou plus tard dans vos paramètres.", fontSize = 13.sp, color = Color.Gray)

                            Spacer(Modifier.height(20.dp))

                            OutlinedTextField(
                                value = niuNumber, onValueChange = { niuNumber = it },
                                label = { Text("Numéro NIU (Fiscal / Optionnel)") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = merchantAccountName, onValueChange = { merchantAccountName = it },
                                label = { Text("Nom du compte marchand / Mobile Money (Optionnel)") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = merchantAccountNumber, onValueChange = { merchantAccountNumber = it },
                                label = { Text("Numéro de compte / Marchand (Optionnel)") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(Modifier.height(24.dp))
                            Text("5 Photos KYC Requises (Optionnel pour l'instant)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))

                            val p1 = rememberImagePickerLauncher { if (it != null) { cniFrontLoaded = true; cniFrontBase64 = it.dataUrl } }
                            val p2 = rememberImagePickerLauncher { if (it != null) { cniBackLoaded = true; cniBackBase64 = it.dataUrl } }
                            val p3 = rememberImagePickerLauncher { if (it != null) { interiorLoaded = true; interiorBase64 = it.dataUrl } }
                            val p4 = rememberImagePickerLauncher { if (it != null) { exteriorLoaded = true; exteriorBase64 = it.dataUrl } }
                            val p5 = rememberImagePickerLauncher { if (it != null) { signPlateLoaded = true; signBase64 = it.dataUrl } }

                            OutlinedButton(onClick = { p1() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Icon(Icons.Default.Badge, null, tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(if (cniFrontLoaded) "1. CNI Recto ✅" else "1. CNI Recto")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { p2() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Icon(Icons.Default.Badge, null, tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(if (cniBackLoaded) "2. CNI Verso ✅" else "2. CNI Verso")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { p3() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Icon(Icons.Default.Store, null, tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(if (interiorLoaded) "3. Intérieur de la boutique ✅" else "3. Intérieur de la boutique")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { p4() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Icon(Icons.Default.Store, null, tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(if (exteriorLoaded) "4. Extérieur de la boutique ✅" else "4. Extérieur de la boutique")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { p5() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                Icon(Icons.Default.PhotoLibrary, null, tint = Orange)
                                Spacer(Modifier.width(8.dp))
                                Text(if (signPlateLoaded) "5. Plaque / Enseigne de la boutique ✅" else "5. Plaque / Enseigne de la boutique")
                            }

                            Spacer(Modifier.height(40.dp))

                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            var logoUrl: String? = null
                                            if (shopLogoBase64 != null) logoUrl = ApiClient.uploadImage(shopLogoBase64!!, "shop_logo.jpg")
                                            var cniFrontUrl: String? = null
                                            if (cniFrontBase64 != null) cniFrontUrl = ApiClient.uploadImage(cniFrontBase64!!, "cni_front.jpg")
                                            var cniBackUrl: String? = null
                                            if (cniBackBase64 != null) cniBackUrl = ApiClient.uploadImage(cniBackBase64!!, "cni_back.jpg")
                                            var interiorUrl: String? = null
                                            if (interiorBase64 != null) interiorUrl = ApiClient.uploadImage(interiorBase64!!, "interior.jpg")
                                            var exteriorUrl: String? = null
                                            if (exteriorBase64 != null) exteriorUrl = ApiClient.uploadImage(exteriorBase64!!, "exterior.jpg")
                                            var signUrl: String? = null
                                            if (signBase64 != null) signUrl = ApiClient.uploadImage(signBase64!!, "sign.jpg")

                                            ApiClient.createShop(shopName, shopDescription, shopPhone, "Dschang", shopCategory, logoUrl, cniFrontUrl, cniBackUrl, interiorUrl, exteriorUrl, signUrl)
                                            successMessage = "Compte marchand en cours de validation par l'équipe TiK-Market. Vous pouvez commencer à publier vos produits !"
                                            kotlinx.coroutines.delay(1500L)
                                            onRegistrationComplete(shopName)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Erreur lors de la création"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Green)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                                } else {
                                    Text("Terminer et soumettre ma boutique", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            var logoUrl: String? = null
                                            if (shopLogoBase64 != null) logoUrl = ApiClient.uploadImage(shopLogoBase64!!, "shop_logo.jpg")
                                            var cniFrontUrl: String? = null
                                            if (cniFrontBase64 != null) cniFrontUrl = ApiClient.uploadImage(cniFrontBase64!!, "cni_front.jpg")
                                            var cniBackUrl: String? = null
                                            if (cniBackBase64 != null) cniBackUrl = ApiClient.uploadImage(cniBackBase64!!, "cni_back.jpg")
                                            var interiorUrl: String? = null
                                            if (interiorBase64 != null) interiorUrl = ApiClient.uploadImage(interiorBase64!!, "interior.jpg")
                                            var exteriorUrl: String? = null
                                            if (exteriorBase64 != null) exteriorUrl = ApiClient.uploadImage(exteriorBase64!!, "exterior.jpg")
                                            var signUrl: String? = null
                                            if (signBase64 != null) signUrl = ApiClient.uploadImage(signBase64!!, "sign.jpg")

                                            ApiClient.createShop(shopName, shopDescription, shopPhone, "Dschang", shopCategory, logoUrl, cniFrontUrl, cniBackUrl, interiorUrl, exteriorUrl, signUrl)
                                            successMessage = "Compte marchand en cours de validation par l'équipe TiK-Market. Vous pouvez commencer à publier vos produits !"
                                            kotlinx.coroutines.delay(1500L)
                                            onRegistrationComplete(shopName)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Erreur lors de la création"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ignorer pour l'instant (Remplir plus tard dans les paramètres)", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
