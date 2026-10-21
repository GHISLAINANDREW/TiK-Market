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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Politique de Confidentialité & Mentions Légales", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
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
            LegalSection("1. Éditeur et Opérateur Légal", "La plateforme TiK-Market est éditée, développée et exploitée à titre exclusif par la société AUTENTIK SOFT SOLUTIONS SARLU (Société à Responsabilité Limitée Unipersonnelle), immatriculée au Registre du Commerce du Cameroun. Contact officiel : AdminAutenTiK@gmail.com / +237 600000000.")
            LegalSection("2. Collecte des Données et Conformité KYC", "Dans le cadre de l'exploitation de TiK-Market, AutenTik Soft Solution SARLU collecte et traite les informations nécessaires au fonctionnement de la marketplace : Nom, prénom, adresse email, numéro de téléphone, documents officiels d'identité (CNI recto/verso, photos de boutique, enseigne pour le KYC des vendeurs/partenaires) et numéro d'identifiant unique fiscal (NIU).")
            LegalSection("3. Géolocalisation GPS Certifiée", "La position GPS exacte de la boutique capturée lors de sa création constitue la référence officielle pour les livraisons et la découvrabilité par les acheteurs à proximité. Cette position est immuable et certifiée par les serveurs d'AutenTik Soft Solution SARLU.")
            LegalSection("4. Sécurité des Transactions et Mobile Money", "Toutes les transactions bancaires et Mobile Money (Orange Money, MTN Mobile Money) s'effectuent directement vers les comptes marchands agréés des vendeurs partenaires ou via les systèmes sécurisés d'AutenTik Soft Solution SARLU. Les mots de passe sont cryptés avec Bcrypt et aucune donnée sensible n'est transmise à des tiers sans accord explicite.")
            LegalSection("5. Droits d'Accès et de Rectification (Lois Locales / RGPD)", "Chaque utilisateur ou partenaire vendeur dispose d'un droit d'accès, de modification, de rectification et de suppression de ses données personnelles. Vous pouvez exercer ce droit à tout moment depuis les paramètres de votre compte ou en adressant une demande écrite à notre service juridique.")
            LegalSection("6. Propriété Intellectuelle et Droits Réservés", "L'ensemble des logiciels, architectures, interfaces, marques, logos, graphismes et algorithmes de TiK-Market sont la propriété intellectuelle exclusive d'AUTENTIK SOFT SOLUTIONS SARLU. Toute reproduction ou exploitation non autorisée fera l'objet de poursuites judiciaires.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                "Mentions Légales éditées par AutenTik Soft Solution SARLU - Octobre 2026",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
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
            LegalSection("1. Objet et Portée des CGU", "Les présentes Conditions Générales d'Utilisation définissent les relations contractuelles entre la société AUTENTIK SOFT SOLUTIONS SARLU (opérateur de TiK-Market) et tout utilisateur (Acheteur, Vendeur Partenaire, Livreur). L'utilisation de la plateforme implique l'acceptation sans réserve des présentes CGU.")
            LegalSection("2. Adhésion des Vendeurs Partenaires & KYC", "L'ouverture d'une boutique partenaire requiert la validation des 5 photos obligatoires (CNI recto/verso, vue intérieure, vue extérieure, enseigne), la saisie du numéro NIU fiscal et la capture GPS certifiée sur site. AutenTik Soft Solution SARLU se réserve le droit de refuser ou de révoquer l'accès à toute boutique non conforme.")
            LegalSection("3. Contrats de Partenariat et Fiches Vendeurs", "La validation d'un compte marchand donne lieu à l'émission d'une Fiche Officielle du Vendeur et d'un Contrat de Partenariat Commercial téléchargeables sous format certifié par AutenTik Soft Solution SARLU.")
            LegalSection("4. Obligations des Partenaires et Acheteurs", "Les vendeurs s'engagent à proposer des articles authentiques, à respecter les prix affichés et à honorer rapidement les commandes. Les acheteurs s'engagent à fournir des informations de livraison exactes et à adopter un comportement respectueux.")
            LegalSection("5. Responsabilité et Résolution des Différends", "AutenTik Soft Solution SARLU met à disposition les outils technologiques de mise en relation. En cas de différend commercial, notre service d'assistance interviendra pour favoriser une résolution amiable.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                "En poursuivant la navigation sur TiK-Market, vous confirmez votre adhésion aux présentes Conditions Générales édictées par AutenTik Soft Solution SARLU.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
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
