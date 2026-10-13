package com.tik_market.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Green
import com.tik_market.theme.Orange
import kotlinx.coroutines.delay

@Composable
fun WelcomeRoleScreen(
    onBuyerClick: () -> Unit,
    onVendorClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100L)
        visible = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            BrandTopBarColor.copy(alpha = 0.08f),
                            Color.White
                        )
                    )
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(animationSpec = tween(600)) { it / 4 },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo / Icon header
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = BrandTopBarColor.copy(alpha = 0.1f),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TiK",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandTopBarColor
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // App Title / Branding
                    Text(
                        text = "BIENVENUE SUR TIK-MARKET",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandTopBarColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Le marché plus près de vous",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(40.dp))

                    // Question card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quel type de compte voulez-vous créer ?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(20.dp))

                            // Button 1: Compte Acheteur
                            RoleOptionButton(
                                title = "Compte Acheteur",
                                subtitle = "Achetez vos produits locaux en toute simplicité",
                                icon = Icons.Default.Person,
                                color = Green,
                                onClick = onBuyerClick
                            )

                            Spacer(Modifier.height(14.dp))

                            // Button 2: Compte Vendeur / Partenaire
                            RoleOptionButton(
                                title = "Compte Vendeur / Partenaire",
                                subtitle = "Vendez vos produits, faites des lives et gérez votre boutique",
                                icon = Icons.Default.Storefront,
                                color = Orange,
                                onClick = onVendorClick
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                        Text(
                            text = "  ou  ",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                    }

                    Spacer(Modifier.height(24.dp))

                    // Button 3: J'ai déjà un compte
                    TextButton(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandTopBarColor)
                    ) {
                        Text(
                            text = "J'ai déjà un compte",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleOptionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}
