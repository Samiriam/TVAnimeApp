package com.tvanime.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
import com.tvanime.app.ui.theme.*
import com.tvanime.app.ui.viewmodel.CrawlerUiState
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
    onOpenBrowser: () -> Unit,
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
                NavItem(icon = Icons.Default.Search, label = "Buscar", onClick = onOpenBrowser)
                NavItem(icon = Icons.Default.List, label = "Navegador Web", onClick = onOpenBrowser)
                NavItem(icon = Icons.Default.Star, label = "Mi biblioteca", onClick = {})
                NavItem(icon = Icons.Default.Favorite, label = "Favoritos", onClick = {})
                NavItem(icon = Icons.Default.Refresh, label = "Historial", onClick = {})

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
                                TvButton("Navegador Web", Icons.Default.List, onClick = onOpenBrowser)
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
fun SettingsScreen(
    uiState: SettingsUiState, crawlerState: CrawlerUiState, onBack: () -> Unit,
    onSelectDemo: () -> Unit, onSelectRemoteUrl: () -> Unit,
    onRemoteUrlChanged: (String) -> Unit, onSave: () -> Unit,
    onToggleCategory: (String, Boolean) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp).verticalScroll(rememberScrollState())) {
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
        Spacer(Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(Modifier.height(32.dp))
        Text("Categorias para crawler", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Selecciona las categorias que quieres crawler automaticamente cada 6 horas.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        crawlerState.categories.forEach { cat ->
            val catConfig = com.tvanime.app.domain.model.CategoryConfig.DEFAULT.find { it.category == cat.category }
            var focused by remember { mutableStateOf(false) }
            val label = catConfig?.label ?: cat.category
            val icon = when (cat.category) {
                "anime" -> Icons.Default.Star
                "movies" -> Icons.Default.Star
                "series" -> Icons.Default.Star
                "documentaries" -> Icons.Default.Star
                else -> Icons.Default.Star
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).focusable().onFocusChanged { focused = it.isFocused },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, Modifier.size(24.dp), tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal)
                }
                Switch(
                    checked = cat.enabled,
                    onCheckedChange = { onToggleCategory(cat.category, it) },
                    modifier = Modifier.scale(if (focused) 1.2f else 1f)
                )
            }
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
    val context = androidx.compose.ui.platform.LocalContext.current
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