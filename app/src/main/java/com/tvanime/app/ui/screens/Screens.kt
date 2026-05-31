package com.tvanime.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tvanime.app.data.settings.PlaylistSource
import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.domain.model.DetectedMedia
import com.tvanime.app.ui.theme.*
import com.tvanime.app.ui.viewmodel.ExtractMediaUiState
import com.tvanime.app.ui.viewmodel.SettingsUiState
import androidx.compose.foundation.BorderStroke

private fun Modifier.tvFocus(focused: Boolean): Modifier = this
    .focusable()
    .scale(if (focused) 1.06f else 1f)
    .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
    .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(12.dp))

@Composable
fun HomeScreen(
    catalog: List<ContentItem>,
    onOpenExtractor: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentSelected: (ContentItem) -> Unit
) {
    val prim = MaterialTheme.colorScheme.primary
    val surfV = MaterialTheme.colorScheme.surfaceVariant

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(260.dp).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(surfV.copy(alpha = 0.5f), Color.Transparent)))
                    .padding(vertical = 48.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(Modifier.padding(start = 20.dp, bottom = 48.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Home, null, Modifier.size(32.dp), tint = prim)
                    Text("TVAnime", style = MaterialTheme.typography.headlineSmall,
                        color = prim, fontWeight = FontWeight.Bold)
                }

                NavItem(icon = Icons.Default.Home, label = "Inicio", active = true, onClick = {})
                NavItem(icon = Icons.Default.Search, label = "Buscar", onClick = onOpenExtractor)
                NavItem(icon = Icons.Default.Search, label = "Analizar URL", onClick = onOpenExtractor)
                NavItem(icon = Icons.Default.Star, label = "Mi biblioteca", onClick = {})
                NavItem(icon = Icons.Default.Favorite, label = "Favoritos", onClick = {})
                NavItem(icon = Icons.Default.Refresh, label = "Historial", onClick = {})
                NavItem(icon = Icons.Default.List, label = "Listas M3U", onClick = {})

                Spacer(Modifier.weight(1f))
                NavItem(icon = Icons.Default.Settings, label = "Ajustes", onClick = onOpenSettings)
            }

            Column(Modifier.fillMaxSize().padding(start = 16.dp)) {
                if (catalog.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(72.dp), tint = prim)
                            Text("TVAnimeApp", style = MaterialTheme.typography.displayMedium,
                                color = prim, fontWeight = FontWeight.Bold)
                            Text("Tu app de streaming para Android TV", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                TvButton("Analizar URL", Icons.Default.Search, onClick = onOpenExtractor)
                                TvButton("Ajustes", Icons.Default.Settings, onClick = onOpenSettings)
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(360.dp)) {
                        Box(Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(surfV, MaterialTheme.colorScheme.background))))
                        Box(Modifier.align(Alignment.BottomStart).padding(60.dp, 48.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = Color(0x22FFFFFF), shape = RoundedCornerShape(999.dp)) {
                                    Text("Catalogo", Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        color = prim, style = MaterialTheme.typography.labelMedium)
                                }
                                Text("TVAnime", style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text("Explora tu contenido favorito", style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Contenido", Modifier.padding(horizontal = 48.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(start = 48.dp, end = 40.dp)
                    ) {
                        items(catalog) { item ->
                            var focused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { onContentSelected(item) },
                                modifier = Modifier.width(300.dp)
                                    .focusable().onFocusChanged { focused = it.isFocused }
                                    .scale(if (focused) 1.1f else 1f)
                                    .border(if (focused) BorderStroke(5.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
                                    .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = surfV.copy(alpha = if (focused) 1f else 0.6f))
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 2)
                                    Spacer(Modifier.height(8.dp))
                                    Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
                    active: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary
    val bg = when {
        focused -> FocusBg
        active -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val fg = if (focused || active) prim else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.04f else 1f)
            .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .background(bg, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp), color = bg
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun TvButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.08f else 1f)
            .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp))
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (focused) prim else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, null, Modifier.size(22.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractMediaScreen(
    uiState: ExtractMediaUiState,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onExtract: () -> Unit,
    onPlayCandidate: (String, Map<String, String>) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSiteSelected: (com.tvanime.app.ui.components.SiteSuggestion) -> Unit,
    onAutoAnalyze: () -> Unit,
    onToggleSuggestions: (Boolean) -> Unit
) {
    val focusUrl = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusUrl.requestFocus() }

    val prim = MaterialTheme.colorScheme.primary
    val surfV = MaterialTheme.colorScheme.surfaceVariant
    val onSurf = MaterialTheme.colorScheme.onSurface

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            TvIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
            Text("Explorador Web", style = MaterialTheme.typography.headlineMedium, color = onSurf, fontWeight = FontWeight.Bold)
            TvIconButton(onClick = { onToggleSuggestions(!uiState.showSuggestions) }, icon = Icons.Default.KeyboardArrowDown)
        }
        Spacer(Modifier.height(20.dp))

        var urlTextField by remember { mutableStateOf(TextFieldValue(uiState.pageUrl)) }

        var urlFieldFocused by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.fillMaxWidth()
                .focusable().onFocusChanged { urlFieldFocused = it.isFocused }
                .border(2.dp, if (urlFieldFocused) FocusCyan else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = surfV.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Search, null, Modifier.size(28.dp), tint = prim)
                BasicTextField(
                    value = urlTextField,
                    onValueChange = {
                        urlTextField = it
                        onUrlChanged(it.text)
                    },
                    modifier = Modifier.weight(1f).focusRequester(focusUrl).focusable(),
                    textStyle = TextStyle(color = onSurf, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                    singleLine = true,
                    cursorBrush = SolidColor(prim),
                    decorationBox = { inner ->
                        Box {
                            if (urlTextField.text.isBlank()) {
                                Text("Pega la URL completa aqui...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyLarge)
                            }
                            inner()
                        }
                    }
                )
                if (uiState.pageUrl.isNotBlank()) {
                    TvIconButton(onClick = { urlTextField = TextFieldValue(""); onUrlChanged("") }, icon = Icons.Default.Clear, size = 20.dp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(14.dp)) {
            TvButton("Analizar", Icons.Default.PlayArrow) { onExtract() }
            TvButton("Auto", Icons.Default.Star) { onAutoAnalyze() }
        }

        uiState.autoAnalyzeProgress?.let {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = prim)
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (uiState.showSuggestions && uiState.result == null) {
            Spacer(Modifier.height(20.dp))
            Text("Sitios populares", style = MaterialTheme.typography.titleMedium, color = onSurf, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            com.tvanime.app.ui.components.WebSearchSuggestions(
                query = uiState.searchQuery,
                onSiteSelected = onSiteSelected,
                modifier = Modifier.weight(1f)
            )
        }

        uiState.result?.let { result ->
            Spacer(Modifier.height(20.dp))

            Surface(color = surfV.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(result.title, style = MaterialTheme.typography.titleLarge, color = onSurf, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.List, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.sourceHost, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val playable = result.candidates.filter { it.isDirect && it.format != "embed" }
            val fallback = result.candidates.filterNot { it in playable }
            val visible = if (playable.isNotEmpty()) playable else fallback

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    if (playable.isNotEmpty()) "Reproducibles: ${playable.size}" else "Detectados: ${fallback.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (playable.isNotEmpty()) Color(0xFF34D399) else Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
                Text("${result.candidates.size} total", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp)) {
                items(visible) { c -> CandidateCard(c, { onPlayCandidate(c.url, c.headers) }) }
            }
        }
    }
}

@Composable
private fun TvIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, size: androidx.compose.ui.unit.Dp = 28.dp) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary
    IconButton(onClick = onClick, modifier = Modifier
        .focusable().onFocusChanged { focused = it.isFocused }
        .border(if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
        .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
    ) { Icon(icon, "Accion", Modifier.size(size), tint = if (focused) prim else MaterialTheme.colorScheme.onSurface) }
}

@Composable
private fun CandidateCard(c: DetectedMedia, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val surf = MaterialTheme.colorScheme.surfaceVariant
    val fc = when (c.format.lowercase()) {
        "mp4" -> Color(0xFF34D399); "hls" -> Color(0xFF60A5FA)
        "webm" -> Color(0xFFA78BFA); "mkv" -> Color(0xFFFB923C)
        "ts" -> Color(0xFFF472B6); "audio" -> Color(0xFFFBBF24); else -> Color(0xFF94A3B8)
    }

    Card(
        onClick = onPlay,
        modifier = Modifier.width(340.dp)
            .focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.1f else 1f)
            .border(if (focused) BorderStroke(5.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = surf.copy(alpha = if (focused) 1f else 0.6f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(color = fc.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(c.format.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = fc, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                c.quality?.let {
                    Surface(color = fc.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(it.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = fc, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (c.isDirect) Icons.Default.CheckCircle else Icons.Default.Star, null, Modifier.size(18.dp),
                    tint = if (c.isDirect) Color(0xFF34D399) else Color(0xFFFBBF24))
                Text(c.server.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
            Text(c.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)

            var btnFocused by remember { mutableStateOf(false) }
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()
                .focusable().onFocusChanged { btnFocused = it.isFocused }
                .border(if (btnFocused) BorderStroke(3.dp, FocusCyan) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(999.dp)),
                enabled = c.isDirect,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (c.isDirect) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (c.isDirect) "Reproducir" else "No directo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState, onBack: () -> Unit, onSelectDemo: () -> Unit, onSelectRemoteUrl: () -> Unit,
    onRemoteUrlChanged: (String) -> Unit, onRecurringSitesChanged: (String) -> Unit, onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
        TvIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(20.dp))
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Text("Origen de playlist M3U", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton("Demo local", Icons.Default.List) { onSelectDemo() }
            TvButton("URL remota", Icons.Default.Edit) { onSelectRemoteUrl() }
        }
        if (uiState.selectedSource == PlaylistSource.REMOTE_URL) {
            Spacer(Modifier.height(14.dp))
            var urlField by remember { mutableStateOf(TextFieldValue(uiState.remoteUrl)) }
            var urlFocused by remember { mutableStateOf(false) }
            Surface(modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { urlFocused = it.isFocused }
                .border(2.dp, if (urlFocused) FocusCyan else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Email, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                    BasicTextField(value = urlField, onValueChange = { urlField = it; onRemoteUrlChanged(it.text) },
                        modifier = Modifier.fillMaxWidth().focusable(),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box { if (urlField.text.isBlank()) Text("https://...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyLarge); inner() }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Sitios recurrentes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Una URL por linea: URL | Categoria", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        var sitesField by remember { mutableStateOf(TextFieldValue(uiState.recurringSitesText)) }
        var sitesFocused by remember { mutableStateOf(false) }
        Surface(modifier = Modifier.fillMaxWidth().height(120.dp).focusable().onFocusChanged { sitesFocused = it.isFocused }
            .border(2.dp, if (sitesFocused) FocusCyan else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
            BasicTextField(value = sitesField, onValueChange = { sitesField = it; onRecurringSitesChanged(it.text) },
                modifier = Modifier.padding(16.dp).fillMaxSize().focusable(),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box { if (sitesField.text.isBlank()) Text("https://animeflv.net | Anime", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium); inner() }
                }
            )
        }
        Spacer(Modifier.height(24.dp))
        TvButton("Guardar y sincronizar", Icons.Default.Refresh) { onSave() }
    }
}

@Composable
fun DetailScreen(contentItem: ContentItem?, isFavorite: Boolean, onPlayClick: () -> Unit, onToggleFavorite: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
        TvIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(20.dp))
        contentItem?.let {
            Text(it.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(it.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton("Reproducir", Icons.Default.PlayArrow) { onPlayClick() }
                TvButton(if (isFavorite) "Quitar favorito" else "Agregar a favoritos", Icons.Default.Favorite) { onToggleFavorite() }
            }
        }
    }
}

@Composable
fun PlayerScreen(videoUrl: String, modifier: Modifier = Modifier, headers: Map<String, String> = emptyMap()) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoUrl) {
        val factory = DefaultHttpDataSource.Factory().apply { if (headers.isNotEmpty()) setDefaultRequestProperties(headers) }
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(factory))
            .build().apply { setMediaItem(MediaItem.Builder().setUri(videoUrl).build()); prepare(); playWhenReady = true }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    DisposableEffect(exoPlayer) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(s: Int) { currentPosition = exoPlayer.currentPosition; duration = exoPlayer.duration }
        }; exoPlayer.addListener(l); onDispose { exoPlayer.removeListener(l) }
    }
    LaunchedEffect(showControls) { if (showControls) { kotlinx.coroutines.delay(4000); showControls = false } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { ctx ->
            PlayerView(ctx).apply { player = exoPlayer; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = android.view.ViewGroup.LayoutParams(-1, -1) } }, modifier = Modifier.fillMaxSize())

        androidx.compose.animation.AnimatedVisibility(visible = showControls, modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
            Row(Modifier.fillMaxWidth().padding(20.dp), Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PlayerControlButton(Icons.Default.Home, { exoPlayer.seekTo(0) })
                PlayerControlButton(if (isPlaying) Icons.Default.Star else Icons.Default.Home, { if (isPlaying) exoPlayer.pause() else exoPlayer.play() })
                PlayerControlButton(Icons.Default.Home, { exoPlayer.seekTo(duration) })
            }
        }
    }
}

@Composable
private fun PlayerControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    IconButton(onClick = onClick, modifier = Modifier.size(60.dp)
        .focusable().onFocusChanged { focused = it.isFocused }
        .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
        .background(if (focused) FocusHighlight else Color.Transparent, RoundedCornerShape(12.dp))
    ) { Icon(icon, null, Modifier.size(32.dp), tint = Color.White) }
}