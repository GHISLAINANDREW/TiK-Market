package com.tik_market.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = Dp.Unspecified,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
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
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
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
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .then(if (height != Dp.Unspecified) Modifier.height(height) else Modifier)
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun ProductShimmer() {
    Column(
        modifier = Modifier
            .width(160.dp)
            .padding(8.dp)
    ) {
        ShimmerItem(height = 120.dp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        ShimmerItem(height = 16.dp, width = 100.dp)
        Spacer(Modifier.height(4.dp))
        ShimmerItem(height = 12.dp, width = 60.dp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            ShimmerItem(height = 20.dp, width = 50.dp)
            Spacer(Modifier.weight(1f))
            ShimmerItem(height = 24.dp, width = 24.dp, shape = RoundedCornerShape(12.dp))
        }
    }
}

@Composable
fun StoryShimmer() {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        ShimmerItem(width = 68.dp, height = 96.dp, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(4.dp))
        ShimmerItem(width = 40.dp, height = 8.dp)
    }
}
