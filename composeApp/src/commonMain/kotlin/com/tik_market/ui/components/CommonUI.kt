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
                moveTo(w * 0.15f, h * 0.45f)
                lineTo(w * 0.85f, h * 0.45f)
                lineTo(w * 0.80f, h * 0.92f)
                lineTo(w * 0.20f, h * 0.92f)
                close()
            }
            drawPath(path = bagPath, color = color)
            
            // Small "T" cut-out inside the bag
            // (Simulated by drawing a white/surface color T)
            val tPath = Path().apply {
                // Horizontal bar
                moveTo(w * 0.40f, h * 0.65f)
                lineTo(w * 0.60f, h * 0.65f)
                // Vertical bar
                moveTo(w * 0.50f, h * 0.65f)
                lineTo(w * 0.50f, h * 0.85f)
            }
            drawPath(path = tPath, color = Color.White, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
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
