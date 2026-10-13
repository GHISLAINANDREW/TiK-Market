package com.tik_market.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.theme.Green
import com.tik_market.theme.Orange

@Composable
fun WelcomeRoleScreen(
    onBuyerClick: () -> Unit,
    onVendorClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title / Branding
            Text(
                text = "BIENVENUE SUR TIK-MARKET",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BrandTopBarColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Le marché plus près de vous",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Question prompt
            Text(
                text = "Quel type de compte voulez-vous créer ?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Button 1: Compte Acheteur
            Button(
                onClick = onBuyerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Compte Acheteur",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            // Button 2: Compte Vendeur / Partenaire
            Button(
                onClick = onVendorClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Icon(Icons.Default.Storefront, null, tint = Color.White)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Compte Vendeur / Partenaire",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                Text(
                    text = "  ou  ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Box(Modifier.weight(1f).height(1.dp).background(Color.LightGray))
            }

            Spacer(Modifier.height(32.dp))

            // Button 3: J'ai déjà un compte
            OutlinedButton(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BrandTopBarColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandTopBarColor)
            ) {
                Text(
                    text = "J'ai déjà un compte",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
