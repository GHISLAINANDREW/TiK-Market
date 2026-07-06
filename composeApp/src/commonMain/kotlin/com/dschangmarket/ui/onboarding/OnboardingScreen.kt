package com.dschangmarket.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dschangmarket.theme.*
import com.dschangmarket.ui.components.loadImageFromUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val imageUrl: String,     // Unsplash photo
    val gradientFrom: Color,  // dégradé personnalisé par page
    val gradientTo: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Bienvenue sur DschangMarket",
                subtitle = "Achetez et vendez facilement des produits locaux au Cameroun. Poisson frais, légumes, artisanat, mode et bien plus !",
                imageUrl = "https://images.unsplash.com/photo-1587593810167-a84920ea0781?w=800&q=80",
                gradientFrom = Color(0xFF1B5E20),
                gradientTo = Color(0xFF2E7D32)
            ),
            OnboardingPage(
                title = "Le terroir à votre porte",
                subtitle = "Découvrez les produits authentiques du marché de Dschang : fruits, légumes, épices et spécialités locales livrés chez vous.",
                imageUrl = "https://images.unsplash.com/photo-1594142510255-a4968875560b?w=800&q=80",
                gradientFrom = Color(0xFFE65100),
                gradientTo = Color(0xFFFF8A65)
            ),
            OnboardingPage(
                title = "Mode & Tissus",
                subtitle = "L'élégance du pagne traditionnel camerounais. Trouvez les plus beaux tissus Wax, bazin et tenues sur mesure.",
                imageUrl = "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=800&q=80",
                gradientFrom = Color(0xFF6A1B9A),
                gradientTo = Color(0xFFCE93D8)
            ),
            OnboardingPage(
                title = "Prêt à commencer ?",
                subtitle = "Créez votre compte et explorez les meilleurs produits de Dschang. Paiement Mobile Money sécurisé.",
                imageUrl = "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800&q=80",
                gradientFrom = Color(0xFF0D47A1),
                gradientTo = Color(0xFF42A5F5)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // Animation de particules flottantes (cercles décoratifs)
    val infiniteTransition = rememberInfiniteTransition()
    val particleOffsets = (0..5).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween((3000 + i * 800).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    BoxWithConstraints {
        val screenW = maxWidth
        val screenH = maxHeight

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 2,
            key = { pages[it].title }
        ) { pageIndex ->
            val page = pages[pageIndex]
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction

            Box(Modifier.fillMaxSize()) {
                // ── Image de fond pleine page ──
                var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(page.imageUrl) {
                    bitmap = loadImageFromUrl(page.imageUrl)
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap as ImageBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(page.gradientFrom, page.gradientTo))))
                }

                // ── Overlay gradient (haut et bas) ──
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(
                                page.gradientFrom.copy(alpha = 0.85f),
                                Color.Transparent,
                                page.gradientTo.copy(alpha = 0.92f)
                            ),
                            startY = 0f,
                            endY = with(LocalDensity.current) { screenH.toPx() }
                        )
                    )
                )

                // ── Particules flottantes décoratives ──
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    particleOffsets.forEachIndexed { i, offset ->
                        val angle = offset.value * (PI / 180f).toFloat()
                        val radius = w * 0.3f + (i * 40f)
                        val cx = w / 2f + cos(angle + i * 1.2f) * radius
                        val cy = h / 2f + sin(angle + i * 1.4f) * (radius * 0.4f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.07f),
                            radius = 8f + i * 4f,
                            center = Offset(cx, cy)
                        )
                    }

                    // ── Cercles concentriques décoratifs ──
                    val cx = w / 2f
                    val cy = h * 0.3f
                    val ringAlpha = ((sin(particleOffsets[0].value * 0.02f) + 1f) / 2f).toFloat() * 0.12f
                    for (r in 0..3) {
                        drawCircle(
                            color = Color.White.copy(alpha = ringAlpha / (r + 1)),
                            radius = w * 0.15f + r * 25f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // ── Contenu textuel avec animations ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 160.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(600, delayMillis = 200)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 200))
                    ) {
                        Text(
                            page.title,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    offset = Offset(2f, 3f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(600, delayMillis = 400)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 400))
                    ) {
                        Text(
                            page.subtitle,
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(1f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // ── Parallax offset sur le contenu ──
                val parallaxOffset = pageOffset * 60f
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = parallaxOffset.dp)
                        .graphicsLayer { alpha = (1f - kotlin.math.abs(pageOffset * 0.3f)).coerceIn(0f, 1f) },
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {}
            }
        }

        // ── Overlay fixe en bas : indicateurs + bouton ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f)
                        ),
                        startY = 0f,
                        endY = 300f
                    )
                )
                .padding(top = 40.dp, bottom = 48.dp)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Indicateurs de page animés ──
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { i ->
                        val isSelected = pagerState.currentPage == i
                        val dotScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.3f else 0.7f,
                            animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f)
                        )
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (isSelected) 32.dp else 8.dp,
                                    height = 8.dp
                                )
                                .scale(dotScale)
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(Orange, Gold)))
                                    else Modifier.background(Color.White.copy(alpha = 0.35f))
                                )
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Bouton avec animation de pulsation ──
                val buttonScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (pagerState.currentPage == pages.size - 1) 1.05f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(buttonScale)
                        .shadow(
                            elevation = if (pagerState.currentPage == pages.size - 1) 16.dp else 8.dp,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pagerState.currentPage == pages.size - 1) Orange
                        else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Suivant" else "🚀 Commencer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pagerState.currentPage == pages.size - 1) Color.White
                        else Color.White.copy(alpha = 0.9f)
                    )
                }

                // ── Bouton Passer ──
                if (pagerState.currentPage < pages.size - 1) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pages.size - 1)
                            }
                        }
                    ) {
                        Text(
                            "Passer",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
