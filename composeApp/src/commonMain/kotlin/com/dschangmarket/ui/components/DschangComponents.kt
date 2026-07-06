package com.dschangmarket.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dschangmarket.theme.*

// ═══════════════════════════════════════════
//  DSCHANG DESIGN SYSTEM — Composants pro
// ═══════════════════════════════════════════

// ─── CARD ──────────────────────────────────

enum class DschangCardElevation { None, Low, Raised }
enum class DschangCardShape { Small, Medium, Large }

@Composable
fun DschangCard(
    modifier: Modifier = Modifier,
    elevation: DschangCardElevation = DschangCardElevation.Low,
    shape: DschangCardShape = DschangCardShape.Medium,
    color: Color = CardWhite,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dp = when (elevation) {
        DschangCardElevation.None -> 0.dp
        DschangCardElevation.Low -> CardElevation
        DschangCardElevation.Raised -> CardElevationRaised
    }
    val shapeDp = when (shape) {
        DschangCardShape.Small -> CardShapeSmall
        DschangCardShape.Medium -> CardShapeMedium
        DschangCardShape.Large -> CardShapeLarge
    }
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(shapeDp),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = dp)
        ) { Column(Modifier.padding(12.dp), content = content) }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(shapeDp),
            colors = CardDefaults.cardColors(containerColor = color),
            elevation = CardDefaults.cardElevation(defaultElevation = dp)
        ) { Column(Modifier.padding(12.dp), content = content) }
    }
}

// ─── BUTTONS ───────────────────────────────

enum class DschangButtonVariant { Primary, Secondary, Danger, Outline }

@Composable
fun DschangButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DschangButtonVariant = DschangButtonVariant.Primary,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val (containerColor, contentColor) = when (variant) {
        DschangButtonVariant.Primary -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        DschangButtonVariant.Secondary -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        DschangButtonVariant.Danger -> RedAccent to Color.White
        DschangButtonVariant.Outline -> Color.Transparent to MaterialTheme.colorScheme.primary
    }
    val border = if (variant == DschangButtonVariant.Outline) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier).height(48.dp),
        shape = RoundedCornerShape(CardShapeSmall),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        border = border
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            if (icon != null) {
                Icon(icon, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun DschangTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Green
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

// ─── BADGES ────────────────────────────────

enum class DschangBadgeColor { Green, Orange, Red, Blue, Gray }

@Composable
fun DschangBadge(
    text: String,
    color: DschangBadgeColor = DschangBadgeColor.Green,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (color) {
        DschangBadgeColor.Green -> Green
        DschangBadgeColor.Orange -> Orange
        DschangBadgeColor.Red -> RedAccent
        DschangBadgeColor.Blue -> BlueAccent
        DschangBadgeColor.Gray -> TextSecondary
    }
    Surface(
        modifier = modifier,
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─── STATE DISPLAYS ────────────────────────

@Composable
fun DschangEmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

@Composable
fun DschangErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, Modifier.size(36.dp), tint = RedAccent)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = RedAccent, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            DschangButton("Réessayer", onClick = onRetry, variant = DschangButtonVariant.Danger)
        }
    }
}

// ─── SHIMMER ───────────────────────────────

@Composable
fun DschangShimmer(
    modifier: Modifier = Modifier,
    shape: DschangCardShape = DschangCardShape.Small
) {
    val shimmerColors = listOf(
        DividerGray,
        DividerGray.copy(alpha = 0.3f),
        DividerGray
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
    val shapeDp = when (shape) { DschangCardShape.Small -> CardShapeSmall; DschangCardShape.Medium -> CardShapeMedium; DschangCardShape.Large -> CardShapeLarge }
    Box(modifier = modifier.clip(RoundedCornerShape(shapeDp)).background(brush))
}

// ─── DIVIDER AVEC TEXTE ────────────────────

@Composable
fun DschangDividerWithText(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(DividerGray))
        Text("  $text  ", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        Box(Modifier.weight(1f).height(1.dp).background(DividerGray))
    }
}

// ─── SECTION HEADER ────────────────────────

@Composable
fun DschangSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        if (action != null) action()
    }
}

// ─── OUTLINED TEXT FIELD THÉMATISÉ ─────────

@Composable
fun DschangTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    prefix: (@Composable () -> Unit)? = null,
    error: String? = null,
    singleLine: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            trailingIcon = when {
                trailingIcon != null -> {{ trailingIcon() }}
                isPassword && onTogglePassword != null -> {
                    {
                        IconButton(onClick = onTogglePassword, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, Modifier.size(18.dp), tint = TextSecondary
                            )
                        }
                    }
                }
                else -> null
            },
            prefix = prefix,
            singleLine = singleLine,
            isError = error != null,
            supportingText = if (error != null) {{ Text(error, color = RedAccent, style = MaterialTheme.typography.labelSmall) }} else null,
            visualTransformation = if (isPassword && !showPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = DividerGray,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = TextSecondary,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                errorBorderColor = RedAccent,
                errorLabelColor = RedAccent
            ),
            shape = RoundedCornerShape(CardShapeSmall),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
        )
    }
}
