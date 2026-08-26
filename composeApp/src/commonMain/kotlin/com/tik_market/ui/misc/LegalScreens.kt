package com.tik_market.ui.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.BrandTopBarColor
import com.tik_market.utils.LocalAppStrings
import com.tik_market.utils.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalNoticeScreen(onBack: () -> Unit) {
    val ts = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.legalMentions, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LegalSection("1. Éditeur de la plateforme", "La plateforme TiK-Market est éditée par la société AUTENTIK SOFT SOLUTIONS SARLU, siège social basé au Cameroun. Contact : AdminAutenTiK@gmail.com")
            LegalSection("2. Objet du service", "TiK-Market est une place de marché numérique mettant en relation des vendeurs locaux et des acheteurs. TiK-Market n'est pas le vendeur des produits proposés, sauf mention contraire.")
            LegalSection("3. Hébergement", "Le service est hébergé sur des infrastructures sécurisées dédiées aux applications mobiles.")
            LegalSection("4. Propriété intellectuelle", "L'ensemble du contenu de l'application (textes, logos, images, icônes) est la propriété exclusive de TiK-Market ou de ses partenaires. Toute reproduction est interdite.")
            LegalSection("5. Protection des données", "Conformément aux lois en vigueur au Cameroun, vos informations personnelles (Nom, Téléphone, Email) ne sont utilisées que pour le bon fonctionnement du service de commande et de livraison.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                ts.lastUpdate.format("Août 2026"),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(onBack: () -> Unit) {
    val ts = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.termsOfUse, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LegalSection("1. Acceptation des conditions", "L'utilisation de TiK-Market implique l'acceptation pleine et entière des présentes conditions générales d'utilisation.")
            LegalSection("2. Transactions et Paiements", "Les paiements s'effectuent soit à la livraison, soit via virement direct au vendeur par Mobile Money. TiK-Market décline toute responsabilité en cas de litige sur une transaction directe non validée sur la plateforme.")
            LegalSection("3. Responsabilités du Vendeur", "Le vendeur est seul responsable de la conformité, de la qualité et de la disponibilité des produits proposés sur sa boutique.")
            LegalSection("4. Responsabilités de l'Acheteur", "L'acheteur s'engage à fournir des informations de livraison exactes et à honorer ses commandes.")
            LegalSection("5. Programme de fidélité", "Les points de fidélité sont cumulés lors des achats validés. Ils peuvent être échangés contre des coupons de réduction selon les barèmes en vigueur dans l'application.")
            LegalSection("6. Litiges", "En cas de litige, les parties s'engagent à rechercher une solution amiable avant toute action judiciaire.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                "En utilisant TiK-Market, vous reconnaissez avoir pris connaissance de ces conditions.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegalSection(title: String, content: String) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandTopBarColor)
        Spacer(Modifier.height(4.dp))
        Text(content, fontSize = 14.sp, lineHeight = 20.sp, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))
        Divider(color = Color(0xFFF0F0F0))
    }
}

@Composable
private fun Divider(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ts = LocalAppStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ts.about, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandTopBarColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = BrandTopBarColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("TiK", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = BrandTopBarColor)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("TiK-Market", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(ts.version.format("1.0.2"), fontSize = 14.sp, color = Color.Gray)
            
            Spacer(Modifier.height(32.dp))
            Text(
                "TiK-Market est une plateforme innovante de commerce de proximité opérée par la société :",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp
            )
            
            Spacer(Modifier.height(12.dp))
            Text(
                "AUTENTIK SOFT SOLUTIONS SARLU",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = BrandTopBarColor
            )
            Text(
                "Société à Responsabilité Limitée Unipersonnelle",
                fontSize = 13.sp,
                color = Color.Gray
            )
            
            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(ts.ourMissionTitle, fontWeight = FontWeight.Bold, color = BrandTopBarColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Digitaliser les marchés locaux et faciliter les échanges entre vendeurs et acheteurs grâce à des solutions logicielles innovantes et sécurisées.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            Text(ts.contactSupport, fontWeight = FontWeight.Bold)
            Text(com.tik_market.utils.Constants.SUPPORT_EMAIL, color = BrandTopBarColor)
            Text("Tel/WhatsApp : ${com.tik_market.utils.Constants.ASSISTANCE_PHONE}", color = BrandTopBarColor)
            
            Spacer(Modifier.weight(1f))
            Text(
                ts.allRights,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}
