package com.dschangmarket.ui.misc

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(onBack: () -> Unit, onResult: (String) -> Unit = {}) {
    var isFlashOn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner un produit", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview Placeholder
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Initialisation de la caméra...", color = Color.White.copy(alpha = 0.7f))
                }
            }

            // Scanner Overlay
            ScannerOverlay(Modifier.fillMaxSize())

            // Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Placez le code-barres dans le cadre pour le scanner",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                    fontSize = 14.sp
                )
                
                Spacer(Modifier.height(32.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            null,
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { onResult("690123456789") }, // Simulate successful scan
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                    ) {
                        Icon(Icons.Default.Image, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val lineY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier) {
        // Darken the outside area
        // In a real implementation, we would use a custom painter to punch a hole
        
        // Scan frame
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(width = 280.dp, height = 200.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            // Scanning line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.01f)
                    .align(Alignment.TopCenter)
                    .offset(y = 200.dp * lineY)
                    .background(Color.Red)
            )
            
            // Corners (decorations)
            Box(Modifier.fillMaxSize()) {
                val cornerSize = 20.dp
                val thickness = 4.dp
                val color = Color.White
                
                // Top Left
                Box(Modifier.size(cornerSize).align(Alignment.TopStart).border(thickness, color, RoundedCornerShape(topStart = 12.dp)).padding(thickness))
                // Top Right
                Box(Modifier.size(cornerSize).align(Alignment.TopEnd).border(thickness, color, RoundedCornerShape(topEnd = 12.dp)).padding(thickness))
                // Bottom Left
                Box(Modifier.size(cornerSize).align(Alignment.BottomStart).border(thickness, color, RoundedCornerShape(bottomStart = 12.dp)).padding(thickness))
                // Bottom Right
                Box(Modifier.size(cornerSize).align(Alignment.BottomEnd).border(thickness, color, RoundedCornerShape(bottomEnd = 12.dp)).padding(thickness))
            }
        }
    }
}
