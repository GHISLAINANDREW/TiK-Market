package com.tik_market.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand Palette ──
val GreenDark = Color(0xFF1B5E20)
val Green = Color(0xFF2E7D32)
val GreenLight = Color(0xFF4CAF50)
val GreenSurface = Color(0xFFE8F5E9)
val GreenAccent = Color(0xFF43A047)
val GreenAccentLight = Color(0xFF81C784)
val GreenAccentSurface = Color(0xFFC8E6C9)
val Amber = Color(0xFFFFD600)
val Gold = Color(0xFFFFD600)
val Orange = Color(0xFFFF9800)
val Violet = Color(0xFF6200EE)
val VioletLight = Color(0xFFBB86FC)
val VioletSoft = Color(0xFFF3E5F5)
val Brown = Color(0xFF5D4037)
val RedAccent = Color(0xFFD32F2F)
val BlueAccent = Color(0xFF1565C0)
val DarkGreenOrange = Color(0xFF1B3022)
val BrandGradient = Brush.horizontalGradient(listOf(Orange, GreenDark))
val BrandTopBarColor = Color(0xFF2E4D23)

// ── City Specific Palettes ──

data class CityColors(
    val primary: Color,
    val secondary: Color,
    val gradient: Brush,
    val topBar: Color
)

val TikMarketColors = CityColors(
    primary = Green,
    secondary = Orange,
    gradient = Brush.horizontalGradient(listOf(Orange, GreenDark)),
    topBar = Color(0xFF2E4D23)
)

val BafoussamColors = CityColors(
    primary = Color(0xFFD32F2F), // Rouge Passion
    secondary = Color(0xFFFFD600), // Or
    gradient = Brush.horizontalGradient(listOf(Color(0xFFFFD600), Color(0xFFD32F2F))),
    topBar = Color(0xFF8B0000)
)

val DoualaColors = CityColors(
    primary = Color(0xFF1976D2), // Bleu Océan
    secondary = Color(0xFFBBDEFB),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF1976D2), Color(0xFF64B5F6))),
    topBar = Color(0xFF0D47A1)
)

val YaoundeColors = CityColors(
    primary = Color(0xFF388E3C), // Vert Forêt
    secondary = Color(0xFFD32F2F),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF388E3C), Color(0xFFD32F2F))),
    topBar = Color(0xFF1B5E20)
)

val BamendaColors = CityColors(
    primary = Color(0xFF7B1FA2), // Violet Royal
    secondary = Color(0xFFE1BEE7),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0))),
    topBar = Color(0xFF4A148C)
)

val GarouaColors = CityColors(
    primary = Color(0xFFFBC02D), // Jaune Sahel
    secondary = Color(0xFFF57F17),
    gradient = Brush.horizontalGradient(listOf(Color(0xFFFBC02D), Color(0xFFF57F17))),
    topBar = Color(0xFFBF8F00)
)

val KribiColors = CityColors(
    primary = Color(0xFF00ACC1), // Turquoise Plage
    secondary = Color(0xFFE0F7FA),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF00ACC1), Color(0xFF007C91))),
    topBar = Color(0xFF00838F)
)

val BueaColors = CityColors(
    primary = Color(0xFF5E35B1), // Violet Montagne
    secondary = Color(0xFFD1C4E9),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF5E35B1), Color(0xFF4527A0))),
    topBar = Color(0xFF311B92)
)

val BertouaColors = CityColors(
    primary = Color(0xFF8BC34A), // Vert Lime
    secondary = Color(0xFFFFC107),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF8BC34A), Color(0xFFFFC107))),
    topBar = Color(0xFF33691E)
)

val EdeaColors = CityColors(
    primary = Color(0xFF00897B), // Vert Eau
    secondary = Color(0xFFB2DFDB),
    gradient = Brush.horizontalGradient(listOf(Color(0xFF00897B), Color(0xFF004D40))),
    topBar = Color(0xFF00695C)
)

val DefaultColors = TikMarketColors
val LocalCityColors = staticCompositionLocalOf { DefaultColors }

// ── Neutral / Surface ──
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundViolet = Color(0xFFF5F3FF)
val BackgroundGray = Color(0xFFF8F9FA)
val CardWhite = Color(0xFFFFFFFF)
val DividerGray = Color(0xFFEEEEEE)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)

// ── Dark theme ──
private val DarkBackground = Color(0xFF0F0F1A)
private val DarkSurface = Color(0xFF1A1A2E)
private val DarkCard = Color(0xFF252540)
private val DarkText = Color(0xFFE8E8E8)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenDark,
    secondary = Orange,
    onSecondary = Color.White,
    tertiary = Violet,
    background = BackgroundGray,
    surface = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenSurface,
    secondary = Orange,
    onSecondary = Color.White,
    tertiary = VioletLight,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkText,
    onSurface = DarkText
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun TiKMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    city: String? = null,
    content: @Composable () -> Unit
) {
    val cityColors = when {
        city?.contains("Bafoussam", ignoreCase = true) == true || city?.contains("Fu'sap", ignoreCase = true) == true || 
        city?.contains("Mbouda", ignoreCase = true) == true || city?.contains("Bangangté", ignoreCase = true) == true || 
        city?.contains("Foumban", ignoreCase = true) == true -> BafoussamColors
        
        city?.contains("Douala", ignoreCase = true) == true || city?.contains("Nkongsamba", ignoreCase = true) == true -> DoualaColors
        
        city?.contains("Yaoundé", ignoreCase = true) == true || city?.contains("Yaounde", ignoreCase = true) == true || 
        city?.contains("Ebolowa", ignoreCase = true) == true -> YaoundeColors
        
        city?.contains("Bamenda", ignoreCase = true) == true -> BamendaColors
        
        city?.contains("Garoua", ignoreCase = true) == true || city?.contains("Maroua", ignoreCase = true) == true || 
        city?.contains("Ngaoundéré", ignoreCase = true) == true -> GarouaColors
        
        city?.contains("Kribi", ignoreCase = true) == true || city?.contains("Limbé", ignoreCase = true) == true -> KribiColors
        
        city?.contains("Buea", ignoreCase = true) == true -> BueaColors
        
        city?.contains("Bertoua", ignoreCase = true) == true -> BertouaColors
        
        city?.contains("Edéa", ignoreCase = true) == true -> EdeaColors

        else -> TikMarketColors
    }

    val colorScheme = if (darkTheme) {
        DarkColors.copy(primary = cityColors.primary, secondary = cityColors.secondary)
    } else {
        LightColors.copy(primary = cityColors.primary, secondary = cityColors.secondary)
    }

    CompositionLocalProvider(LocalCityColors provides cityColors) {
        MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
    }
}
