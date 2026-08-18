package com.tik_market.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.data.Resource

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition()
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun <T> ResourceBox(
    resource: Resource<T>,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
    loadingContent: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    },
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Loading -> loadingContent()
        is Resource.Error -> {
            Column(
                modifier = modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Une erreur est survenue", style = MaterialTheme.typography.titleMedium)
                Text(resource.message, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Réessayer")
                }
            }
        }
        is Resource.Success -> content(resource.data)
        else -> Unit
    }
}

/**
 * TiK-Market Official Logo: Concept "Sac-GPS"
 * A shopping bag where the handle forms a GPS Location Pin.
 */
@Composable
fun TiKLogo(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    showText: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val w = size.width
            val h = size.height
            
            // Draw Bag Handle as GPS Pin (Stroke)
            drawCircle(
                color = color,
                radius = w * 0.22f,
                center = Offset(w * 0.5f, h * 0.28f),
                style = Stroke(width = w * 0.08f)
            )
            // Dot in center of GPS Pin
            drawCircle(
                color = color,
                radius = w * 0.06f,
                center = Offset(w * 0.5f, h * 0.28f)
            )
            
            // Draw Shopping Bag Body (Filled with rounded corners look)
            val bagPath = Path().apply {
                moveTo(w * 0.20f, h * 0.45f)
                lineTo(w * 0.80f, h * 0.45f)
                lineTo(w * 0.80f, h * 0.85f)
                quadraticBezierTo(w * 0.80f, h * 0.95f, w * 0.70f, h * 0.95f)
                lineTo(w * 0.30f, h * 0.95f)
                quadraticBezierTo(w * 0.20f, h * 0.95f, w * 0.20f, h * 0.85f)
                close()
            }
            drawPath(path = bagPath, color = color)
            
            // Draw "TiK" text inside (simplified paths for cross-platform canvas)
            // This replaces the old "T" with the full "TiK" style from the web icon
            val textScale = w / 100f
            
            // T
            val tPath = Path().apply {
                moveTo(38f * textScale, 62f * textScale)
                lineTo(48f * textScale, 62f * textScale)
                lineTo(48f * textScale, 65f * textScale)
                lineTo(44f * textScale, 65f * textScale)
                lineTo(44f * textScale, 77f * textScale)
                lineTo(42f * textScale, 77f * textScale)
                lineTo(42f * textScale, 65f * textScale)
                lineTo(38f * textScale, 65f * textScale)
                close()
            }
            // i
            val iPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(51f * textScale, 62f * textScale, 53f * textScale, 64f * textScale))
                addRect(androidx.compose.ui.geometry.Rect(51f * textScale, 66f * textScale, 53f * textScale, 77f * textScale))
            }
            // k
            val kPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(57f * textScale, 62f * textScale, 59f * textScale, 77f * textScale))
                moveTo(59f * textScale, 70f * textScale)
                lineTo(64f * textScale, 62f * textScale)
                lineTo(66f * textScale, 62f * textScale)
                lineTo(61f * textScale, 70f * textScale)
                lineTo(67f * textScale, 77f * textScale)
                lineTo(65f * textScale, 77f * textScale)
                lineTo(60f * textScale, 71f * textScale)
                lineTo(59f * textScale, 72f * textScale)
                lineTo(59f * textScale, 77f * textScale)
                lineTo(57f * textScale, 77f * textScale)
                close()
            }
            
            drawPath(path = tPath, color = Color.White)
            drawPath(path = iPath, color = Color.White)
            drawPath(path = kPath, color = Color.White)
        }
        
        if (showText) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TiK", 
                    fontWeight = FontWeight.Black, 
                    fontSize = 18.sp, 
                    color = color,
                    letterSpacing = 1.sp
                )
                Text(
                    "-MARKET",
                    fontWeight = FontWeight.Light, 
                    fontSize = 18.sp, 
                    color = color.copy(alpha = 0.8f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
