@file:OptIn(ExperimentalMaterial3Api::class)

package com.tik_market.ui.auth

import com.tik_market.api.*
import com.tik_market.api.dto.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.currentTimeMillis
import com.tik_market.theme.*
import com.tik_market.utils.format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AuthFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: String = "buyer",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVendorSignup: Boolean = false,
    val isOtpStep: Boolean = false,
    val otpCode: String = "",
    val otpExpiresAt: Long = 0L,
    val canResend: Boolean = false,
    val termsAccepted: Boolean = false
)

enum class AuthMode { Email, Google, Phone }

@Composable
fun AuthScreen(
    onLoginSuccess: (token: String, userName: String, userRole: String) -> Unit,
    onBack: () -> Unit,
    onTermsClick: () -> Unit = {},
    onLegalClick: () -> Unit = {},
    initialModeRegister: Boolean = false
) {
    println("[Auth] AuthScreen chargé (initialModeRegister=$initialModeRegister)")
    val s = com.tik_market.utils.LocalAppStrings.current
    var isLogin by remember { mutableStateOf(!initialModeRegister) }
    var authMode by remember { mutableStateOf<AuthMode>(AuthMode.Email) }
    var form by remember { mutableStateOf(AuthFormState()) }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val googleAuthManager = com.tik_market.utils.rememberGoogleAuthManager()

    // ── Animation du fond (dégradé vert/orange qui ondule) ──
    val infiniteTransition = rememberInfiniteTransition(label = "authBg")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAnim"
    )
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            Green.copy(alpha = 0.08f + animProgress * 0.06f),
            GreenSurface,
            GreenAccentSurface.copy(alpha = 0.3f + animProgress * 0.2f)
        ),
        start = Offset(0f, 0f),
        end = Offset(animProgress * 500f, animProgress * 500f)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isLogin) s.login else s.register, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding)) {
            val isCompact = maxWidth < 480.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
            ) {
                // Column d'abord (calque du bas)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(if (isCompact) 16.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.height(if (isCompact) 12.dp else 24.dp))

                    Text(
                        "Tik Market",
                        style = if (isCompact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = Green,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            form.isOtpStep -> s.authOtpTitle
                            authMode == AuthMode.Phone -> s.phoneLogin
                            isLogin -> s.loginContinue
                            else -> s.createYourAccount
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(if (isCompact) 20.dp else 28.dp))

                    Surface(
                        modifier = Modifier.widthIn(max = 400.dp),
                        shape = RoundedCornerShape(CardShapeMedium),
                        color = CardWhite,
                        shadowElevation = 2.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isCompact) 20.dp else 28.dp)
                        ) {
                            AnimatedVisibility(
                                visible = form.error != null,
                                enter = slideInVertically() + fadeIn(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = RedAccent.copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = RedAccent)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            form.error ?: "",
                                            color = RedAccent,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                                when {
                                    form.isOtpStep -> OtpStep(isLogin, form, { form = it }, scope, onLoginSuccess, onTermsClick, onLegalClick)
                                    authMode == AuthMode.Phone -> PhoneStep(isLogin, form, { form = it }, scope, { authMode = AuthMode.Email }, onTermsClick, onLegalClick)
                                    else -> EmailAuthStep(
                                        isLogin = isLogin,
                                        form = form,
                                        onFormChange = { form = it },
                                        showPassword = showPassword,
                                        onTogglePassword = { showPassword = !showPassword },
                                        scope = scope,
                                        onLoginSuccess = onLoginSuccess,
                                        onToggleMode = { isLogin = !isLogin },
                                        onTermsClick = onTermsClick,
                                        onLegalClick = onLegalClick
                                    )
                                }

                            if (!form.isOtpStep && authMode == AuthMode.Email) {
                                Spacer(Modifier.height(12.dp))
                                TextButton(
                                    onClick = { isLogin = !isLogin },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isLogin) s.noAccountSignup
                                        else s.alreadyAccountLogin,
                                        color = Green,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!form.isOtpStep && authMode == AuthMode.Email) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.weight(1f).height(1.dp).background(DividerGray))
                                    Text("  ${s.or}  ", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                                    Box(Modifier.weight(1f).height(1.dp).background(DividerGray))
                                }
                                Spacer(Modifier.height(12.dp))

                                SocialButton(
                                    text = s.continueWithGoogle,
                                    icon = Icons.Default.Email,
                                    iconTint = BlueAccent,
                                    textColor = TextPrimary,
                                    borderColor = DividerGray,
                                    onClick = {
                                        if (form.location.isBlank()) {
                                            form = form.copy(error = s.chooseCityRequired)
                                            return@SocialButton
                                        }
                                        scope.launch {
                                            form = form.copy(isLoading = true, error = null)
                                            val data = googleAuthManager.signIn()
                                            if (data?.idToken != null) {
                                                try {
                                                    val result = ApiClient.googleLogin(data.idToken, form.location)
                                                    onLoginSuccess(result.token, result.user.name, result.user.role)
                                                } catch (e: Exception) {
                                                    form = form.copy(error = e.message ?: s.serverLoginFailed)
                                                }
                                            } else {
                                                form = form.copy(error = s.googleLoginFailed)
                                            }
                                            form = form.copy(isLoading = false)
                                        }
                                    }
                                )
                                /*
                                Spacer(Modifier.height(8.dp))
                                SocialButton(
                                    text = "Téléphone (Cameroun)",
                                    icon = Icons.Default.Phone,
                                    iconTint = Green,
                                    textColor = TextPrimary,
                                    borderColor = DividerGray,
                                    onClick = { authMode = AuthMode.Phone }
                                )
                                */
                            }

                            if (!form.isOtpStep && authMode == AuthMode.Phone) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { authMode = AuthMode.Email }, modifier = Modifier.fillMaxWidth()) {
                                    Text(s.useEmailPassword, color = Green, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun EmailAuthStep(
    isLogin: Boolean,
    form: AuthFormState,
    onFormChange: (AuthFormState) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoginSuccess: (String, String, String) -> Unit,
    onToggleMode: () -> Unit,
    onTermsClick: () -> Unit,
    onLegalClick: () -> Unit
) {
    val s = com.tik_market.utils.LocalAppStrings.current
    // Track per-field validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true
        if (!isLogin) {
            if (form.location.isBlank()) { emailError = s.chooseCityRequired; valid = false }
            if (form.name.isBlank()) { nameError = s.nameRequired; valid = false } else nameError = null
            if (form.phone.isBlank()) { phoneError = s.phoneRequired; valid = false }
            else if (form.phone.replace(Regex("[^0-9]"), "").length < 8) { phoneError = s.invalidPhone; valid = false }
            else phoneError = null
        }
        if (form.email.isBlank()) { emailError = s.emailRequired; valid = false }
        else if (!form.email.contains("@") || !form.email.contains(".")) { emailError = s.invalidEmail; valid = false }
        else emailError = null
        if (form.password.isBlank()) { passwordError = s.passwordRequired; valid = false }
        else if (form.password.length < 4) { passwordError = s.passwordTooShort; valid = false }
        else passwordError = null
        if (!isLogin) {
            if (form.confirmPassword.isBlank()) { confirmError = s.confirmRequired; valid = false }
            else if (form.password != form.confirmPassword) { confirmError = s.passwordsDontMatch; valid = false }
            else confirmError = null
            
            if (!form.termsAccepted) {
                onFormChange(form.copy(error = s.acceptTermsRequired))
                valid = false
            }
        }
        return valid
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (isLogin) s.signIn else s.createAccountTitle,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // City Selector (Visible for both Login and Register to ensure location is set)
        var showCityPicker by remember { mutableStateOf(false) }
        // Lieu réel détecté (hors villes de l'app) : conservé lors de l'enregistrement
        var detectedPlace by remember { mutableStateOf<String?>(null) }

        // Détection automatique de la ville :
        // - proche (≤ 20 km) d'une ville de l'app → pré-remplir avec cette ville
        // - sinon → conserver le lieu réel de l'utilisateur (hors villes de l'app)
        LaunchedEffect(Unit) {
            com.tik_market.utils.getCurrentLocationLatLng { lat, lng ->
                if (lat != null && lng != null) {
                    val nearby = com.tik_market.utils.findNearbyAppCity(lat, lng)
                    if (nearby != null) {
                        detectedPlace = nearby.name
                        if (form.location.isBlank()) onFormChange(form.copy(location = nearby.name, error = null))
                    } else {
                        // Lieu hors villes de l'app : on conserve le lieu réel de l'utilisateur
                        com.tik_market.utils.getCurrentLocationName { placeName ->
                            detectedPlace = placeName
                            if (form.location.isBlank() && placeName.isNotBlank() && placeName != "Dschang") {
                                onFormChange(form.copy(location = placeName, error = null))
                            }
                        }
                    }
                }
            }
        }

        val cities = buildList {
            addAll(listOf("Dschang", "Bafoussam", "Douala", "Yaoundé", "Bamenda"))
            detectedPlace?.takeIf { p ->
                p.isNotBlank() && none { it.equals(p, ignoreCase = true) } && !p.equals(s.other, ignoreCase = true)
            }?.let { add(it) }
            add(s.other)
        }
        
        OutlinedCard(
            onClick = { showCityPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardShapeSmall),
            border = BorderStroke(1.dp, if (form.location.isBlank()) RedAccent.copy(alpha = 0.5f) else DividerGray)
        ) {
            Row(
                Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = if (form.location.isBlank()) RedAccent else Green, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (form.location.isBlank()) "${s.chooseYourCity} *" else "${s.cityLabel} : ${form.location}",
                        color = if (form.location.isBlank()) RedAccent else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary)
            }
        }

        if (showCityPicker) {
            AlertDialog(
                onDismissRequest = { showCityPicker = false },
                title = { Text(s.selectYourCity) },
                text = {
                    Column {
                        cities.forEach { city ->
                            TextButton(
                                onClick = { 
                                    onFormChange(form.copy(location = city, error = null))
                                    showCityPicker = false 
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(city, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth(), color = TextPrimary)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (!isLogin) {
            LightTextField(
                value = form.name,
                onValueChange = { onFormChange(form.copy(name = it, error = null)); nameError = null },
                label = s.fullName,
                isError = nameError != null,
                errorMessage = nameError
            )
            LightTextField(
                value = form.phone,
                onValueChange = { onFormChange(form.copy(phone = it, error = null)); phoneError = null },
                label = s.phone,
                keyboardType = KeyboardType.Phone,
                isError = phoneError != null,
                errorMessage = phoneError
            )
        }
        LightTextField(
            value = form.email,
            onValueChange = { onFormChange(form.copy(email = it, error = null)); emailError = null },
            label = s.email,
            keyboardType = KeyboardType.Email,
            isError = emailError != null,
            errorMessage = emailError
        )
        LightTextField(
            value = form.password,
            onValueChange = { onFormChange(form.copy(password = it, error = null)); passwordError = null },
            label = s.passwordLabel,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = onTogglePassword,
            isError = passwordError != null,
            errorMessage = passwordError
        )
        if (!isLogin) {
            LightTextField(
                value = form.confirmPassword,
                onValueChange = { onFormChange(form.copy(confirmPassword = it, error = null)); confirmError = null },
                label = s.confirmPasswordLabel,
                isPassword = true,
                showPassword = showPassword,
                onTogglePassword = onTogglePassword,
                isError = confirmError != null,
                errorMessage = confirmError,
                imeAction = ImeAction.Done,
                onImeAction = { /* Submit via the button's onClick logic */ }
            )
        }

        Spacer(Modifier.height(4.dp))

        if (!isLogin) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = form.termsAccepted,
                    onCheckedChange = { onFormChange(form.copy(termsAccepted = it, error = null)) },
                    colors = CheckboxDefaults.colors(checkedColor = Green)
                )
                Column(Modifier.clickable { onTermsClick() }) {
                    Text(
                        text = s.acceptTerms,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = s.andPrivacy,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PrimaryButton(
            text = if (isLogin) s.loginBtn else s.signupBtn,
            isLoading = form.isLoading,
            onClick = {
                if (!validate()) return@PrimaryButton
                onFormChange(form.copy(isLoading = true, error = null))
                scope.launch {
                    try {
                        if (isLogin) {
                            val response = ApiClient.login(form.email, form.password)
                            onLoginSuccess(response.token, response.user.name, response.user.role)
                        } else {
                            val response = ApiClient.register(
                                name = form.name,
                                email = form.email,
                                phone = form.phone,
                                password = form.password,
                                role = "buyer"
                            )
                            onLoginSuccess(response.token, response.user.name, response.user.role)
                        }
                    } catch (e: Throwable) {
                        onFormChange(form.copy(error = "${s.authErrorPrefix}: ${e.message}", isLoading = false))
                    }
                }
            }
        )
    }
}

@Composable
private fun PhoneStep(
    isLogin: Boolean,
    form: AuthFormState,
    onFormChange: (AuthFormState) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
    onTermsClick: () -> Unit,
    onLegalClick: () -> Unit
) {
    val s = com.tik_market.utils.LocalAppStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            s.phoneLogin,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(s.receiveCodeSms, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        LightTextField(
            value = form.phone,
            onValueChange = { onFormChange(form.copy(phone = it, error = null)) },
            label = "6XXXXXXXX",
            keyboardType = KeyboardType.Phone,
            prefix = { Text("+237 ", color = TextSecondary, fontWeight = FontWeight.Bold) }
        )

        Spacer(Modifier.height(20.dp))
        if (!isLogin) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = form.termsAccepted,
                    onCheckedChange = { onFormChange(form.copy(termsAccepted = it, error = null)) },
                    colors = CheckboxDefaults.colors(checkedColor = Green)
                )
                Column(Modifier.clickable { onTermsClick() }) {
                    Text(
                        text = s.acceptTerms,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = s.andPrivacy,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PrimaryButton(
            text = if (form.isLoading) "" else s.sendCode,
            isLoading = form.isLoading,
            onClick = {
                val cleaned = form.phone.replace(Regex("[^0-9]"), "")
                if (cleaned.length < 8) {
                    onFormChange(form.copy(error = s.invalidNumberExample))
                } else {
                    scope.launch {
                        onFormChange(form.copy(isLoading = true, error = null))
                        try {
                            val resp = ApiClient.sendOtp(cleaned)
                            onFormChange(form.copy(
                                isLoading = false,
                                isOtpStep = true,
                                otpExpiresAt = currentTimeMillis() + resp.expiresIn * 1000L,
                                canResend = false
                            ))
                        } catch (e: Throwable) {
                            onFormChange(form.copy(
                                isLoading = false,
                                error = "${s.sendingError} : ${e.message}"
                            ))
                        }
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) {
            Text(s.cancel, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OtpStep(
    isLogin: Boolean,
    form: AuthFormState,
    onFormChange: (AuthFormState) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoginSuccess: (String, String, String) -> Unit,
    onTermsClick: () -> Unit,
    onLegalClick: () -> Unit
) {
    var remainingSeconds by remember { mutableStateOf(0) }
    val cleanedPhone = form.phone.replace(Regex("[^0-9]"), "")
    val s = com.tik_market.utils.LocalAppStrings.current

    LaunchedEffect(form.otpExpiresAt, form.canResend) {
        while (form.otpExpiresAt > 0L) {
            val left = ((form.otpExpiresAt - currentTimeMillis()) / 1000).toInt()
            if (left <= 0) {
                remainingSeconds = 0
                if (!form.canResend) onFormChange(form.copy(canResend = true))
                break
            }
            remainingSeconds = left
            delay(1000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            s.authOtpTitle,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(s.sentTo.format(cleanedPhone), style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        Spacer(Modifier.height(20.dp))

        LightTextField(
            value = form.otpCode,
            onValueChange = { if (it.length <= 6) onFormChange(form.copy(otpCode = it, error = null)) },
            label = s.sixDigitCode,
            keyboardType = KeyboardType.Number
        )

        if (remainingSeconds > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                s.codeValidFor.format(remainingSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = if (remainingSeconds < 30) RedAccent else TextSecondary
            )
        }

        Spacer(Modifier.height(20.dp))
        if (!isLogin) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = form.termsAccepted,
                    onCheckedChange = { onFormChange(form.copy(termsAccepted = it, error = null)) },
                    colors = CheckboxDefaults.colors(checkedColor = Green)
                )
                Column(Modifier.clickable { onTermsClick() }) {
                    Text(
                        text = s.acceptTerms,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = s.andPrivacy,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PrimaryButton(
            text = if (form.isLoading) "" else s.verify,
            isLoading = form.isLoading,
            onClick = {
                scope.launch {
                    if (form.otpCode.length < 4) {
                        onFormChange(form.copy(error = s.codeTooShort))
                        return@launch
                    }
                    onFormChange(form.copy(isLoading = true, error = null))
                    try {
                        val resp = ApiClient.verifyOtp(cleanedPhone, form.otpCode)
                        if (resp.success) {
                            onLoginSuccess(
                                resp.token,
                                resp.user?.name ?: "Client $cleanedPhone",
                                resp.user?.role ?: "buyer"
                            )
                        } else {
                            onFormChange(form.copy(error = s.incorrectCode, isLoading = false))
                        }
                    } catch (e: Throwable) {
                        onFormChange(form.copy(error = "${s.authErrorPrefix} : ${e.message}", isLoading = false))
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        if (form.canResend) {
            TextButton(onClick = {
                scope.launch {
                    onFormChange(form.copy(isLoading = true, error = null))
                    try {
                        val resp = ApiClient.sendOtp(cleanedPhone)
                        onFormChange(form.copy(
                            isLoading = false,
                            otpCode = "",
                            otpExpiresAt = currentTimeMillis() + resp.expiresIn * 1000L,
                            canResend = false
                        ))
                    } catch (e: Throwable) {
                        onFormChange(form.copy(isLoading = false, error = e.message))
                    }
                }
            }) {
                Text(s.resendCode, color = Green, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            TextButton(onClick = { onFormChange(form.copy(isOtpStep = false)) }) {
                Text(s.changeNumber, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onTogglePassword, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, Modifier.size(18.dp),
                            tint = if (isError) RedAccent else TextSecondary
                        )
                    }
                }
            } else if (isError) {
                { Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = RedAccent) }
            } else null,
            prefix = prefix,
            singleLine = true,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onImeAction() }, onNext = { onImeAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) RedAccent else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isError) RedAccent.copy(alpha = 0.5f) else DividerGray,
                focusedLabelColor = if (isError) RedAccent else MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = if (isError) RedAccent else TextSecondary,
                cursorColor = if (isError) RedAccent else MaterialTheme.colorScheme.primary,
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite
            ),
            shape = RoundedCornerShape(CardShapeSmall),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
        )
        if (isError && errorMessage != null) {
            Text(
                errorMessage,
                color = RedAccent,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(CardShapeSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun SocialButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(CardShapeSmall),
        color = CardWhite,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
            Spacer(Modifier.width(10.dp))
            Text(text, color = textColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}
