package com.tvanime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow

@Composable
fun UrlBar(
    currentUrl: String,
    onUrlChanged: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHomeClick: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    modifier: Modifier = Modifier
) {
    var urlText by remember(currentUrl) { mutableStateOf(currentUrl) }
    var urlFieldFocused by remember { mutableStateOf(false) }

    fun navigateFromText() {
        val raw = urlText.trim()
        if (raw.isBlank()) return
        val normalized = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        urlText = normalized
        onUrlChanged(normalized)
        onNavigate(normalized)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF1A1D21), RoundedCornerShape(14.dp))
            .border(2.dp, if (urlFieldFocused) FocusCyan else Color(0xFF2A2D31), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UrlBarButton(icon = Icons.Default.ArrowBack, label = "Atrás", enabled = canGoBack, onClick = onBack)
        UrlBarButton(icon = Icons.Default.ArrowForward, label = "Adelante", enabled = canGoForward, onClick = onForward)
        UrlBarButton(icon = Icons.Default.Refresh, label = "Recargar", enabled = true, onClick = onRefresh)

        Box(
            modifier = Modifier.weight(1f)
                .focusable()
                .onFocusChanged { urlFieldFocused = it.isFocused }
        ) {
            BasicTextField(
                value = urlText,
                onValueChange = {
                    urlText = it
                    onUrlChanged(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigateFromText() }),
                cursorBrush = SolidColor(FocusCyan),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Box {
                            if (urlText.isBlank()) {
                                Text(
                                    "Buscar o escribir URL...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            inner()
                        }
                    }
                }
            )
        }

        UrlBarButton(icon = Icons.Default.Home, label = "Inicio", enabled = true, onClick = onHomeClick)
        UrlBarButton(icon = Icons.Default.PlayArrow, label = "Ir", enabled = urlText.isNotBlank(), onClick = { navigateFromText() })

        if (urlText.isNotBlank()) {
            var clearFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    urlText = ""
                    onUrlChanged("")
                },
                modifier = Modifier
                    .size(36.dp)
                    .focusable()
                    .onFocusChanged { clearFocused = it.isFocused }
                    .border(
                        if (clearFocused) BorderStroke(2.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                        else BorderStroke(0.dp, Color.Transparent),
                        RoundedCornerShape(8.dp)
                    )
                    .background(if (clearFocused) FocusBg else Color.Transparent, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Clear, "Limpiar", Modifier.size(18.dp), tint = Color.White)
            }
        }
    }
}

private fun normalizeBrowserInput(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
    }
}

@Composable
private fun UrlBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.4f

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(10.dp)
            )
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = Color.White.copy(alpha = alpha)
        )
    }
}
