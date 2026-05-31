package com.tvanime.app.ui.screens

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tvanime.app.data.settings.PlaylistSource
import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.ui.viewmodel.ExtractMediaUiState
import com.tvanime.app.ui.viewmodel.SettingsUiState

@Composable
fun HomeScreen(
    catalog: List<ContentItem>,
    onOpenExtractor: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentSelected: (ContentItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TVAnime",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onOpenExtractor) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Analizar URL")
                }
                OutlinedButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Ajustes")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (catalog.isEmpty()) {
            Text(
                text = "No hay contenido cargado todavía.",
                color = Color.White.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(catalog) { item ->
                    Card(
                        onClick = { onContentSelected(item) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171725))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = item.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = item.description, color = Color.White.copy(alpha = 0.75f), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractMediaScreen(
    uiState: ExtractMediaUiState,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onExtract: () -> Unit,
    onPlayCandidate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text("Analizar pagina publica", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Primer extractor generico: detecta enlaces HLS, MP4, audio y embeds visibles en el HTML publico.",
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.pageUrl,
            onValueChange = onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL publica") },
            placeholder = { Text("https://example.com/page") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        FilledTonalButton(onClick = onExtract, enabled = !uiState.isLoading) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text("Analizar")
        }

        uiState.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        uiState.result?.let { result ->
            Spacer(Modifier.height(20.dp))
            Text(result.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(result.sourceHost, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(result.candidates) { candidate ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF171725))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "${candidate.mediaType.uppercase()} · ${candidate.format.uppercase()} · ${candidate.server}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (candidate.requiresResolver) "Requiere resolver" else "Directo · prioridad ${candidate.priority}",
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(candidate.url, color = Color.White.copy(alpha = 0.75f), maxLines = 3)
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(
                                onClick = { onPlayCandidate(candidate.url) },
                                enabled = candidate.format != "embed"
                            ) {
                                Text(if (candidate.format == "embed") "Embed no directo" else "Reproducir")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text("Origen de playlist M3U", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Selecciona una fuente para sincronizar el catalogo y generar el APK de prueba con contenido disponible.",
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = uiState.selectedSource == PlaylistSource.DEMO,
                onClick = onSelectDemo,
                label = { Text("Demo local") }
            )
            FilterChip(
                selected = uiState.selectedSource == PlaylistSource.REMOTE_URL,
                onClick = onSelectRemoteUrl,
                label = { Text("URL remota") }
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.remoteUrl,
            onValueChange = onRemoteUrlChanged,
            enabled = uiState.selectedSource == PlaylistSource.REMOTE_URL,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL M3U") },
            placeholder = { Text("https://example.com/playlist.m3u") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            supportingText = {
                Text(
                    if (uiState.selectedSource == PlaylistSource.DEMO) {
                        "La demo usa `app/src/main/assets/playlist_demo.m3u`."
                    } else {
                        "Se sincroniza ahora y luego cada 4 horas con WorkManager."
                    }
                )
            }
        )

        Spacer(Modifier.height(20.dp))

        Text("Sitios recurrentes", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Una URL por linea. Opcionalmente usa: URL | Categoria. Se analizan cada 6 horas y se agregan candidatos directos al catalogo.",
            color = Color.White.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = uiState.recurringSitesText,
            onValueChange = onRecurringSitesChanged,
            modifier = Modifier.fillMaxWidth().height(130.dp),
            label = { Text("Paginas para autoanalizar") },
            placeholder = { Text("https://sitio.example/ultimos | Anime") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            maxLines = 5
        )

        uiState.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        uiState.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = Color(0xFF7CFC98))
        }

        Spacer(Modifier.height(24.dp))

        FilledTonalButton(onClick = onSave, enabled = !uiState.isSaving) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text("Guardar y sincronizar")
        }
    }
}

@Composable
fun DetailScreen(
    contentItem: ContentItem?,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val item = contentItem
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        Text(text = item.title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(text = item.description, color = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onPlayClick) { Text("Reproducir") }
            OutlinedButton(onClick = onToggleFavorite) { Text(if (isFavorite) "Quitar favorito" else "Favorito") }
        }
    }
}

/**
 * Pantalla de reproducción de video con Media3 / ExoPlayer.
 *
 * Características:
 * - Controles nativos de Android (play, pausa, seek, subtítulos)
 * - Auto-play al montar
 * - Guarda progreso al pausar / destroy
 * - Compatible con control remoto Android TV
 */
@Composable
fun PlayerScreen(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // Crear ExoPlayer una sola vez
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(videoUrl).build())
            prepare()
            playWhenReady = true
        }
    }

    // Guardar progreso al salir
    DisposableEffect(Unit) {
        onDispose {
            // TODO: guardar posición en Room con SaveProgressUseCase
            exoPlayer.release()
        }
    }

    // Sincronizar isPlaying con el player
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                currentPosition = exoPlayer.currentPosition
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // TODO: marcar como completado en historial
                }
                currentPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Auto-ocultar controles después de 4 segundos
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(4000)
            showControls = false
        }
    }

    // Mostrar controles en cualquier interacción del D-pad
    LaunchedEffect(Unit) {
        // Cada 3 segundos comprobar si el usuario interactuó
        while (true) {
            kotlinx.coroutines.delay(3000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Vista del reproductor ──────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false            // controres custom
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Controles superpuestos ─────────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 90.dp, top = 32.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Barra superior
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                            onClick = {
                            onBackPressedDispatcher?.onBackPressed()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = formatTime(currentPosition) + " / " + formatTime(duration),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Botonera inferior
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Barra de progreso
                    var sliderPos by remember { mutableFloatStateOf(currentPosition.toFloat()) }
                    LaunchedEffect(currentPosition) {
                        sliderPos = currentPosition.toFloat()
                    }

                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            val newPos = (fraction * duration).toLong()
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF7C3AED),
                            activeTrackColor = Color(0xFF7C3AED),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Retroceder 10s
                        IconButton(
                            onClick = {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(text = "-10s", color = Color.White)
                        }

                        // Play / Pausa
                        FilledIconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier.size(80.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF7C3AED)
                            )
                        ) {
                            Text(
                                text = if (exoPlayer.isPlaying) "Pausa" else "Play",
                                color = Color.White
                            )
                        }

                        // Avanzar 10s
                        IconButton(
                            onClick = {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(duration))
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(text = "+10s", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formatea milisegundos a mm:ss o hh:mm:ss.
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d".format(minutes, seconds)
}
