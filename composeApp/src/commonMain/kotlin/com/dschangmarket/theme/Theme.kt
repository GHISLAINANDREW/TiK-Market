package com.dschangmarket.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
val DarkGreenOrange = Color(0xFF1B3022) // Vert sombre
val BrandGradient = Brush.horizontalGradient(listOf(Orange, GreenDark))
val BrandTopBarColor = Color(0xFF2E4D23)

// ── Neutral / Surface ──
val SurfaceWhite = Color(0xFFFFFFFF)
val BackgroundViolet = Color(0xFFF5F3FF) // Violet très clair, plus doux
val BackgroundGray = Color(0xFFF8F9FA)
val CardWhite = Color(0xFFFFFFFF)
val DividerGray = Color(0xFFEEEEEE)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val SurfaceElevated = Color(0xFFFFFFFF)

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
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFFE65100),
    tertiary = Violet,
    background = BackgroundViolet,
    surface = BackgroundViolet,
    surfaceVariant = Color(0xFFEBE7F2),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = RedAccent,
    outline = DividerGray,
    outlineVariant = Color(0xFFE5E7EB)
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenSurface,
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E2723),
    onSecondaryContainer = Color(0xFFFFCC80),
    tertiary = VioletLight,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFEF5350),
    outline = Color(0xFF444466)
)

// ── Typography ──
private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

val CardElevation = 0.5.dp
val CardElevationRaised = 2.dp
val CardShapeLarge = 16.dp
val CardShapeMedium = 12.dp
val CardShapeSmall = 8.dp

@Composable
fun DschangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
