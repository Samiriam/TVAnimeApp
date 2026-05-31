package com.tvanime.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow

@Composable
fun VideoCaptureOverlay(
    stream: VideoStreamUI?,
    isVisible: Boolean,
    onPlayVideo: (String, Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && stream != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        if (stream != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1D21).copy(alpha = 0.95f)
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = FocusCyan
                            )
                            Text(
                                text = "Video detectado",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            FormatBadge(format = stream.format)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stream.domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = stream.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    var playFocused by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            val headers = if (stream.referer.isNotBlank()) {
                                mapOf("Referer" to stream.referer)
                            } else emptyMap()
                            onPlayVideo(stream.url, headers)
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .focusable()
                            .onFocusChanged { playFocused = it.isFocused }
                            .scale(if (playFocused) 1.05f else 1f)
                            .border(
                                if (playFocused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                                else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(999.dp)
                            )
                            .background(
                                if (playFocused) Color(0x33FFFFFF) else Color(0xFF00CED1),
                                RoundedCornerShape(999.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(28.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reproducir en TV",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    var dismissFocused by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .focusable()
                            .onFocusChanged { dismissFocused = it.isFocused }
                            .border(
                                if (dismissFocused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                                else BorderStroke(0.dp, Color.Transparent),
                                RoundedCornerShape(12.dp)
                            )
                            .background(if (dismissFocused) FocusBg else Color.Transparent, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", Modifier.size(24.dp), tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(format: String) {
    val color = when (format.uppercase()) {
        "HLS" -> Color(0xFF60A5FA)
        "MP4" -> Color(0xFF34D399)
        "WEBM" -> Color(0xFFA78BFA)
        "MKV" -> Color(0xFFFB923C)
        "TS" -> Color(0xFFF472B6)
        else -> Color(0xFF94A3B8)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = format.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

data class VideoStreamUI(
    val url: String,
    val format: String,
    val domain: String,
    val referer: String
)