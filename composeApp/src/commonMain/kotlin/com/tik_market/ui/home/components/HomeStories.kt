package com.tik_market.ui.home.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.theme.DividerGray
import com.tik_market.ui.components.loadImageFromUrl
import com.tik_market.ui.story.StoryItem
import com.tik_market.utils.LocalAppStrings
import com.tik_market.theme.LocalCityColors

@Composable
fun HomeStories(
    stories: List<StoryItem>,
    userRole: String,
    viewedStoryIds: Set<Int>,
    isLoading: Boolean = false,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    val s = LocalAppStrings.current
    val cityColors = LocalCityColors.current
    val primary = MaterialTheme.colorScheme.primary

    if (!isLoading && stories.isEmpty() && userRole != "vendor" && userRole != "admin" && userRole != "super_admin") return

    Column(Modifier.padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.arrivalsToday, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(6.dp))
            Text("🔥", fontSize = 14.sp)
        }
        
        Spacer(Modifier.height(4.dp))
        
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (userRole == "vendor" || userRole == "admin" || userRole == "super_admin") {
                AddStoryButton(onClick = onAddStoryClick, primary = primary)
            }
            
            if (isLoading && stories.isEmpty()) {
                repeat(5) {
                    com.tik_market.ui.components.StoryShimmer()
                }
            } else {
                stories.forEachIndexed { index, item ->
                    StoryThumbnail(
                        item = item,
                        isSelected = item.storyId > 0 && item.storyId !in viewedStoryIds,
                        cityColors = cityColors,
                        primary = primary,
                        onClick = { onStoryClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AddStoryButton(onClick: () -> Unit, primary: Color) {
    val s = LocalAppStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier.width(68.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE0E0E0).copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Add, null, tint = primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(2.dp))
                Text(s.story, fontSize = 10.sp, color = primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(s.add, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primary)
    }
}

@Composable
private fun StoryThumbnail(
    item: StoryItem,
    isSelected: Boolean,
    cityColors: com.tik_market.theme.CityColors,
    primary: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier.width(72.dp)
                .height(96.dp)
                .then(
                    if (isSelected) Modifier.border(2.dp, cityColors.gradient, RoundedCornerShape(14.dp)).padding(2.dp)
                    else Modifier.border(1.dp, DividerGray, RoundedCornerShape(14.dp)).padding(1.dp)
                )
        ) {
            Box(
                Modifier.width(68.dp)
                    .height(92.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.imageUrl.isEmpty()) primary else Color(0xFFF0F0F0))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(item.imageUrl) {
                        bitmap = loadImageFromUrl(item.imageUrl)
                    }
                    
                    if (bitmap != null) {
                        Image(
                            bitmap!!,
                            null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.title.take(1),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        }
                    }
                    
                    if (item.mediaType == "video") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp).padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(68.dp),
            textAlign = TextAlign.Center
        )
    }
}
