package com.tvanime.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvanime.app.domain.model.DetectedMedia

@Composable
fun CandidateCard(
    candidate: DetectedMedia,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatColor = when (candidate.format.lowercase()) {
        "mp4" -> Color(0xFF4CAF50)
        "hls", "m3u8" -> Color(0xFF2196F3)
        "webm" -> Color(0xFF9C27B0)
        "mkv" -> Color(0xFFFF5722)
        "audio" -> Color(0xFFFF9800)
        else -> Color(0xFF607D8B)
    }

    val qualityBadge = candidate.quality?.uppercase() ?: when {
        candidate.url.contains("1080") -> "1080P"
        candidate.url.contains("720") -> "720P"
        candidate.url.contains("480") -> "480P"
        else -> null
    }

    Card(
        modifier = modifier
            .width(380.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = formatColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = candidate.format.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = formatColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (qualityBadge != null) {
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = qualityBadge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color(0xFFFFD700),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (candidate.server) {
                        "streamtape" -> Icons.Default.PlayArrow
                        "streamwish", "voe", "mixdrop" -> Icons.Default.Star
                        "directo", "m3u8hls" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Favorite
                    },
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = candidate.server.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                color = if (candidate.isDirect) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (candidate.isDirect) Icons.Default.CheckCircle else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (candidate.isDirect) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (candidate.isDirect) "Reproducción directa" else "Requiere resolución",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (candidate.isDirect) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }

            Text(
                text = candidate.url,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (candidate.diagnostics.isNotEmpty()) {
                Text(
                    text = candidate.diagnostics.take(2).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = candidate.isDirect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = formatColor,
                    disabledContainerColor = Color(0xFF424242)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (candidate.isDirect) "Reproducir" else "No disponible",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
