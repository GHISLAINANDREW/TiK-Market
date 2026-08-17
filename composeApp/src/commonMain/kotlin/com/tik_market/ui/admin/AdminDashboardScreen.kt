package com.tik_market.ui.admin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.ApiAdminDashboardResponse
import com.tik_market.api.ApiClient
import com.tik_market.api.ApiOnlineUser
import com.tik_market.api.ApiOnlineUsersResponse
import com.tik_market.api.ApiStory
import com.tik_market.api.ApiPromoCreateBody
import com.tik_market.api.ApiNotification
import com.tik_market.data.models.SampleData
import com.tik_market.data.models.OrderStatus
import com.tik_market.theme.*
import com.tik_market.ui.components.*
import com.tik_market.utils.FormatUtils
import com.tik_market.utils.LocalAppStrings
import kotlinx.coroutines.launch

// ── Admin menu items ──
private data class AdminMenuItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: @Composable () -> Unit,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ts = LocalAppStrings.current
    var selectedOption by remember { mutableStateOf<Int?>(null) }

    // Shared data
    var users by remember { mutableStateOf<List<AdminUser>>(emptyList()) }
    var shops by remember { mutableStateOf<List<AdminShop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Promotion dialog state
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoShop by remember { mutableStateOf<AdminShop?>(null) }
    var promoCode by remember { mutableStateOf("") }
    var promoDiscountPct by remember { mutableStateOf("") }
    var promoDiscountFixed by remember { mutableStateOf("") }
    var promoMinAmount by remember { mutableStateOf("") }
    var promoSending by remember { mutableStateOf(false) }
    var promoMessage by remember { mutableStateOf<String?>(null) }

    // Add user dialog state
    var showAddUserDialog by remember { mutableStateOf(false) }
    var addUserName by remember { mutableStateOf("") }
    var addUserEmail by remember { mutableStateOf("") }
    var addUserPhone by remember { mutableStateOf("") }
    var addUserPassword by remember { mutableStateOf("") }
    var addUserRole by remember { mutableStateOf("buyer") }
    var addUserCity by remember { mutableStateOf("") }
    var addUserSending by remember { mutableStateOf(false) }
    var addUserResult by remember { mutableStateOf<String?>(null) }
    var addUserError by remember { mutableStateOf<String?>(null) }

    // User notification dialog state
    var showUserNotifDialog by remember { mutableStateOf(false) }
    var targetNotifUser by remember { mutableStateOf<AdminUser?>(null) }
    var userNotifTitle by remember { mutableStateOf("") }
    var userNotifMessage by remember { mutableStateOf("") }
    var userNotifSending by remember { mutableStateOf(false) }
    var userNotifResult by remember { mutableStateOf<String?>(null) }

    val menuItems = remember {
        listOf(
            AdminMenuItem(0, ts.usersLabel, ts.manageAccounts, { Icon(Icons.Default.People, null, tint = Color.White) }, BlueAccent),
            AdminMenuItem(1, ts.shop, ts.verifyManage, { Icon(Icons.Default.Store, null, tint = Color.White) }, Orange),
            AdminMenuItem(2, ts.notifications, ts.broadcastMessages, { Icon(Icons.Default.Notifications, null, tint = Color.White) }, Violet),
            AdminMenuItem(3, ts.dashboardTitle, ts.statsKPIs, { Icon(Icons.Default.Dashboard, null, tint = Color.White) }, Green),
            AdminMenuItem(4, ts.onlineLabel, ts.activeUsers, { Icon(Icons.Default.PersonPin, null, tint = Color.White) }, Color(0xFF00897B)),
            AdminMenuItem(5, ts.storiesLabel, ts.ephemeralContent, { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) }, Color(0xFFE91E63)),
            AdminMenuItem(6, ts.promoHeroLabel, ts.homeBanners, { Icon(Icons.Default.Star, null, tint = Color.White) }, Color(0xFFFF6F00)),
            AdminMenuItem(7, ts.superAdminLabel, ts.totalControl, { Icon(Icons.Default.Security, null, tint = Color.White) }, Color(0xFFD32F2F)),
        )
    }

    val filteredMenuItems = remember(ApiClient.getCurrentUser()) {
        if (ApiClient.isSuperAdmin()) menuItems
        else menuItems.filter { it.id != 7 }
    }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val rawUsers = try {
                    ApiClient.fetchAdminUsers()
                } catch (e: Exception) {
                    println("AdminUsers error: ${e.message}")
                    null
                }
                val rawShops = try {
                    ApiClient.fetchAdminShops()
                } catch (e: Exception) {
                    println("AdminShops error: ${e.message}")
                    null
                }
                if (rawUsers != null) {
                    users = rawUsers.map { AdminUser(it.id, it.name, it.email, it.phone, it.role, it.status, it.createdAt, it.managedCity) }
                }
                if (rawShops != null && rawShops.isNotEmpty()) {
                    shops = rawShops.map {
                        AdminShop(
                            id = it.id, name = it.name, logo = it.logo, location = it.location,
                            phone = it.phone, vendorName = it.vendorName, vendorEmail = it.vendorEmail,
                            vendorPhone = it.vendorPhone, category = it.category, status = it.status,
                            isFeatured = it.isFeatured, productCount = it.productCount, totalSales = it.totalSales,
                            isVerified = it.isVerified, createdAt = it.createdAt, updatedAt = it.updatedAt
                        )
                    }
                }
                if (rawUsers == null && rawShops == null) {
                    errorMessage = ts.adminConnError
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // ── If a sub-screen is selected, show it full page ──
    if (selectedOption != null) {
        if (selectedOption == 7) {
            SuperAdminScreen(onBack = { selectedOption = null })
            return
        }

        AdminSubScreen(
            optionId = selectedOption!!,
            title = menuItems.firstOrNull { it.id == selectedOption }?.title ?: "",
            onBack = { selectedOption = null },
            ts = ts,
            scope = scope, users = users, shops = shops, loadData = { loadData() },
            // Promo dialog
            showPromoDialog = showPromoDialog, setShowPromoDialog = { showPromoDialog = it },
            promoShop = promoShop, setPromoShop = { promoShop = it },
            promoCode = promoCode, setPromoCode = { promoCode = it },
            promoDiscountPct = promoDiscountPct, setPromoDiscountPct = { promoDiscountPct = it },
            promoDiscountFixed = promoDiscountFixed, setPromoDiscountFixed = { promoDiscountFixed = it },
            promoMinAmount = promoMinAmount, setPromoMinAmount = { promoMinAmount = it },
            promoSending = promoSending, setPromoSending = { promoSending = it },
            promoMessage = promoMessage, setPromoMessage = { promoMessage = it },
            // Add user dialog
            showAddUserDialog = showAddUserDialog, setShowAddUserDialog = { showAddUserDialog = it },
            addUserName = addUserName, setAddUserName = { addUserName = it },
            addUserEmail = addUserEmail, setAddUserEmail = { addUserEmail = it },
            addUserPhone = addUserPhone, setAddUserPhone = { addUserPhone = it },
            addUserPassword = addUserPassword, setAddUserPassword = { addUserPassword = it },
            addUserRole = addUserRole, setAddUserRole = { addUserRole = it },
            addUserCity = addUserCity, setAddUserCity = { addUserCity = it },
            addUserSending = addUserSending, setAddUserSending = { addUserSending = it },
            addUserResult = addUserResult, setAddUserResult = { addUserResult = it },
            addUserError = addUserError, setAddUserError = { addUserError = it },
            // User notif dialog
            showUserNotifDialog = showUserNotifDialog, setShowUserNotifDialog = { showUserNotifDialog = it },
            targetNotifUser = targetNotifUser, setTargetNotifUser = { targetNotifUser = it },
            userNotifTitle = userNotifTitle, setUserNotifTitle = { userNotifTitle = it },
            userNotifMessage = userNotifMessage, setUserNotifMessage = { userNotifMessage = it },
            userNotifSending = userNotifSending, setUserNotifSending = { userNotifSending = it },
            userNotifResult = userNotifResult, setUserNotifResult = { userNotifResult = it }
        )
        return
    }

    // ── Main grid menu ──
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.admin, style = MaterialTheme.typography.titleLarge, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    IconButton(onClick = { loadData() }) { Icon(Icons.Default.Refresh, null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (errorMessage != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp)
                    Text(errorMessage!!, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Button(onClick = { loadData() }) { Text(ts.retry) }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMenuItems.size) { index ->
                        val item = filteredMenuItems[index]
                        AdminMenuTile(item = item, onClick = { selectedOption = item.id })
                    }
                }
            }
        }
    }
}

// ── Admin Menu Tile ──
@Composable
private fun AdminMenuTile(item: AdminMenuItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            Column {
                Box(
                    Modifier.size(48.dp).background(item.color, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { item.icon() }
                Spacer(Modifier.weight(1f))
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  SUB-SCREEN (full page per option)
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminSubScreen(
    optionId: Int, title: String, onBack: () -> Unit,
    ts: com.tik_market.utils.AppStrings,
    scope: kotlinx.coroutines.CoroutineScope,
    users: List<AdminUser>, shops: List<AdminShop>, loadData: () -> Unit,
    // Promo dialog
    showPromoDialog: Boolean, setShowPromoDialog: (Boolean) -> Unit,
    promoShop: AdminShop?, setPromoShop: (AdminShop?) -> Unit,
    promoCode: String, setPromoCode: (String) -> Unit,
    promoDiscountPct: String, setPromoDiscountPct: (String) -> Unit,
    promoDiscountFixed: String, setPromoDiscountFixed: (String) -> Unit,
    promoMinAmount: String, setPromoMinAmount: (String) -> Unit,
    promoSending: Boolean, setPromoSending: (Boolean) -> Unit,
    promoMessage: String?, setPromoMessage: (String?) -> Unit,
    // Add user
    showAddUserDialog: Boolean, setShowAddUserDialog: (Boolean) -> Unit,
    addUserName: String, setAddUserName: (String) -> Unit,
    addUserEmail: String, setAddUserEmail: (String) -> Unit,
    addUserPhone: String, setAddUserPhone: (String) -> Unit,
    addUserPassword: String, setAddUserPassword: (String) -> Unit,
    addUserRole: String, setAddUserRole: (String) -> Unit,
    addUserCity: String, setAddUserCity: (String) -> Unit,
    addUserSending: Boolean, setAddUserSending: (Boolean) -> Unit,
    addUserResult: String?, setAddUserResult: (String?) -> Unit,
    addUserError: String?, setAddUserError: (String?) -> Unit,
    // User notif
    showUserNotifDialog: Boolean, setShowUserNotifDialog: (Boolean) -> Unit,
    targetNotifUser: AdminUser?, setTargetNotifUser: (AdminUser?) -> Unit,
    userNotifTitle: String, setUserNotifTitle: (String) -> Unit,
    userNotifMessage: String, setUserNotifMessage: (String) -> Unit,
    userNotifSending: Boolean, setUserNotifSending: (Boolean) -> Unit,
    userNotifResult: String?, setUserNotifResult: (String?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                actions = {
                    if (optionId == 0) {
                        IconButton(onClick = {
                            setAddUserName(""); setAddUserEmail(""); setAddUserPhone(""); setAddUserPassword("")
                            setAddUserRole("buyer"); setAddUserResult(null); setAddUserError(null)
                            setShowAddUserDialog(true)
                        }) {
                            Icon(Icons.Default.PersonAdd, ts.addUserTitle, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            when (optionId) {
                0 -> AdminUsersList(
                    users = users, isLoading = false, errorMessage = null,
                    scope = scope, loadData = loadData, ts = ts,
                    setAddUserError = setAddUserError,
                    setTargetNotifUser = setTargetNotifUser,
                    setUserNotifTitle = setUserNotifTitle,
                    setUserNotifMessage = setUserNotifMessage,
                    setUserNotifResult = setUserNotifResult,
                    setShowUserNotifDialog = setShowUserNotifDialog
                )
                1 -> AdminShopsList(
                    shops = shops, scope = scope, ts = ts,
                    setPromoShop = setPromoShop,
                    setPromoCode = setPromoCode,
                    setPromoDiscountPct = setPromoDiscountPct,
                    setPromoDiscountFixed = setPromoDiscountFixed,
                    setPromoMinAmount = setPromoMinAmount,
                    setPromoMessage = setPromoMessage,
                    setShowPromoDialog = setShowPromoDialog
                )
                2 -> AdminNotificationsContent(scope = scope, users = users, ts = ts)
                3 -> AdminDashboardContent(scope = scope, ts = ts)
                4 -> AdminOnlineUsersContent(scope = scope, ts = ts)
                5 -> AdminStoriesContent(scope = scope, shops = shops, ts = ts)
                6 -> AdminHeroContent(scope = scope, shops = shops, ts = ts)
            }

            // ─── Promotion Dialog ───
            if (showPromoDialog && promoShop != null) {
                AlertDialog(
                    onDismissRequest = { if (!promoSending) setShowPromoDialog(false) },
                    title = { Text(ts.promoAtShop.replace("%s", promoShop!!.name)) },
                    text = {
                        Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) {
                            if (promoMessage != null) {
                                Text(promoMessage!!, color = if (promoMessage!!.startsWith("✅")) Green else RedAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                            }
                            OutlinedTextField(value = promoCode, onValueChange = { setPromoCode(it.uppercase().take(20)) }, label = { Text(ts.promoCode) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = promoDiscountPct, onValueChange = { setPromoDiscountPct(it.filter { c -> c.isDigit() || c == '.' }.take(5)) }, label = { Text(ts.promoReductPct) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(4.dp))
                            Text(ts.or, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = promoDiscountFixed, onValueChange = { setPromoDiscountFixed(it.filter { c -> c.isDigit() }.take(7)) }, label = { Text(ts.promoFixedFcfa) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = promoMinAmount, onValueChange = { setPromoMinAmount(it.filter { c -> c.isDigit() }.take(7)) }, label = { Text(ts.promoMinAmountFcfa) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    setPromoSending(true); setPromoMessage(null)
                                    try {
                                        val pct = promoDiscountPct.toDoubleOrNull() ?: 0.0
                                        val fixed = promoDiscountFixed.toIntOrNull() ?: 0
                                        if (promoCode.isBlank() || (pct <= 0 && fixed <= 0)) {
                                            setPromoMessage(ts.promoCodeRequired)
                                        } else {
                                            ApiClient.createPromotion(ApiPromoCreateBody(shopId = promoShop!!.id, code = promoCode, discountPct = pct, discountFixed = fixed, minAmount = promoMinAmount.toIntOrNull() ?: 0, maxUses = 100))
                                            ApiClient.sendSystemNotification(ts.promoAtShop.replace("%s", promoShop!!.name), Regex("%s").replaceFirst(Regex("%s").replaceFirst(Regex("%s").replaceFirst(ts.promoNotifBody, promoCode), if (pct > 0) "$pct% de réduction" else "$fixed FCFA de réduction"), promoShop!!.name))
                                            setPromoMessage(ts.promoCreatedNotified)
                                        }
                                    } catch (e: Exception) { setPromoMessage(ts.adminErrPrefix.replace("%s", e.message ?: "")) }
                                    setPromoSending(false)
                                }
                            },
                            enabled = !promoSending && promoCode.isNotBlank()
                        ) {
                            if (promoSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            else Text(ts.createNotify)
                        }
                    },
                    dismissButton = { TextButton(onClick = { setShowPromoDialog(false) }, enabled = !promoSending) { Text(ts.close) } }
                )
            }

            // ─── Add User Dialog ───
            if (showAddUserDialog) {
                AlertDialog(
                    onDismissRequest = { if (!addUserSending) setShowAddUserDialog(false) },
                    title = { Text(ts.addUserTitle) },
                    text = {
                        Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                            if (addUserResult != null) {
                                Text(addUserResult!!, color = if (addUserResult!!.startsWith("✅")) Green else RedAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                            }
                            if (addUserError != null) {
                                Text(addUserError!!, color = RedAccent, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                            }
                            OutlinedTextField(value = addUserName, onValueChange = { setAddUserName(it) }, label = { Text(ts.fullName) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = addUserEmail, onValueChange = { setAddUserEmail(it) }, label = { Text(ts.email) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = addUserPhone, onValueChange = { setAddUserPhone(it) }, label = { Text(ts.phone) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = addUserPassword, onValueChange = { setAddUserPassword(it) }, label = { Text(ts.passwordLabel) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            Text(ts.role, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                FilterChip(selected = addUserRole == "buyer", onClick = { setAddUserRole("buyer") }, label = { Text(ts.clientLabel) })
                                FilterChip(selected = addUserRole == "vendor", onClick = { setAddUserRole("vendor") }, label = { Text(ts.vendor) })
                                FilterChip(selected = addUserRole == "admin", onClick = { setAddUserRole("admin") }, label = { Text(ts.adminLabel) })
                                if (ApiClient.isSuperAdmin()) {
                                    FilterChip(selected = addUserRole == "super_admin", onClick = { setAddUserRole("super_admin") }, label = { Text(ts.superLabel) })
                                }
                            }
                            if (addUserRole == "admin") {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = addUserCity, onValueChange = { setAddUserCity(it) }, label = { Text(ts.managedCityOptional) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                                Text(ts.globalAdminHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    setAddUserSending(true); setAddUserResult(null); setAddUserError(null)
                                    try {
                                        if (addUserName.isBlank() || addUserEmail.isBlank() || addUserPhone.isBlank() || addUserPassword.isBlank()) {
                                            setAddUserResult(ts.allFieldsRequired)
                                        } else {
                                            ApiClient.addUser(addUserName, addUserEmail, addUserPhone, addUserPassword, addUserRole, addUserCity.ifBlank { null })
                                            setAddUserResult(ts.userCreatedSuccess.replace("%s", addUserName))
                                            setAddUserName(""); setAddUserEmail(""); setAddUserPhone(""); setAddUserPassword(""); setAddUserCity("")
                                            loadData()
                                        }
                                    } catch (e: Exception) { setAddUserResult(ts.adminErrPrefix.replace("%s", e.message ?: "")) }
                                    setAddUserSending(false)
                                }
                            },
                            enabled = !addUserSending
                        ) {
                            if (addUserSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            else Text(ts.create)
                        }
                    },
                    dismissButton = { TextButton(onClick = { setShowAddUserDialog(false) }, enabled = !addUserSending) { Text(ts.close) } }
                )
            }

            // ─── Individual User Notification Dialog ───
            if (showUserNotifDialog && targetNotifUser != null) {
                AlertDialog(
                    onDismissRequest = { if (!userNotifSending) setShowUserNotifDialog(false) },
                    title = { Text(ts.notifyUser.replace("%s", targetNotifUser!!.name)) },
                    text = {
                        Column(Modifier.width(300.dp)) {
                            if (userNotifResult != null) {
                                Text(userNotifResult!!, color = if (userNotifResult!!.startsWith("✅")) Green else RedAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                            }
                            OutlinedTextField(value = userNotifTitle, onValueChange = { setUserNotifTitle(it) }, label = { Text(ts.notifTitle) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = userNotifMessage, onValueChange = { setUserNotifMessage(it) }, label = { Text(ts.messageLabel) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(CardShapeSmall))
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    setUserNotifSending(true); setUserNotifResult(null)
                                    try {
                                        val ok = ApiClient.sendIndividualNotification(targetNotifUser!!.id, userNotifTitle, userNotifMessage)
                                        if (ok) {
                                            setUserNotifResult(ts.notifSent)
                                            setUserNotifTitle(""); setUserNotifMessage("")
                                        } else { setUserNotifResult(ts.sendFailed) }
                                    } catch (e: Exception) { setUserNotifResult(ts.sendErrPrefix.replace("%s", e.message ?: "")) }
                                    setUserNotifSending(false)
                                }
                            },
                            enabled = !userNotifSending && userNotifTitle.isNotBlank() && userNotifMessage.isNotBlank()
                        ) {
                            if (userNotifSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            else Text(ts.send)
                        }
                    },
                    dismissButton = { TextButton(onClick = { setShowUserNotifDialog(false) }, enabled = !userNotifSending) { Text(ts.close) } }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  USERS LIST
// ═══════════════════════════════════════════════

@Composable
private fun AdminUsersList(
    users: List<AdminUser>,
    isLoading: Boolean, errorMessage: String?,
    scope: kotlinx.coroutines.CoroutineScope, loadData: () -> Unit,
    ts: com.tik_market.utils.AppStrings,
    setAddUserError: (String?) -> Unit,
    setTargetNotifUser: (AdminUser?) -> Unit,
    setUserNotifTitle: (String) -> Unit,
    setUserNotifMessage: (String) -> Unit,
    setUserNotifResult: (String?) -> Unit,
    setShowUserNotifDialog: (Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(users.size) { index ->
            val user = users[index]
            AdminUserCard(user, ts,
                onRoleChange = { role ->
                    scope.launch {
                        try { ApiClient.updateUserRole(user.id, role) } catch (e: Exception) { setAddUserError(ts.roleChangeError.replace("%s", e.message ?: "")) }
                    }
                },
                onDelete = {
                    scope.launch {
                        try { ApiClient.deleteUser(user.id) } catch (e: Exception) { setAddUserError(ts.deleteErrorPrefix.replace("%s", e.message ?: "")) }
                    }
                },
                onBan = { newStatus ->
                    scope.launch {
                        try { ApiClient.banUser(user.id, newStatus) } catch (e: Exception) { setAddUserError(ts.sendErrPrefix.replace("%s", e.message ?: "")) }
                    }
                },
                onNotify = {
                    setTargetNotifUser(user)
                    setUserNotifTitle(""); setUserNotifMessage(""); setUserNotifResult(null)
                    setShowUserNotifDialog(true)
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  SHOPS LIST
// ═══════════════════════════════════════════════

@Composable
private fun AdminShopsList(
    shops: List<AdminShop>,
    scope: kotlinx.coroutines.CoroutineScope,
    ts: com.tik_market.utils.AppStrings,
    setPromoShop: (AdminShop?) -> Unit,
    setPromoCode: (String) -> Unit,
    setPromoDiscountPct: (String) -> Unit,
    setPromoDiscountFixed: (String) -> Unit,
    setPromoMinAmount: (String) -> Unit,
    setPromoMessage: (String?) -> Unit,
    setShowPromoDialog: (Boolean) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (shops.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text(ts.noShopsFound, color = TextSecondary) } }
        }
        items(shops.size) { index ->
            val shop = shops[index]
            AdminShopCard(
                shop = shop, ts = ts,
                onToggleVerify = {
                    scope.launch { try { ApiClient.toggleShopVerification(shop.id, !shop.isVerified) } catch (_: Exception) {} }
                },
                onPromote = { featured ->
                    scope.launch { try { ApiClient.promoteShop(shop.id, featured) } catch (_: Exception) {} }
                },
                onBan = { newStatus ->
                    scope.launch { try { ApiClient.banShop(shop.id, newStatus) } catch (_: Exception) {} }
                },
                onDelete = {
                    scope.launch { try { ApiClient.deleteShop(shop.id) } catch (_: Exception) {} }
                },
                onAddPromo = {
                    setPromoShop(shop)
                    setPromoCode("")
                    setPromoDiscountPct("")
                    setPromoDiscountFixed("")
                    setPromoMinAmount("")
                    setPromoMessage(null)
                    setShowPromoDialog(true)
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════
//  NOTIFICATIONS CONTENT
// ═══════════════════════════════════════════════

@Composable
private fun AdminNotificationsContent(scope: kotlinx.coroutines.CoroutineScope, users: List<AdminUser>, ts: com.tik_market.utils.AppStrings) {
    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var notifResult by remember { mutableStateOf<String?>(null) }

    var indivUserId by remember { mutableStateOf<Int?>(null) }
    var indivUserName by remember { mutableStateOf("") }
    var indivTitle by remember { mutableStateOf("") }
    var indivMessage by remember { mutableStateOf("") }
    var indivSending by remember { mutableStateOf(false) }
    var indivResult by remember { mutableStateOf<String?>(null) }
    var showUserSearch by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }

    var history by remember { mutableStateOf<List<ApiNotification>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(false) }

    fun loadHistory() {
        scope.launch {
            isHistoryLoading = true
            history = try { ApiClient.fetchAdminNotifications() } catch (e: Exception) { emptyList() }
            isHistoryLoading = false
        }
    }

    LaunchedEffect(Unit) { loadHistory() }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        // ── Historique ──
        Text(ts.sendHistory, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardShapeMedium),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (isHistoryLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                } else if (history.isEmpty()) {
                    Text(ts.noHistory, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    history.take(10).forEach { h ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (h.userId == null) Icons.Default.Public else Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = if (h.userId == null) BlueAccent else Green)
                                Spacer(Modifier.width(8.dp))
                                Text(h.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { scope.launch { try { ApiClient.deleteNotification(h.id); loadHistory() } catch (_: Exception) {} } }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = RedAccent, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(h.createdAt.take(16).replace("T", " "), fontSize = 10.sp, color = TextTertiary)
                            }
                            Text(h.message, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = DividerGray.copy(alpha = 0.5f))
                        }
                    }
                    if (history.size > 10) {
                        Text(ts.andOthers.replace("%d", "${history.size - 10}"), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Notification broadcast ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardShapeMedium),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(ts.systemNotif, style = MaterialTheme.typography.titleLarge)
                Text(ts.receivedByAll, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = notifTitle, onValueChange = { notifTitle = it; notifResult = null }, label = { Text(ts.notifTitle) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall), singleLine = true)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = notifMessage, onValueChange = { notifMessage = it; notifResult = null }, label = { Text(ts.messageLabel) }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(CardShapeSmall))
                Spacer(Modifier.height(20.dp))
                if (notifResult != null) {
                    Text(notifResult!!, color = if (notifResult!!.startsWith("✅")) Green else RedAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        scope.launch {
                            sending = true; notifResult = null
                            try { ApiClient.sendSystemNotification(notifTitle, notifMessage); notifTitle = ""; notifMessage = ""; notifResult = ts.notifBroadcastAll; loadHistory() } catch (e: Exception) { notifResult = ts.sendErrPrefix.replace("%s", e.message ?: "") }
                            sending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    enabled = !sending && notifTitle.isNotBlank() && notifMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(CardShapeSmall)
                ) {
                    if (sending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(ts.broadcastAll, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Notification individuelle ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CardShapeMedium),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(ts.individualNotif, style = MaterialTheme.typography.titleLarge)
                Text(ts.sendToSpecific, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))

                if (indivUserId == null) {
                    OutlinedTextField(value = userSearchQuery, onValueChange = { userSearchQuery = it; showUserSearch = it.length >= 2 }, label = { Text(ts.searchUserMin) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall), singleLine = true, trailingIcon = { Icon(Icons.Default.Search, null) })
                    if (showUserSearch) {
                        Spacer(Modifier.height(4.dp))
                        val filtered = users.filter { it.name.contains(userSearchQuery, ignoreCase = true) || it.email.contains(userSearchQuery, ignoreCase = true) }
                        if (filtered.isEmpty()) {
                            Text(ts.noUserFound, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(8.dp))
                        } else {
                            Surface(shape = RoundedCornerShape(CardShapeSmall), color = Color(0xFFF5F5F5), modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    filtered.take(10).forEach { u ->
                                        Row(
                                            Modifier.fillMaxWidth().clickable { indivUserId = u.id; indivUserName = u.name; userSearchQuery = ""; showUserSearch = false }.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                                Text(u.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(u.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                Text(u.email, fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Text(u.role, fontSize = 11.sp, color = Green, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardShapeSmall)).background(Color(0xFFF0F0F0)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Text(indivUserName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Spacer(Modifier.width(12.dp))
                        Text(indivUserName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { indivUserId = null; indivUserName = ""; indivTitle = ""; indivMessage = ""; indivResult = null }) { Text(ts.change, color = RedAccent) }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = indivTitle, onValueChange = { indivTitle = it; indivResult = null }, label = { Text(ts.notifTitle) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(CardShapeSmall), singleLine = true, enabled = indivUserId != null)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = indivMessage, onValueChange = { indivMessage = it; indivResult = null }, label = { Text(ts.messageLabel) }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(CardShapeSmall), enabled = indivUserId != null)
                Spacer(Modifier.height(16.dp))

                if (indivResult != null) {
                    Text(indivResult!!, color = if (indivResult!!.startsWith("✅")) Green else RedAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        scope.launch {
                            indivSending = true; indivResult = null
                            try {
                                val ok = ApiClient.sendIndividualNotification(indivUserId!!, indivTitle, indivMessage)
                                if (ok) { indivResult = ts.sendTo.replace("%s", indivUserName); indivTitle = ""; indivMessage = ""; loadHistory() } else { indivResult = ts.sendFailed }
                            } catch (e: Exception) { indivResult = ts.sendErrPrefix.replace("%s", e.message ?: "") }
                            indivSending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    enabled = !indivSending && indivUserId != null && indivTitle.isNotBlank() && indivMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    shape = RoundedCornerShape(CardShapeSmall)
                ) {
                    if (indivSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text(ts.sendTo.replace("%s", indivUserName), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  DATA CLASSES
// ═══════════════════════════════════════════════

data class AdminUser(val id: Int, val name: String, val email: String, val phone: String, val role: String, val status: String = "active", val createdAt: String, val managedCity: String? = null)
data class AdminShop(
    val id: Int, val name: String, val logo: String = "", val location: String = "",
    val phone: String = "", val vendorName: String, val vendorEmail: String = "",
    val vendorPhone: String = "", val category: String, val status: String = "active",
    val isFeatured: Boolean = false, val productCount: Int = 0, val totalSales: Int = 0,
    val isVerified: Boolean = false, val createdAt: String = "", val updatedAt: String = ""
)

// ═══════════════════════════════════════════════
//  USER CARD
// ═══════════════════════════════════════════════

@Composable
fun AdminUserCard(user: AdminUser, ts: com.tik_market.utils.AppStrings, onRoleChange: (String) -> Unit, onDelete: () -> Unit, onBan: (String) -> Unit, onNotify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardShapeMedium),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleSmall)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleBadge(user.role, ts)
                    if (user.managedCity != null) {
                        Surface(color = BlueAccent.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text(user.managedCity, style = MaterialTheme.typography.labelSmall, color = BlueAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (user.status == "banned") {
                        Surface(color = RedAccent.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                            Text(ts.bannedLabel, style = MaterialTheme.typography.labelSmall, color = RedAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = TextSecondary) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(ts.sendNotifMenuItem) }, leadingIcon = { Icon(Icons.Default.Notifications, null, tint = Green) }, onClick = { onNotify(); showMenu = false })
                    if (ApiClient.isSuperAdmin()) {
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(ts.roleSuperAdmin) }, onClick = { onRoleChange("super_admin"); showMenu = false })
                        DropdownMenuItem(text = { Text(ts.roleAdmin) }, onClick = { onRoleChange("admin"); showMenu = false })
                    }
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(ts.roleVendor) }, onClick = { onRoleChange("vendor"); showMenu = false })
                    DropdownMenuItem(text = { Text(ts.roleClient) }, onClick = { onRoleChange("buyer"); showMenu = false })
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (user.status == "banned") ts.reactivate else ts.ban, color = if (user.status == "banned") Green else RedAccent) },
                        leadingIcon = { Icon(if (user.status == "banned") Icons.Default.CheckCircle else Icons.Default.Block, null, tint = if (user.status == "banned") Green else RedAccent) },
                        onClick = { onBan(if (user.status == "banned") "active" else "banned"); showMenu = false }
                    )
                    DropdownMenuItem(text = { Text(ts.delete, color = RedAccent) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedAccent) }, onClick = { onDelete(); showMenu = false })
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  SHOP CARD
// ═══════════════════════════════════════════════

@Composable
fun AdminShopCard(
    shop: AdminShop,
    ts: com.tik_market.utils.AppStrings,
    onToggleVerify: () -> Unit,
    onPromote: (Boolean) -> Unit,
    onBan: (String) -> Unit,
    onDelete: () -> Unit,
    onAddPromo: () -> Unit
) {
    var logoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(shop.logo) {
        if (shop.logo.isNotBlank()) { logoBitmap = loadImageFromUrl(shop.logo) }
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardShapeMedium),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (logoBitmap != null) { Image(bitmap = logoBitmap!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape)) }
                else { Icon(Icons.Default.Store, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(shop.name, style = MaterialTheme.typography.titleSmall)
                    if (shop.isFeatured) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Star, null, tint = Orange, modifier = Modifier.size(14.dp)) }
                    if (shop.status == "banned") { Spacer(Modifier.width(4.dp)); Surface(color = RedAccent.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) { Text(ts.bannedLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = RedAccent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) } }
                }
                Text(shop.vendorName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ts.productsCount.replace("%d", "${shop.productCount}"), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = DividerGray)
                    Text(ts.salesCount.replace("%d", "${shop.totalSales}"), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    if (shop.location.isNotBlank()) { Text("•", style = MaterialTheme.typography.labelSmall, color = DividerGray); Text(shop.location, style = MaterialTheme.typography.labelSmall, color = TextTertiary) }
                }
            }
            var showActions by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showActions = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp)) }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text(if (shop.isVerified) ts.unverify else ts.verifyAction) }, leadingIcon = { Icon(if (shop.isVerified) Icons.Default.CheckCircle else Icons.Default.Verified, null, tint = if (shop.isVerified) Green else TextSecondary) }, onClick = { onToggleVerify(); showActions = false })
                    DropdownMenuItem(text = { Text(if (shop.isFeatured) ts.removePromo else ts.featureShop, color = Orange) }, leadingIcon = { Icon(Icons.Default.Star, null, tint = Orange) }, onClick = { onPromote(!shop.isFeatured); showActions = false })
                    DropdownMenuItem(text = { Text(ts.createPromo, color = Green) }, leadingIcon = { Icon(Icons.Default.AddCircle, null, tint = Green) }, onClick = { onAddPromo(); showActions = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text(if (shop.status == "banned") ts.reactivate else ts.ban, color = if (shop.status == "banned") Green else RedAccent) }, leadingIcon = { Icon(if (shop.status == "banned") Icons.Default.CheckCircle else Icons.Default.Block, null, tint = if (shop.status == "banned") Green else RedAccent) }, onClick = { onBan(if (shop.status == "banned") "active" else "banned"); showActions = false })
                    DropdownMenuItem(text = { Text(ts.delete, color = RedAccent) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedAccent) }, onClick = { showDeleteConfirm = true; showActions = false })
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(ts.deleteShopTitle) },
            text = { Text(ts.deleteShopConfirm.replace("%s", shop.name)) },
            confirmButton = { Button(onClick = { showDeleteConfirm = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent)) { Text(ts.delete, color = Color.White) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(ts.cancel) } }
        )
    }
}

@Composable
fun RoleBadge(role: String, ts: com.tik_market.utils.AppStrings) {
    val color = when(role) { 
        "super_admin" -> Color(0xFFD32F2F)
        "admin" -> BlueAccent
        "vendor" -> Orange
        else -> Green 
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
        Text(
            when(role) { 
                "super_admin" -> ts.superAdminLabel
                "admin" -> ts.adminLabel
                "vendor" -> ts.vendor
                else -> ts.clientLabel
            },
            style = MaterialTheme.typography.labelSmall, 
            color = color, 
            fontWeight = FontWeight.SemiBold, 
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ═══════════════════════════════════════════════
//  ADMIN DASHBOARD ANALYTICS
// ═══════════════════════════════════════════════

@Composable
fun AdminDashboardContent(scope: kotlinx.coroutines.CoroutineScope, ts: com.tik_market.utils.AppStrings) {
    var data by remember { mutableStateOf<ApiAdminDashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedCity by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch { isLoading = true; error = null; try { data = ApiClient.fetchAdminDashboard(selectedCity) } catch (e: Exception) { error = e.message }; isLoading = false }
    }
    LaunchedEffect(selectedCity) { load() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        if (ApiClient.isSuperAdmin()) {
            Text(ts.filterByCity, style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = selectedCity == null, onClick = { selectedCity = null }, label = { Text(ts.allFilter) })
                listOf("Dschang", "Bafoussam", "Douala", "Yaoundé", "Bamenda").forEach { city ->
                    FilterChip(selected = selectedCity == city, onClick = { selectedCity = city }, label = { Text(city) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (isLoading && data == null) { repeat(6) { TiKShimmer(modifier = Modifier.fillMaxWidth().height(80.dp).padding(bottom = 8.dp)) }; return@Column }
        if (error != null && data == null) { TiKErrorState(message = error!!, onRetry = { load() }); return@Column }
        val d = data ?: return@Column
        val kpis = d.kpis

        Text(ts.overview, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = ts.clientsLabel, value = "${kpis.totalUsers}", color = BlueAccent, modifier = Modifier.weight(1f))
            KpiCard(title = ts.vendorsLabel, value = "${kpis.totalVendors}", color = Green, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = ts.onlineLabel, value = "${kpis.onlineUsers}", color = Orange, modifier = Modifier.weight(1f))
            KpiCard(title = ts.shop, value = "${kpis.totalShops}", color = Violet, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = ts.products, value = "${kpis.totalProducts}", color = Orange, modifier = Modifier.weight(1f))
            KpiCard(title = ts.ordersLabel, value = "${kpis.totalOrders}", color = BlueAccent, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard(title = ts.totalRevenueLabel, value = FormatUtils.formatPrice(kpis.totalRevenue), color = GreenDark, modifier = Modifier.weight(1f))
            KpiCard(title = ts.todayLabel, value = FormatUtils.formatPrice(kpis.revenueToday), color = Orange, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))

        if (kpis.pendingShops > 0 || kpis.bannedShops > 0) {
            Text(ts.alerts, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
            if (kpis.pendingShops > 0) {
                TiKCard(elevation = TiKCardElevation.Low) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Orange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text(ts.pendingVerifyShops.replace("%d", "$kpis.pendingShops"), style = MaterialTheme.typography.bodyMedium); Text(ts.goToShopsVerify, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                    }
                }
            }
            if (kpis.bannedShops > 0) {
                Spacer(Modifier.height(6.dp))
                TiKCard(elevation = TiKCardElevation.Low) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Block, null, tint = RedAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text(ts.bannedShopsLabel.replace("%d", "$kpis.bannedShops"), style = MaterialTheme.typography.bodyMedium); Text(ts.checkShopsDetails, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(ts.registrations30, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        Text(ts.newUsersMonth.replace("%d", "$kpis.newUsers30d"), style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        TiKCard(elevation = TiKCardElevation.Low) { SimpleBarChart(data = d.registrations.map { it.count }, modifier = Modifier.fillMaxWidth().height(80.dp)) }
        Spacer(Modifier.height(16.dp))

        Text(ts.monthlyRevenue12, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        TiKCard(elevation = TiKCardElevation.Low) {
            Column {
                d.monthlyRevenue.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.month.takeLast(2) + "/" + m.month.take(4), style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(50.dp))
                        val maxRev = d.monthlyRevenue.maxOfOrNull { it.revenue } ?: 1.0
                        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(GreenSurface)) { Box(Modifier.fillMaxHeight().fillMaxWidth((m.revenue / maxRev).toFloat()).clip(RoundedCornerShape(4.dp)).background(Green)) }
                        Spacer(Modifier.width(6.dp)); Text(FormatUtils.formatPrice(m.revenue), style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.width(60.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(ts.topVendorsCA, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        d.topVendors.forEachIndexed { idx, v ->
            TiKCard(elevation = TiKCardElevation.Low, modifier = Modifier.padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${idx + 1}", style = MaterialTheme.typography.titleSmall, color = if (idx < 3) Orange else TextTertiary, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) { Text(v.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(v.shopName, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                    Column(horizontalAlignment = Alignment.End) { Text(FormatUtils.formatPrice(v.revenue), style = MaterialTheme.typography.bodyMedium, color = Green, fontWeight = FontWeight.SemiBold); Text(ts.orderCountFmt.replace("%d", "${v.orderCount}"), style = MaterialTheme.typography.labelSmall, color = TextTertiary) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(ts.topProductsSold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        d.topProducts.forEachIndexed { idx, p ->
            TiKCard(elevation = TiKCardElevation.Low, modifier = Modifier.padding(bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${idx + 1}", style = MaterialTheme.typography.titleSmall, color = if (idx < 3) Orange else TextTertiary, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) { Text(p.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1); Text(p.shopName, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
                    Column(horizontalAlignment = Alignment.End) { Text(ts.soldCount.replace("%d", "${p.totalSold}"), style = MaterialTheme.typography.bodyMedium, color = Green, fontWeight = FontWeight.SemiBold); Text(FormatUtils.formatPrice(p.totalGenerated), style = MaterialTheme.typography.labelSmall, color = TextTertiary) }
                }
            }
        }

        if (d.ordersByStatus.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(ts.ordersByStatus, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            TiKCard(elevation = TiKCardElevation.Low) {
                Column {
                    val total = d.ordersByStatus.values.sum().toFloat()
                    d.ordersByStatus.forEach { (status, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            val orderStatus = OrderStatus.fromCode(status); Text(orderStatus.label, style = MaterialTheme.typography.labelSmall, color = TextPrimary, modifier = Modifier.width(80.dp))
                            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(GreenSurface)) { Box(Modifier.fillMaxHeight().fillMaxWidth(count / total).clip(RoundedCornerShape(4.dp)).background(Green)) }
                            Spacer(Modifier.width(6.dp)); Text("$count", style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(30.dp))
                        }
                    }
                }
            }
        }

        if (d.usersByRole.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(ts.usersByRole, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            TiKCard(elevation = TiKCardElevation.Low) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    d.usersByRole.forEach { (role, count) ->
                        val label = when (role) { "admin" -> ts.adminLabel; "vendor" -> ts.vendor; else -> ts.clientLabel }
                        val color = when (role) { "admin" -> RedAccent; "vendor" -> Orange; else -> Green }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$count", style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun KpiCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    TiKCard(elevation = TiKCardElevation.Low, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ═══════════════════════════════════════════════
//  ONLINE USERS
// ═══════════════════════════════════════════════

@Composable
fun AdminOnlineUsersContent(scope: kotlinx.coroutines.CoroutineScope, ts: com.tik_market.utils.AppStrings) {
    var data by remember { mutableStateOf<ApiOnlineUsersResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() { scope.launch { isLoading = true; error = null; try { data = ApiClient.fetchOnlineUsers() } catch (e: Exception) { error = e.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ts.onlineUsersTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            TextButton(onClick = { load() }) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else { Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(ts.refreshLabel) }
            }
        }
        if (isLoading && data == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return@Column }
        if (error != null && data == null) { TiKErrorState(message = error!!, onRetry = { load() }); return@Column }
        val d = data ?: return@Column

        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(12.dp), color = Green.copy(alpha = 0.1f)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Green, CircleShape)); Spacer(Modifier.width(8.dp))
                Text(ts.onlineUsersNow.replace("%d", "$d.totalOnline"), fontWeight = FontWeight.SemiBold, color = GreenDark)
            }
        }

        if (d.onlineUsers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PersonOff, null, Modifier.size(48.dp), tint = Color.LightGray); Spacer(Modifier.height(8.dp)); Text(ts.noOneOnline, color = Color.Gray) } }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(d.onlineUsers.size) { index ->
                    OnlineUserCard(d.onlineUsers[index], ts)
                }
            }
        }
    }
}

@Composable
private fun OnlineUserCard(user: ApiOnlineUser, ts: com.tik_market.utils.AppStrings) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Box(Modifier.size(44.dp).clip(CircleShape).background(GreenSurface), contentAlignment = Alignment.Center) {
                    if (user.avatar.isNotBlank()) {
                        var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                        LaunchedEffect(user.avatar) { bitmap = try { loadImageFromUrl(user.avatar) } catch (_: Exception) { null } }
                        if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Icon(Icons.Default.Person, null, Modifier.size(24.dp), tint = Green)
                    } else { Icon(Icons.Default.Person, null, Modifier.size(24.dp), tint = Green) }
                }
                Box(Modifier.size(14.dp).offset(x = (-2).dp, y = (-2).dp).align(Alignment.BottomEnd).background(Color.White, CircleShape).padding(2.dp)) { Box(Modifier.fillMaxSize().background(Green, CircleShape)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Medium, fontSize = 15.sp); Text(user.email, fontSize = 12.sp, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = when (user.role) { "admin" -> RedAccent.copy(alpha = 0.1f); "vendor" -> Orange.copy(alpha = 0.1f); else -> Green.copy(alpha = 0.1f) }) {
                        Text(when (user.role) { "admin" -> ts.adminLabel; "vendor" -> ts.vendor; else -> ts.clientLabel }, fontSize = 10.sp, color = when (user.role) { "admin" -> RedAccent; "vendor" -> Orange; else -> Green }, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.width(8.dp)); Text(ts.secondsAgo.replace("%d", "${user.secondsAgo}"), fontSize = 11.sp, color = TextTertiary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
//  STORIES
// ═══════════════════════════════════════════════

@Composable
fun AdminStoriesContent(scope: kotlinx.coroutines.CoroutineScope, shops: List<AdminShop>, ts: com.tik_market.utils.AppStrings) {
    var stories by remember { mutableStateOf<List<ApiStory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Create story state
    var showAddDialog by remember { mutableStateOf(false) }
    var newCaption by remember { mutableStateOf("") }
    var newMediaUrl by remember { mutableStateOf("") }
    var newMediaType by remember { mutableStateOf("image") }
    var newMediaDuration by remember { mutableStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; try { stories = ApiClient.fetchStories() } catch (e: Exception) { error = e.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(ts.allStories, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = { showAddDialog = true }, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text(ts.add)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else if (error != null) { TiKErrorState(message = error!!, onRetry = { load() }) }
        else if (stories.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(ts.noStoryNow, color = TextSecondary) } }
        else { LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(stories.size) { index -> val story = stories[index]; AdminStoryCard(story = story, ts, onDelete = { scope.launch { try { ApiClient.deleteStory(story.id); stories = stories.filter { it.id != story.id } } catch (e: Exception) { error = ts.deleteStoryError.replace("%s", e.message ?: "") } } }) } } }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showAddDialog = false },
            title = { Text(ts.newStoryAdmin) },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text(ts.mediaPickLabel, style = MaterialTheme.typography.labelSmall)
                    Text(ts.mediaAdjustNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    MediaPicker(
                        currentUrl = newMediaUrl,
                        onMediaPicked = { res ->
                            scope.launch {
                                try {
                                    isSubmitting = true
                                    val url = ApiClient.uploadImage(res.dataUrl, res.fileName)
                                    newMediaUrl = url
                                    newMediaType = if (res.mimeType.startsWith("video/")) "video" else "image"
                                    newMediaDuration = res.durationSeconds.toInt()
                                } catch (e: Exception) {
error = ts.mediaUploadError.replace("%s", e.message ?: "")
                                } finally { isSubmitting = false }
                            }
                        },
                        label = ts.selectMedia,
                        allowVideo = true,
                        maxDurationSeconds = 30
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCaption,
                        onValueChange = { newCaption = it },
                        label = { Text(ts.captionOptional) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            try {
                                ApiClient.createStory(shopId = shops.firstOrNull()?.id ?: 0, mediaUrl = newMediaUrl, mediaType = newMediaType, caption = newCaption, duration = newMediaDuration)
                                showAddDialog = false
                                newMediaUrl = ""; newCaption = ""; newMediaDuration = 0; load()
                            } catch (e: Exception) { error = e.message }
                            isSubmitting = false
                        }
                    },
                    enabled = !isSubmitting && newMediaUrl.isNotBlank()
                ) {
                    if (isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                    else Text(ts.publish)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSubmitting) { Text(ts.cancel) }
            }
        )
    }
}

@Composable
private fun AdminStoryCard(story: ApiStory, ts: com.tik_market.utils.AppStrings, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE0E0E0))) {
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(story.mediaUrl) { bitmap = try { loadImageFromUrl(story.mediaUrl) } catch (_: Exception) { null } }
                if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.PhotoLibrary, null, Modifier.size(24.dp), tint = Color.Gray)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (story.isAdmin != 0) "TIK-MARKET" else story.shopName.ifBlank { story.userName }, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (!story.caption.isNullOrBlank()) Text(story.caption!!, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${story.mediaType} · ${ts.replyCountFmt.replace("%d", "${story.replyCount}")}", fontSize = 11.sp, color = TextTertiary)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, ts.delete, tint = RedAccent) }
        }
    }
}

// ═══════════════════════════════════════════════
//  HERO PROMO
// ═══════════════════════════════════════════════

@Composable
fun AdminHeroContent(scope: kotlinx.coroutines.CoroutineScope, shops: List<AdminShop>, ts: com.tik_market.utils.AppStrings) {
    var heroItems by remember { mutableStateOf<List<com.tik_market.api.ApiHeroItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newSubtitle by remember { mutableStateOf("") }
    var newImageUrl by remember { mutableStateOf("") }
    var selectedShopId by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun load() { scope.launch { isLoading = true; error = null; try { heroItems = ApiClient.fetchHeroItems() } catch (e: Exception) { error = e.message }; isLoading = false } }
    LaunchedEffect(Unit) { load() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(ts.heroSectionMgmt, style = MaterialTheme.typography.titleLarge)
            Text(ts.heroModifyHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(16.dp))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(ts.addPromotion, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text(ts.heroTitleExample) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newSubtitle, onValueChange = { newSubtitle = it }, label = { Text(ts.heroSubtitleExample) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    Text(ts.heroMediaLabel, style = MaterialTheme.typography.labelSmall)
                    MediaPicker(
                        currentUrl = newImageUrl,
                        onMediaPicked = { res ->
                            scope.launch {
                                try {
                                    val url = ApiClient.uploadImage(res.dataUrl, res.fileName)
                                    newImageUrl = url
                                } catch (e: Exception) { error = ts.mediaUploadError.replace("%s", e.message ?: "") }
                            }
                        },
                        label = ts.selectMedia,
                        allowVideo = true,
                        maxDurationSeconds = 10,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = newImageUrl, onValueChange = { newImageUrl = it }, label = { Text(ts.orDirectUrl) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    Text(ts.shopToPromote, style = MaterialTheme.typography.labelSmall)
                    var showShopList by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { showShopList = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(shops.find { it.id == selectedShopId }?.name ?: ts.noShopSelected); Spacer(Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = showShopList, onDismissRequest = { showShopList = false }) {
                            DropdownMenuItem(text = { Text(ts.noneLabel) }, onClick = { selectedShopId = null; showShopList = false })
                            shops.forEach { shop -> DropdownMenuItem(text = { Text(shop.name) }, onClick = { selectedShopId = shop.id; showShopList = false }) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isSubmitting = true; try {
                                    ApiClient.createHeroItem(com.tik_market.api.ApiCreateHeroBody(title = newTitle, subtitle = newSubtitle, imageUrl = newImageUrl, shopId = selectedShopId))
                                    newTitle = ""; newSubtitle = ""; newImageUrl = ""; selectedShopId = null; load()
                                } catch (e: Exception) { error = e.message }; isSubmitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = !isSubmitting && newTitle.isNotBlank() && newImageUrl.isNotBlank()
                    ) {
                        if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        else Text(ts.addToHome)
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Text(ts.activeBanners, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)) }
        if (isLoading) { item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } } }
        else if (heroItems.isEmpty()) { item(span = { GridItemSpan(maxLineSpan) }) { Text(ts.noCustomBanner, style = MaterialTheme.typography.bodySmall, color = TextTertiary) } }
        else {
            items(heroItems.size) { index ->
                val item = heroItems[index]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)).background(Color.LightGray)) {
                            var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                            LaunchedEffect(item.imageUrl) { bitmap = loadImageFromUrl(item.imageUrl) }
                            if (bitmap != null) Image(bitmap!!, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
                            if (item.shopName != null) Text(ts.linkPrefix.replace("%s", item.shopName), fontSize = 10.sp, color = Orange, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { scope.launch { try { ApiClient.deleteHeroItem(item.id); load() } catch (_: Exception) {} } }) { Icon(Icons.Default.Delete, null, tint = RedAccent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleBarChart(data: List<Int>, modifier: Modifier = Modifier) {
    val max = data.maxOrNull()?.let { if (it == 0) 1 else it } ?: 1
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        data.forEach { v -> Box(Modifier.weight(1f).fillMaxHeight().background(GreenSurface, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) { Box(Modifier.fillMaxWidth().fillMaxHeight(v.toFloat() / max).background(Green, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) } }
    }
}
