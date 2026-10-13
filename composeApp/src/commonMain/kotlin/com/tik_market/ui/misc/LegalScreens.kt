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
            LegalSection("1. Éditeur de la Plateforme", "La plateforme TiK-Market est éditée et exploitée par la société AUTENTIK SOFT SOLUTIONS SARLU, immatriculée au Cameroun. Contact support : AdminAutenTiK@gmail.com / +237 600000000.")
            LegalSection("2. Collecte des Données Personnelles", "Dans le cadre de l'utilisation de TiK-Market, nous collectons les informations que vous nous fournissez directement lors de l'inscription et de la création de boutique : Nom, prénom, adresse email, numéro de téléphone, photos de profil, documents d'identité (KYC pour les vendeurs), et position géographique GPS.")
            LegalSection("3. Utilisation de la Géolocalisation GPS", "La géolocalisation de votre appareil est utilisée pour deux objectifs principaux : permettre aux acheteurs de découvrir les boutiques situées à proximité (de 5m à 400m) et garantir la précision des livraisons de proximité. La position GPS de la boutique enregistrée lors de sa création est définitive et immuable.")
            LegalSection("4. Sécurité et Confidentialité des Données", "Vos données personnelles font l'objet d'un traitement informatique rigoureux. Les mots de passe sont cryptés via l'algorithme sécurisé Bcrypt. Vos informations bancaires et numéros de compte marchand Mobile Money sont strictement protégés et ne sont jamais partagés à des tiers non autorisés.")
            LegalSection("5. Droits des Utilisateurs (RGPD / Lois Locales)", "Conformément aux réglementations en vigueur, vous disposez d'un droit d'accès, de rectification et de suppression de vos données personnelles. Vous pouvez exercer ces droits à tout moment depuis les paramètres de votre profil ou en contactant notre support technique.")
            LegalSection("6. Propriété Intellectuelle", "L'ensemble des éléments textuels, graphiques, logiciels, interfaces et logos de TiK-Market sont la propriété exclusive d'AUTENTIK SOFT SOLUTIONS SARLU. Toute reproduction, modification ou exploitation non autorisée est strictement interdite.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                "Dernière mise à jour : Août 2026",
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
            LegalSection("1. Objet et Acceptation des CGU", "Les présentes Conditions Générales d'Utilisation régissent l'accès et l'utilisation de la place de marché TiK-Market. En créant un compte (Acheteur ou Vendeur/Partenaire), vous acceptez sans réserve l'intégralité de ces conditions.")
            LegalSection("2. Inscription et Validation des Comptes (KYC)", "L'inscription est ouverte aux particuliers et aux professionnels. Pour les marchands (Compte Partenaire), la création de boutique requiert des informations exactes, la capture GPS sur place (immuable) et la soumission optionnelle ou immédiate de pièces justificatives (KYC). TiK-Market se réserve le droit de valider ou de suspendre tout compte ne respectant pas nos critères de sécurité.")
            LegalSection("3. Commandes, Paiements et Livraisons", "Les transactions s'effectuent entre acheteurs et vendeurs locaux. Les paiements peuvent être réglés à la livraison ou via les comptes marchands Mobile Money configurés par les vendeurs. TiK-Market agit en tant qu'intermédiaire technologique et décline toute responsabilité en cas de litige sur une transaction hors plateforme.")
            LegalSection("4. Engagements des Vendeurs (Partenaires)", "Le vendeur s'engage à proposer des produits conformes, authentiques et de qualité, à honorer les commandes reçues et à respecter sa position géographique GPS déclarée lors de l'enregistrement initial de sa boutique.")
            LegalSection("5. Engagements des Acheteurs", "L'acheteur s'engage à fournir des coordonnées de livraison valides, à respecter les commerçants partenaires et à maintenir une conduite courtoise au sein de la messagerie instantanée intégrée.")
            LegalSection("6. Programme de Fidélité et Parrainage", "Les points de fidélité et bonus de parrainage sont octroyés conformément aux règles du programme en vigueur sur l'application. Ils ne constituent pas une monnaie ayant cours légal et ne sont pas convertibles en espèces.")
            LegalSection("7. Limitation de Responsabilité et Litiges", "TiK-Market ne saurait être tenu responsable des interruptions de service dues à la maintenance ou à des cas de force majeure. En cas de différend, une résolution amiable sera privilégiée entre les parties.")
            
            Spacer(Modifier.height(32.dp))
            Text(
                "En vous inscrivant sur TiK-Market, vous reconnaissez avoir lu, compris et accepté les présentes Conditions d'Utilisation.",
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
