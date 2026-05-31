package com.tvanime.app.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.tvanime.app.ui.viewmodel.ExtractMediaUiState
import com.tvanime.app.ui.viewmodel.SettingsUiState

// ── Cinematic tokens ──
private val FocusRing = Color(0xFF00CED1)
private val FocusBg = Color(0x1A00CED1)
private val GlassBg = Color(0xCC1D2022)
private val GlassBorder = Color(0x333B4949)
private val ChipBg = Color(0x991D2022)

@Composable
private fun focusRingMod(focused: Boolean, modifier: Modifier, scale: Float = 1.05f) = modifier
    .focusable()
    .onFocusChanged {}
    .scale(if (focused) scale else 1f)
    .border(if (focused) BorderStroke(5.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
    .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(14.dp))

// ─────────────────────────────────────────────────────────────────
// HomeScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun HomeScreen(
    catalog: List<ContentItem>,
    onOpenExtractor: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentSelected: (ContentItem) -> Unit
) {
    val focusExtract = remember { FocusRequester() }
    val prim = MaterialTheme.colorScheme.primary
    val surfV = MaterialTheme.colorScheme.surfaceVariant

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Side nav rail
        Column(
            Modifier.fillMaxHeight().width(280.dp)
                .background(Brush.horizontalGradient(listOf(surfV.copy(alpha = 0.7f), Color.Transparent)))
                .border(0.dp, GlassBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .padding(vertical = 60.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.padding(start = 24.dp, bottom = 56.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Home, null, Modifier.size(36.dp), tint = prim)
                Text("TVAnime", style = MaterialTheme.typography.headlineMedium,
                    color = prim, fontWeight = FontWeight.Bold)
            }

            NavItem(Icons.Default.Home, "Inicio", active = true, onClick = {})
            NavItem(Icons.Default.Search, "Buscar", onClick = onOpenExtractor)
            NavItem(Icons.Default.Search, "Analizar URL", onClick = onOpenExtractor)
            NavItem(Icons.Default.Star, "Mi biblioteca", onClick = {})
            NavItem(Icons.Default.Favorite, "Favoritos", onClick = {})
            NavItem(Icons.Default.Refresh, "Historial", onClick = {})
            NavItem(Icons.Default.List, "Listas M3U", onClick = {})

            Spacer(Modifier.weight(1f))
            NavItem(Icons.Default.Settings, "Ajustes", onClick = onOpenSettings)
        }

        // Main content
        Column(
            Modifier.fillMaxSize().padding(start = 280.dp)
        ) {
            LaunchedEffect(Unit) { focusExtract.requestFocus() }

            if (catalog.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("TVAnimeApp", style = MaterialTheme.typography.headlineLarge,
                            color = prim, fontWeight = FontWeight.Bold)
                        Text("Tu app de streaming para Android TV", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CinematicButton("Analizar URL", Icons.Default.Search, focusExtract, onOpenExtractor)
                            CinematicButton("Ajustes", Icons.Default.Settings, onClick = onOpenSettings)
                        }
                    }
                }
            } else {
                // Hero section
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    Box(Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(surfV, MaterialTheme.colorScheme.background))))
                    Box(Modifier.align(Alignment.BottomStart).padding(80.dp, 60.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(color = ChipBg, shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, prim.copy(alpha = 0.3f))) {
                                    Text("Catalogo", Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        color = prim, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text("TVAnime", style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text("Explora tu contenido favorito", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text("Contenido", Modifier.padding(horizontal = 80.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(start = 80.dp, end = 40.dp)
                ) {
                    items(catalog) { item ->
                        var focused by remember { mutableStateOf(false) }
                        Card(
                            onClick = { onContentSelected(item) },
                            modifier = Modifier.width(320.dp).focusable().onFocusChanged { focused = it.isFocused }
                                .scale(if (focused) 1.08f else 1f)
                                .border(if (focused) BorderStroke(5.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
                                .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = surfV.copy(alpha = if (focused) 1f else 0.7f))
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
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
        active -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    val fg = if (focused || active) prim else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.04f else 1f)
            .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp), color = bg
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, Modifier.size(24.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun CinematicButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                            focusReq: FocusRequester? = null, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.run {
            val m = focusable().onFocusChanged { focused = it.isFocused }
                .scale(if (focused) 1.06f else 1f)
                .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(1.dp, GlassBorder), RoundedCornerShape(14.dp))
                .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(14.dp))
            if (focusReq != null) focusRequester(focusReq) else m
        },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (focused) prim else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal)
    }
}

// ─────────────────────────────────────────────────────────────────
// ExtractMediaScreen
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractMediaScreen(
    uiState: ExtractMediaUiState,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onExtract: () -> Unit,
    onPlayCandidate: (String, Map<String, String>) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onSiteSelected: (com.tvanime.app.ui.components.SiteSuggestion) -> Unit = {},
    onAutoAnalyze: () -> Unit = {},
    onToggleSuggestions: (Boolean) -> Unit = {}
) {
    val focusUrl = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusUrl.requestFocus() }

    val prim = MaterialTheme.colorScheme.primary
    val surfV = MaterialTheme.colorScheme.surfaceVariant
    val onSurf = MaterialTheme.colorScheme.onSurface

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
            Text("Explorador Web", style = MaterialTheme.typography.headlineMedium, color = onSurf)
            FocusableIconButton(onClick = { onToggleSuggestions(!uiState.showSuggestions) },
                icon = if (uiState.showSuggestions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown)
        }
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.pageUrl, onValueChange = onUrlChanged,
            modifier = Modifier.fillMaxWidth().focusRequester(focusUrl).focusable(),
            label = { Text("URL de pagina o enlace de video") },
            placeholder = { Text("pega aqui la URL completa...") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = prim) },
            trailingIcon = { if (uiState.pageUrl.isNotBlank()) IconButton(onClick = { onUrlChanged("") }) { Icon(Icons.Default.Clear, "Limpiar") } },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            var f1 by remember { mutableStateOf(false) }; var f2 by remember { mutableStateOf(false) }

            FilledTonalButton(onClick = onExtract, enabled = !uiState.isLoading && uiState.pageUrl.isNotBlank(),
                modifier = Modifier.weight(1f).focusable().onFocusChanged { f1 = it.isFocused }
                    .border(if (f1) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
                    .background(if (f1) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp)); Text("Analizar", fontWeight = if (f1) FontWeight.Bold else FontWeight.Normal)
            }
            OutlinedButton(onClick = onAutoAnalyze, enabled = !uiState.isLoading,
                modifier = Modifier.weight(1f).focusable().onFocusChanged { f2 = it.isFocused }
                    .border(if (f2) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
                    .background(if (f2) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Star, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp)); Text("Auto", fontWeight = if (f2) FontWeight.Bold else FontWeight.Normal)
            }
        }

        uiState.autoAnalyzeProgress?.let {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Text(it) }
        }

        uiState.error?.let {
            Spacer(Modifier.height(10.dp))
            Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (uiState.showSuggestions && uiState.result == null) {
            Spacer(Modifier.height(20.dp))
            Text("Sitios populares", style = MaterialTheme.typography.titleMedium, color = onSurf, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            com.tvanime.app.ui.components.WebSearchSuggestions(query = uiState.searchQuery,
                onSiteSelected = onSiteSelected, modifier = Modifier.weight(1f))
        }

        uiState.result?.let { result ->
            Spacer(Modifier.height(20.dp))
            Surface(color = surfV.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(result.title, style = MaterialTheme.typography.titleLarge, color = onSurf, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.sourceHost, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            val playable = result.candidates.filter { it.isDirect && it.format != "embed" }
            val fallback = result.candidates.filterNot { it in playable }
            val visible = if (playable.isNotEmpty()) playable else fallback

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (playable.isNotEmpty()) "Reproducibles: ${playable.size}" else "Detectados: ${fallback.size}",
                    style = MaterialTheme.typography.titleMedium, color = if (playable.isNotEmpty()) Color(0xFF34D399) else Color(0xFFFBBF24))
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
private fun CandidateCard(c: DetectedMedia, onPlay: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary
    val surf = MaterialTheme.colorScheme.surfaceVariant
    val fc = when (c.format.lowercase()) {
        "mp4" -> Color(0xFF34D399); "hls" -> Color(0xFF60A5FA)
        "webm" -> Color(0xFFA78BFA); "mkv" -> Color(0xFFFB923C)
        "audio" -> Color(0xFFFBBF24); else -> Color(0xFF94A3B8)
    }

    Card(onClick = onPlay,
        modifier = Modifier.width(360.dp).focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.08f else 1f)
            .border(if (focused) BorderStroke(5.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = surf.copy(alpha = if (focused) 1f else 0.6f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(color = fc.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(c.format.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = fc, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                c.quality?.let {
                    Surface(color = fc.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(it.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = fc)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (c.isDirect) Icons.Default.CheckCircle else Icons.Default.Star, null, Modifier.size(18.dp),
                    tint = if (c.isDirect) Color(0xFF34D399) else Color(0xFFFBBF24))
                Text(c.server.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Text(c.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Button(onClick = onPlay, Modifier.fillMaxWidth(), enabled = c.isDirect, shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (c.isDirect) MaterialTheme.colorScheme.secondary else Color.Gray)) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (c.isDirect) "Reproducir" else "No directo")
            }
        }
    }
}

@Composable
private fun FocusableIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var focused by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary
    IconButton(onClick = onClick, modifier = Modifier.focusable().onFocusChanged { focused = it.isFocused }
        .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
        .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
    ) { Icon(icon, "Volver", tint = if (focused) prim else MaterialTheme.colorScheme.onSurface) }
}

// ─────────────────────────────────────────────────────────────────
// SettingsScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    uiState: SettingsUiState, onBack: () -> Unit, onSelectDemo: () -> Unit, onSelectRemoteUrl: () -> Unit,
    onRemoteUrlChanged: (String) -> Unit, onRecurringSitesChanged: (String) -> Unit, onSave: () -> Unit
) {
    var fd by remember { mutableStateOf(false) }; var fu by remember { mutableStateOf(false) }
    var fs by remember { mutableStateOf(false) }
    val prim = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
        FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(16.dp))
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(24.dp))
        Text("Origen de playlist M3U", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSelectDemo,
                modifier = Modifier.focusable().onFocusChanged { fd = it.isFocused }
                    .border(if (fd) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
                    .background(if (fd) FocusBg else Color.Transparent, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)) { Text("Demo local") }
            OutlinedButton(onClick = onSelectRemoteUrl,
                modifier = Modifier.focusable().onFocusChanged { fu = it.isFocused }
                    .border(if (fu) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
                    .background(if (fu) FocusBg else Color.Transparent, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)) { Text("URL remota") }
        }
        if (uiState.selectedSource == PlaylistSource.REMOTE_URL) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = uiState.remoteUrl, onValueChange = onRemoteUrlChanged, Modifier.fillMaxWidth(),
                label = { Text("URL del .m3u") }, singleLine = true, shape = RoundedCornerShape(10.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Sitios recurrentes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Una URL por linea: URL | Categoria", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = uiState.recurringSitesText, onValueChange = onRecurringSitesChanged, Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(10.dp), maxLines = 8)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onSave,
            modifier = Modifier.focusable().onFocusChanged { fs = it.isFocused }
                .border(if (fs) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(14.dp))
                .background(if (fs) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp)) { Text("Guardar y sincronizar", fontWeight = if (fs) FontWeight.Bold else FontWeight.Normal) }
    }
}

// ─────────────────────────────────────────────────────────────────
// DetailScreen  &  PlayerScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun DetailScreen(contentItem: ContentItem?, isFavorite: Boolean, onPlayClick: () -> Unit, onToggleFavorite: () -> Unit, onBack: () -> Unit) {
    val prim = MaterialTheme.colorScheme.primary
    var fp by remember { mutableStateOf(false) }; var ff by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp)) {
        FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(16.dp))
        contentItem?.let {
            Text(it.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(it.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledTonalButton(onClick = onPlayClick,
                    modifier = Modifier.focusable().onFocusChanged { fp = it.isFocused }
                        .border(if (fp) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
                        .background(if (fp) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text("Reproducir", fontWeight = if (fp) FontWeight.Bold else FontWeight.Normal) }
                OutlinedButton(onClick = onToggleFavorite,
                    modifier = Modifier.focusable().onFocusChanged { ff = it.isFocused }
                        .border(if (ff) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color(0xFF47EAED)))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(16.dp))
                        .background(if (ff) FocusBg else Color.Transparent, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(20.dp)); Spacer(Modifier.size(8.dp)); Text(if (isFavorite) "Quitar" else "Favorito", fontWeight = if (ff) FontWeight.Bold else FontWeight.Normal) }
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
                layoutParams = ViewGroup.LayoutParams(-1, -1) } }, modifier = Modifier.fillMaxSize())
        androidx.compose.animation.AnimatedVisibility(visible = showControls, modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceEvenly) {
                var a by remember { mutableStateOf(false) }; var b by remember { mutableStateOf(false) }; var c by remember { mutableStateOf(false) }
                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) },
                    modifier = Modifier.size(56.dp).focusable().onFocusChanged { a = it.isFocused }
                        .border(if (a) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color.White))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
                        .background(if (a) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(10.dp)))
                { Text("\u23EA", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                IconButton(onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    modifier = Modifier.size(56.dp).focusable().onFocusChanged { b = it.isFocused }
                        .border(if (b) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color.White))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
                        .background(if (b) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(10.dp)))
                { Text(if (exoPlayer.isPlaying) "\u23F8" else "\u25B6", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(duration)) },
                    modifier = Modifier.size(56.dp).focusable().onFocusChanged { c = it.isFocused }
                        .border(if (c) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusRing, Color.White))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(10.dp))
                        .background(if (c) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(10.dp)))
                { Text("\u23E9", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
            }
        }
    }
}
