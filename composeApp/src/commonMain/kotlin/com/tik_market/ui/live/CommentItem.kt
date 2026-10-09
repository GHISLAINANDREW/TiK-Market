package com.tik_market.ui.live

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tik_market.api.dto.ApiLiveComment
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun CommentItem(
    comment: ApiLiveComment,
    onClickLike: (Int) -> Unit = {},
    onLongClick: (Int) -> Unit = {}
) {
    val createdAt = remember(comment.createdAt) {
        try {
            Instant.parse(comment.createdAt).toLocalDateTime(TimeZone.UTC)
        } catch (_: Exception) {
            null
        }
    }

    val timeAgo by remember(createdAt) { mutableStateOf(computeTimeAgo(createdAt)) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (comment.city.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                comment.city,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Text(
                    text = timeAgo,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comment.text,
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onClickLike(comment.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (comment.likedByMe) Color.Red else Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        "${comment.likes_count}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLongClick(comment.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun computeTimeAgo(createdAt: LocalDateTime?): String {
    if (createdAt == null) return ""
    return try {
        val now = Clock.System.now()
        val createdInstant = createdAt.toInstant(TimeZone.UTC)
        val diffMs = now.toEpochMilliseconds() - createdInstant.toEpochMilliseconds()

        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        when {
            diffSec < 60 -> "il y a ${diffSec}s"
            diffMin < 60 -> "il y a ${diffMin}m"
            diffHour < 24 -> "il y a ${diffHour}h"
            else -> "il y a ${diffDay}j"
        }
    } catch (_: Exception) {
        ""
    }
}
