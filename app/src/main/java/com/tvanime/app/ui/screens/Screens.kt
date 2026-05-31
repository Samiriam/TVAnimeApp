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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        LaunchedEffect(Unit) { focusExtract.requestFocus() }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "TVAnime",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton(text = "Explorar", icon = Icons.Default.Search,
                    focusRequester = focusExtract, onClick = onOpenExtractor)
                TvButton(text = "Ajustes", icon = Icons.Default.Settings,
                    onClick = onOpenSettings)
            }
        }

        Spacer(Modifier.height(24.dp))

        if (catalog.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin contenido. Ve a Explorar para analizar URLs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(catalog) { item ->
                    CatalogCard(item = item, onClick = { onContentSelected(item) })
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(item: ContentItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .then(if (focused) Modifier.scale(1.03f) else Modifier)
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun TvButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focusRequester ?: FocusRequester())
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp))
            .scale(if (focused) 1.08f else 1f),
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = null, Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(text)
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

    val surf = MaterialTheme.colorScheme.surfaceVariant
    val onSurf = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
            Text("Explorador Web", style = MaterialTheme.typography.headlineMedium, color = onSurf)
            FocusableIconButton(onClick = { onToggleSuggestions(!uiState.showSuggestions) },
                icon = if (uiState.showSuggestions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.pageUrl,
            onValueChange = onUrlChanged,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusUrl)
                .focusable(),
            label = { Text("URL de pagina o enlace de video") },
            placeholder = { Text("pega aqui la URL completa...") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null, tint = primary) },
            trailingIcon = {
                if (uiState.pageUrl.isNotBlank())
                    IconButton(onClick = { onUrlChanged("") }) { Icon(Icons.Default.Clear, "Limpiar") }
            },
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
            var focus1 by remember { mutableStateOf(false) }
            var focus2 by remember { mutableStateOf(false) }

            FilledTonalButton(
                onClick = onExtract,
                enabled = !uiState.isLoading && uiState.pageUrl.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .focusable().onFocusChanged { focus1 = it.isFocused }
                    .border(2.dp, if (focus1) primary else Color.Transparent, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Analizar")
            }

            OutlinedButton(
                onClick = onAutoAnalyze,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .focusable().onFocusChanged { focus2 = it.isFocused }
                    .border(2.dp, if (focus2) primary else Color.Transparent, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Auto")
            }
        }

        uiState.autoAnalyzeProgress?.let {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        uiState.error?.let {
            Spacer(Modifier.height(10.dp))
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (uiState.showSuggestions && uiState.result == null) {
            Spacer(Modifier.height(20.dp))
            Text("Sitios populares", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            com.tvanime.app.ui.components.WebSearchSuggestions(
                query = uiState.searchQuery,
                onSiteSelected = onSiteSelected,
                modifier = Modifier.weight(1f)
            )
        }

        uiState.result?.let { result ->
            Spacer(Modifier.height(20.dp))

            Surface(color = surf, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(result.title, style = MaterialTheme.typography.titleLarge,
                        color = onSurf, fontWeight = FontWeight.Bold)
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
                Text(
                    if (playable.isNotEmpty()) "Reproducibles: ${playable.size}" else "Detectados: ${fallback.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (playable.isNotEmpty()) Color(0xFF34D399) else Color(0xFFFBBF24)
                )
                Text("${result.candidates.size} total", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp)) {
                items(visible) { candidate ->
                    CandidateResultCard(candidate = candidate,
                        onPlayClick = { onPlayCandidate(candidate.url, candidate.headers) })
                }
            }
        }
    }
}

@Composable
private fun CandidateResultCard(candidate: DetectedMedia, onPlayClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val surf = MaterialTheme.colorScheme.surfaceVariant
    val formatColor = when (candidate.format.lowercase()) {
        "mp4" -> Color(0xFF34D399)
        "hls" -> Color(0xFF60A5FA)
        "webm" -> Color(0xFFA78BFA)
        "mkv" -> Color(0xFFFB923C)
        "audio" -> Color(0xFFFBBF24)
        else -> Color(0xFF94A3B8)
    }

    Card(
        onClick = onPlayClick,
        modifier = Modifier
            .width(370.dp)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.04f else 1f)
            .border(2.dp, if (focused) primary else Color.Transparent, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surf)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Surface(color = formatColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(candidate.format.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = formatColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                candidate.quality?.let {
                    Surface(color = formatColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(it.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = formatColor, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (candidate.isDirect) Icons.Default.CheckCircle else Icons.Default.Star,
                    null, Modifier.size(18.dp), tint = if (candidate.isDirect) Color(0xFF34D399) else Color(0xFFFBBF24))
                Text(candidate.server.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }

            Text(candidate.url, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)

            Button(onClick = onPlayClick, modifier = Modifier.fillMaxWidth(),
                enabled = candidate.isDirect, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = formatColor.copy(alpha = 0.8f))) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (candidate.isDirect) "Reproducir" else "No directo")
            }
        }
    }
}

@Composable
private fun FocusableIconButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    var focused by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, if (focused) primary else Color.Transparent, RoundedCornerShape(10.dp))
    ) {
        Icon(icon, "Volver", tint = if (focused) primary else MaterialTheme.colorScheme.onSurface)
    }
}

// ─────────────────────────────────────────────────────────────────
// SettingsScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onSelectDemo: () -> Unit,
    onSelectRemoteUrl: () -> Unit,
    onRemoteUrlChanged: (String) -> Unit,
    onRecurringSitesChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    var focusDemo by remember { mutableStateOf(false) }
    var focusUrl by remember { mutableStateOf(false) }
    var focusSave by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(16.dp))
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(24.dp))

        Text("Origen de playlist M3U", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onSelectDemo,
                modifier = Modifier.focusable().onFocusChanged { focusDemo = it.isFocused }
                    .border(2.dp, if (focusDemo) primary else Color.Transparent, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)) { Text("Demo local") }

            OutlinedButton(onClick = onSelectRemoteUrl,
                modifier = Modifier.focusable().onFocusChanged { focusUrl = it.isFocused }
                    .border(2.dp, if (focusUrl) primary else Color.Transparent, RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp)) { Text("URL remota") }
        }

        if (uiState.selectedSource == PlaylistSource.REMOTE_URL) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = uiState.remoteUrl, onValueChange = onRemoteUrlChanged,
                modifier = Modifier.fillMaxWidth(), label = { Text("URL del .m3u") },
                singleLine = true, shape = RoundedCornerShape(10.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text("Sitios recurrentes", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Una URL por linea: URL | Categoria", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = uiState.recurringSitesText, onValueChange = onRecurringSitesChanged,
            modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(10.dp),
            maxLines = 8)

        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onSave,
            modifier = Modifier.focusable().onFocusChanged { focusSave = it.isFocused }
                .border(2.dp, if (focusSave) primary else Color.Transparent, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)) { Text("Guardar y sincronizar") }
    }
}

// ─────────────────────────────────────────────────────────────────
// DetailScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun DetailScreen(
    contentItem: ContentItem?,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        FocusableIconButton(onClick = onBack, icon = Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(Modifier.height(16.dp))

        contentItem?.let { item ->
            Text(item.title, style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                var focusPlay by remember { mutableStateOf(false) }
                var focusFav by remember { mutableStateOf(false) }

                FilledTonalButton(onClick = onPlayClick,
                    modifier = Modifier.focusable().onFocusChanged { focusPlay = it.isFocused }
                        .border(2.dp, if (focusPlay) primary else Color.Transparent, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Reproducir")
                }

                OutlinedButton(onClick = onToggleFavorite,
                    modifier = Modifier.focusable().onFocusChanged { focusFav = it.isFocused }
                        .border(2.dp, if (focusFav) primary else Color.Transparent, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (isFavorite) "Quitar favorito" else "Agregar favorito")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// PlayerScreen
// ─────────────────────────────────────────────────────────────────
@Composable
fun PlayerScreen(
    videoUrl: String,
    modifier: Modifier = Modifier,
    headers: Map<String, String> = emptyMap()
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(videoUrl) {
        val factory = DefaultHttpDataSource.Factory().apply {
            if (headers.isNotEmpty()) setDefaultRequestProperties(headers)
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(factory))
            .build().apply {
                setMediaItem(MediaItem.Builder().setUri(videoUrl).build())
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(showControls) {
        if (showControls) { kotlinx.coroutines.delay(4000); showControls = false }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = showControls, modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceEvenly) {
                var fp by remember { mutableStateOf(false) }; var ff by remember { mutableStateOf(false) }
                var fs by remember { mutableStateOf(false) }

                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)) },
                    modifier = Modifier.size(56.dp).focusable().onFocusChanged { fp = it.isFocused }
                        .border(2.dp, if (fp) Color.White else Color.Transparent, RoundedCornerShape(10.dp))) {
                    Text("⏪", color = Color.White, style = MaterialTheme.typography.headlineSmall) }

                IconButton(onClick = {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }, modifier = Modifier.size(56.dp).focusable().onFocusChanged { fs = it.isFocused }
                    .border(2.dp, if (fs) Color.White else Color.Transparent, RoundedCornerShape(10.dp))) {
                    Text(if (exoPlayer.isPlaying) "⏸" else "▶", color = Color.White,
                        style = MaterialTheme.typography.headlineSmall) }

                IconButton(onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(duration)) },
                    modifier = Modifier.size(56.dp).focusable().onFocusChanged { ff = it.isFocused }
                        .border(2.dp, if (ff) Color.White else Color.Transparent, RoundedCornerShape(10.dp))) {
                    Text("⏩", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
            }
        }
    }
}
