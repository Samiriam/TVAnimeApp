package com.tvanime.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
    .scale(if (focused) 1.06f else 1f)
    .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
    .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(12.dp))

@Composable
fun HomeScreen(
    catalog: List<ContentItem>,
    permissionsGranted: Boolean = true,
    onOpenBrowser: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentSelected: (ContentItem) -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF143A3B), MaterialTheme.colorScheme.background),
                    radius = 1200f
                )
            )
            .padding(42.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TVAnime Capture", style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("Abre una pagina publica, detecta enlaces reproducibles y envia el stream al reproductor TV.",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PermissionStatusPill(permissionsGranted)
            }

            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Surface(
                    modifier = Modifier.weight(1.25f).fillMaxHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF11191B).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, FocusCyan.copy(alpha = 0.22f))
                ) {
                    Column(Modifier.padding(34.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        Surface(color = FocusCyan.copy(alpha = 0.12f), shape = RoundedCornerShape(999.dp)) {
                            Text("Flujo principal", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = FocusCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("Capturar video desde una URL", style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Usa el navegador integrado para entrar al sitio, reproducir el contenido y dejar que la app detecte HLS, MP4 u otros recursos compatibles. Cuando aparezca el aviso, pulsa Reproducir en TV.",
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TvButton("Abrir navegador", Icons.Default.Search, onClick = onOpenBrowser)
                            TvButton("Permisos y fuentes", Icons.Default.Settings, onClick = onOpenSettings)
                        }
                    }
                }

                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeStepCard("1", "Permisos", if (permissionsGranted) "Camara, microfono y notificaciones autorizadas." else "Autoriza permisos para WebView y avisos de captura.")
                    HomeStepCard("2", "Navegacion", "Escribe una URL o elige una fuente publica desde el navegador.")
                    HomeStepCard("3", "Reproduccion", "Selecciona el stream detectado y Media3 lo abre en pantalla completa.")
                }
            }

            if (catalog.isNotEmpty()) {
                Text("Catalogo M3U disponible", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(catalog.take(12)) { item ->
                        CatalogMiniCard(item = item, onClick = { onContentSelected(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionGateScreen(
    onRequestPermissions: () -> Unit,
    onContinueWithoutPermissions: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.76f),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF11191B),
            border = BorderStroke(1.dp, FocusCyan.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(40.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Icon(Icons.Default.Lock, null, Modifier.size(58.dp), tint = FocusCyan)
                Text("Permisos requeridos", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text("TVAnime necesita permisos runtime para responder a solicitudes del navegador integrado, reproducir sitios que pidan camara o microfono, y mostrar avisos de captura cuando Android lo requiera.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvButton("Solicitar permisos", Icons.Default.Lock, onClick = onRequestPermissions)
                    TvButton("Continuar limitado", Icons.Default.PlayArrow, onClick = onContinueWithoutPermissions)
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusPill(granted: Boolean) {
    Surface(color = if (granted) Color(0x2234D399) else Color(0x33FFB59C), shape = RoundedCornerShape(999.dp)) {
        Text(
            text = if (granted) "Permisos activos" else "Permisos pendientes",
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (granted) Color(0xFF34D399) else MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HomeStepCard(number: String, title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF172023).copy(alpha = 0.86f)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(color = FocusCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp)) {
                Text(number, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = FocusCyan,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CatalogMiniCard(item: ContentItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.width(260.dp)
            .scale(if (focused) 1.06f else 1f)
            .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, maxLines = 2)
            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
                    active: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val prim = MaterialTheme.colorScheme.primary
    val bg = when {
        focused -> FocusBg
        active -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    val fg = if (focused || active) prim else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth()
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val prim = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
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
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val prim = MaterialTheme.colorScheme.primary
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
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
            if (it.videoUrl.isBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Este item aun no tiene stream reproducible. Abre la fuente original desde Captura Web para encontrarlo.",
                            color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(20.dp))
                TvButton("Abrir en navegador", Icons.Default.Search) { onBack() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvButton("Reproducir", Icons.Default.PlayArrow) { onPlayClick() }
                    TvButton(if (isFavorite) "Quitar favorito" else "Agregar a favoritos", Icons.Default.Favorite) { onToggleFavorite() }
                }
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
    var playbackError by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember(videoUrl, headers) {
        val factory = DefaultHttpDataSource.Factory().apply { if (headers.isNotEmpty()) setDefaultRequestProperties(headers) }
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(factory))
            .build().apply { setMediaItem(MediaItem.Builder().setUri(videoUrl).build()); prepare(); playWhenReady = true }
    }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    DisposableEffect(exoPlayer) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(p: Boolean) { isPlaying = p }
            override fun onPlaybackStateChanged(s: Int) { currentPosition = exoPlayer.currentPosition; duration = exoPlayer.duration }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { playbackError = error.message ?: "No se pudo reproducir el stream" }
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
                PlayerControlButton(Icons.Default.Refresh, "Reiniciar", { exoPlayer.seekTo(0) })
                PlayerControlButton(if (isPlaying) Icons.Default.Build else Icons.Default.PlayArrow, if (isPlaying) "Pausar" else "Reproducir", { if (isPlaying) exoPlayer.pause() else exoPlayer.play() })
                PlayerControlButton(Icons.Default.Send, "Avanzar al final", { exoPlayer.seekTo(duration) })
            }
        }

        playbackError?.let { error ->
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                color = Color(0xDD1A1D21),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Error de reproducción\n$error",
                    modifier = Modifier.padding(24.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PlayerControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    IconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.size(60.dp)
            .border(if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow))) else BorderStroke(0.dp, Color.Transparent), RoundedCornerShape(12.dp))
            .background(if (focused) FocusHighlight else Color.Transparent, RoundedCornerShape(12.dp))
    ) { Icon(icon, contentDescription, Modifier.size(32.dp), tint = Color.White) }
}
