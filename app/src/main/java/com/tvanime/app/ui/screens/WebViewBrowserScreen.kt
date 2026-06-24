package com.tvanime.app.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvanime.app.ui.components.VideoCaptureOverlay
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow
import com.tvanime.app.ui.viewmodel.WebViewBrowserViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val DEFAULT_HOME_URL = "https://www.google.com"

@Composable
fun WebViewBrowserScreen(
    initialUrl: String? = null,
    onBack: () -> Unit,
    onPlayVideo: (String, Map<String, String>) -> Unit,
    viewModel: WebViewBrowserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val detectedStream by viewModel.detectedStream.collectAsState()

    val startingUrl = initialUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_HOME_URL
    var currentUrl by remember { mutableStateOf(startingUrl) }
    var urlInput by remember(startingUrl) { mutableStateOf(startingUrl) }
    var showSiteSelector by remember { mutableStateOf(false) }
    var webPermissionMessage by remember { mutableStateOf<String?>(null) }
    val webViewHolder = remember { WebViewHolder() }
    val focusableElements = remember { mutableStateListOf<FocusableElement>() }
    var selectedIndex by remember { mutableStateOf(0) }
    val webViewFocusRequester = remember { FocusRequester() }
    val searchBarFocusRequester = remember { FocusRequester() }
    var isSearchBarFocused by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder.webView?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
                wv.removeAllViews()
                wv.destroy()
            }
        }
    }

    LaunchedEffect(startingUrl) {
        viewModel.setDefaultUrl(startingUrl)
    }

    LaunchedEffect(webViewHolder.webView) {
        if (webViewHolder.webView != null) {
            webViewFocusRequester.requestFocus()
        }
    }

    BackHandler {
        when {
            isSearchBarFocused -> {
                webViewFocusRequester.requestFocus()
            }
            showSiteSelector -> {
                showSiteSelector = false
                webViewFocusRequester.requestFocus()
            }
            webViewHolder.webView?.canGoBack() == true -> {
                webViewHolder.webView?.goBack()
            }
            else -> onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    androidx.compose.ui.input.key.Key.Back -> {
                        if (isSearchBarFocused) {
                            webViewFocusRequester.requestFocus()
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        HeaderBar(
            onBack = onBack,
            onToggleSelector = {
                showSiteSelector = !showSiteSelector
                if (showSiteSelector) webViewFocusRequester.requestFocus()
            },
            selectorOpen = showSiteSelector
        )

        SearchBar(
            url = urlInput,
            onUrlChange = { urlInput = it },
            onSubmit = { query ->
                val target = if (query.startsWith("http://") || query.startsWith("https://")) {
                    query
                } else if (query.contains(".") && !query.contains(" ")) {
                    "https://$query"
                } else {
                    "https://www.google.com/search?q=${query.replace(" ", "+")}"
                }
                urlInput = target
                currentUrl = target
                viewModel.setDefaultUrl(target)
                viewModel.addToHistory(target)
                webViewFocusRequester.requestFocus()
            },
            focusRequester = searchBarFocusRequester,
            onFocusChange = { isSearchBarFocused = it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(
            visible = showSiteSelector,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SiteSelectorPanel(
                onSiteSelected = { url ->
                    urlInput = url
                    currentUrl = url
                    viewModel.setDefaultUrl(url)
                    viewModel.addToHistory(url)
                    showSiteSelector = false
                    webViewFocusRequester.requestFocus()
                },
                onClose = {
                    showSiteSelector = false
                    webViewFocusRequester.requestFocus()
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            AndroidWebView(
                holder = webViewHolder,
                url = currentUrl,
                focusRequester = webViewFocusRequester,
                canGrantWebPermissions = context.hasWebRuntimePermissions(),
                onUrlChanged = {
                    currentUrl = it
                    urlInput = it
                    viewModel.setDefaultUrl(it)
                    viewModel.addToHistory(it)
                },
                onFocusablesChanged = { elements ->
                    focusableElements.clear()
                    focusableElements.addAll(elements)
                    if (selectedIndex >= focusableElements.size) {
                        selectedIndex = focusableElements.coerceFirstVisible()
                    }
                },
                onStreamDetected = { url, format, domain ->
                    viewModel.onStreamDetected(url, format, domain, currentUrl)
                },
                onPageLoading = {},
                onTitleChanged = {},
                onPermissionRequest = { message -> webPermissionMessage = message }
            )

            if (focusableElements.isNotEmpty() && !isSearchBarFocused) {
                DpadCursorOverlay(
                    elements = focusableElements,
                    selectedIndex = selectedIndex
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = FocusCyan,
                    trackColor = Color.Transparent
                )
            }

            DpadHandler(
                elements = focusableElements,
                selectedIndex = selectedIndex,
                onSelectIndex = { selectedIndex = it },
                onActivate = { element ->
                    webViewHolder.webView?.activateElement(element)
                },
                enabled = !isSearchBarFocused && !showSiteSelector,
                onRefreshFocusables = {
                    webViewHolder.webView?.refreshFocusables { result ->
                        focusableElements.clear()
                        focusableElements.addAll(result)
                        if (selectedIndex >= focusableElements.size) {
                            selectedIndex = focusableElements.coerceFirstVisible()
                        }
                    }
                }
            )

            if (focusableElements.isEmpty() && !uiState.isLoading && !isSearchBarFocused && !showSiteSelector) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .background(Color(0xDD11191B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Usa el control: flechas para mover, OK para abrir, atras para volver",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            webPermissionMessage?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    color = Color(0xFF221A10).copy(alpha = 0.96f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f))
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.secondary)
                        Text(message, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { webPermissionMessage = null }) { Text("Cerrar") }
                    }
                }
            }
        }
    }

    VideoCaptureOverlay(
        stream = detectedStream,
        isVisible = uiState.showOverlay,
        onPlayVideo = { url, headers ->
            val mergedHeaders = viewModel.getPlaybackHeaders(url, detectedStream?.referer.orEmpty()) + headers
            onPlayVideo(url, mergedHeaders)
            viewModel.dismissOverlay()
        },
        onDismiss = { viewModel.dismissOverlay() }
    )
}

private fun SnapshotStateList<FocusableElement>.coerceFirstVisible(): Int {
    for (i in indices) if (this[i].visible) return i
    return 0
}

@Composable
private fun HeaderBar(
    onBack: () -> Unit,
    onToggleSelector: () -> Unit,
    selectorOpen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TvFocusableButton(
            onClick = onBack,
            contentDescription = "Volver",
            modifier = Modifier.size(56.dp)
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(28.dp), tint = Color.White) }

        Column(Modifier.weight(1f)) {
            Text(
                "Captura Web",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Flechas para mover el cursor, OK para abrir, atras para volver",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        TvFocusableButton(
            onClick = onToggleSelector,
            contentDescription = if (selectorOpen) "Cerrar sitios" else "Abrir sitios",
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (selectorOpen) Icons.Default.Close else Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SearchBar(
    url: String,
    onUrlChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocusChange(focused) }

    Surface(
        onClick = { focusRequester.requestFocus() },
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .border(
                if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                RoundedCornerShape(12.dp)
            ),
        color = if (focused) FocusBg else Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(FocusCyan),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.key == androidx.compose.ui.input.key.Key.Enter) {
                            onSubmit(url)
                            true
                        } else false
                    },
                decorationBox = { inner ->
                    Box {
                        if (url.isBlank()) {
                            Text(
                                "Buscar o escribir URL...",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        inner()
                    }
                }
            )
            TvFocusableButton(
                onClick = { onSubmit(url) },
                contentDescription = "Ir",
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Send, "Ir", Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun TvFocusableButton(
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .scale(if (focused) 1.08f else 1f)
            .border(
                if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                RoundedCornerShape(12.dp)
            ),
        color = if (focused) FocusBg else Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
private fun DpadCursorOverlay(elements: List<FocusableElement>, selectedIndex: Int) {
    val safe = elements.getOrNull(selectedIndex) ?: return
    if (!safe.visible) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = with(density) { safe.x.toDp().roundToPx() },
                        y = with(density) { safe.y.toDp().roundToPx() }
                    )
                }
                .size(
                    width = with(density) { safe.w.toDp() },
                    height = with(density) { safe.h.toDp() }
                )
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(listOf(FocusCyan, FocusGlow)),
                    shape = RoundedCornerShape(6.dp)
                )
                .background(FocusBg.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(FocusCyan, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = safe.label.ifBlank { safe.tag },
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DpadHandler(
    elements: List<FocusableElement>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onActivate: (FocusableElement) -> Unit,
    onRefreshFocusables: () -> Unit,
    enabled: Boolean
) {
    if (!enabled) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (elements.isEmpty()) return@onPreviewKeyEvent false
                when (event.key) {
                    androidx.compose.ui.input.key.Key.DirectionUp -> {
                        onSelectIndex(moveVertical(elements, selectedIndex, -1))
                        true
                    }
                    androidx.compose.ui.input.key.Key.DirectionDown -> {
                        onSelectIndex(moveVertical(elements, selectedIndex, 1))
                        true
                    }
                    androidx.compose.ui.input.key.Key.DirectionLeft -> {
                        onSelectIndex(moveHorizontal(elements, selectedIndex, -1))
                        true
                    }
                    androidx.compose.ui.input.key.Key.DirectionRight -> {
                        onSelectIndex(moveHorizontal(elements, selectedIndex, 1))
                        true
                    }
                    androidx.compose.ui.input.key.Key.Enter, androidx.compose.ui.input.key.Key.NumPadEnter -> {
                        elements.getOrNull(selectedIndex)?.let { onActivate(it) }
                        true
                    }
                    androidx.compose.ui.input.key.Key.MediaPlayPause, androidx.compose.ui.input.key.Key.MediaPlay -> {
                        elements.getOrNull(selectedIndex)?.let { onActivate(it) }
                        true
                    }
                    androidx.compose.ui.input.key.Key.F5 -> {
                        onRefreshFocusables()
                        true
                    }
                    else -> false
                }
            }
    )
}

private fun moveVertical(elements: List<FocusableElement>, current: Int, direction: Int): Int {
    val cur = elements.getOrNull(current) ?: return 0
    val cy = cur.y + cur.h / 2
    val candidates = elements.withIndex()
        .filter { (i, e) -> i != current && e.visible }
        .map { (i, e) ->
            val ec = e.y + e.h / 2
            val dy = ec - cy
            val sameColumn = kotlin.math.abs(e.x - cur.x) < (cur.w + e.w) / 2
            val verticalDist = if (sameColumn) kotlin.math.abs(dy) else kotlin.math.abs(dy) + (cur.w + e.w) / 2
            Triple(i, verticalDist, dy)
        }
        .filter { it.second > 0 }
        .filter { direction > 0 || it.third < 0 }
        .filter { direction < 0 || it.third > 0 }
        .sortedBy { it.second }
    return candidates.firstOrNull()?.first ?: current
}

private fun moveHorizontal(elements: List<FocusableElement>, current: Int, direction: Int): Int {
    val cur = elements.getOrNull(current) ?: return 0
    val cy = cur.y + cur.h / 2
    val candidates = elements.withIndex()
        .filter { (i, e) ->
            i != current && e.visible && kotlin.math.abs((e.y + e.h / 2) - cy) < cur.h
        }
        .map { (i, e) ->
            val dx = (e.x + e.w / 2) - (cur.x + cur.w / 2)
            Triple(i, kotlin.math.abs(dx), dx)
        }
        .filter { it.second > 0 }
        .filter { direction > 0 || it.third < 0 }
        .filter { direction < 0 || it.third > 0 }
        .sortedBy { it.second }
    return candidates.firstOrNull()?.first ?: current
}

@Composable
private fun SiteSelectorPanel(onSiteSelected: (String) -> Unit, onClose: () -> Unit) {
    val sites = listOf(
        SiteGroup("Fuentes de prueba", listOf(
            SiteItem("Google", "https://www.google.com", "BUSCAR"),
            SiteItem("Archive.org", "https://archive.org", "WEB"),
            SiteItem("Video test HLS", "https://test-streams.mux.dev", "HLS"),
            SiteItem("Wikipedia", "https://www.wikipedia.org", "INFO"),
        )),
        SiteGroup("Entrada manual", listOf(
            SiteItem("Escribir arriba", "https://", "URL"),
            SiteItem("Buscar en Google", "https://www.google.com/search?q=public+domain+video", "BUSCAR"),
        ))
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sitios", color = FocusCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            TvFocusableButton(onClick = onClose, contentDescription = "Cerrar sitios", modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, "Cerrar", Modifier.size(20.dp), tint = Color.White)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sites.forEach { group ->
                item {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = FocusCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                items(group.sites) { site ->
                    SiteCard(site = site, onClick = { onSiteSelected(site.url) })
                }
            }
        }
    }
}

@Composable
private fun SiteCard(site: SiteItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (focused) 1.03f else 1f)
            .border(
                if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                RoundedCornerShape(14.dp)
            )
            .focusable(),
        color = if (focused) FocusBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = FocusCyan.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                Text(text = site.icon, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge, color = FocusCyan, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = if (focused) FocusCyan else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

data class FocusableElement(
    val index: Int,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val tag: String,
    val label: String,
    val visible: Boolean
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndroidWebView(
    holder: WebViewHolder,
    url: String,
    focusRequester: FocusRequester,
    canGrantWebPermissions: Boolean,
    onUrlChanged: (String) -> Unit,
    onFocusablesChanged: (List<FocusableElement>) -> Unit,
    onStreamDetected: (String, String, String) -> Unit,
    onPageLoading: (Boolean) -> Unit,
    onTitleChanged: (String?) -> Unit,
    onPermissionRequest: (String) -> Unit
) {
    var webViewError by remember { mutableStateOf<String?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(interactionSource = interaction)
            .border(
                width = if (focused) 4.dp else 2.dp,
                brush = if (focused) Brush.linearGradient(listOf(FocusCyan, FocusGlow))
                else androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (focused) FocusBg.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        factory = { ctx ->
            TvDpadWebView(ctx).also { wv ->
                holder.webView = wv
                wv.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.setGeolocationEnabled(false)
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.userAgentString = com.tvanime.app.data.capture.WebViewSessionManager.USER_AGENT
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    isFocusable = true
                    isFocusableInTouchMode = true

                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                            val lower = reqUrl.lowercase()

                            if (lower.contains(".m3u8") || lower.contains(".mp4") ||
                                lower.contains(".webm") || lower.contains(".ts") ||
                                lower.contains(".mkv") || lower.contains("manifest") ||
                                lower.contains("segment") || lower.contains("playlist.m3u8") ||
                                lower.contains("chunk")) {

                                val format = when {
                                    lower.contains(".m3u8") -> "HLS"
                                    lower.contains(".mp4") -> "MP4"
                                    lower.contains(".webm") -> "WEBM"
                                    lower.contains(".mkv") -> "MKV"
                                    lower.contains(".ts") -> "TS"
                                    else -> "VIDEO"
                                }

                                post { onStreamDetected(reqUrl, format, request.url.host ?: "") }
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            webViewError = null
                            url?.let { post { onUrlChanged(it) } }
                            post { onPageLoading(true) }
                        }

                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            post { onPageLoading(false) }
                            view?.title?.let { post { onTitleChanged(it) } }
                            view?.evaluateJavascript(INJECTED_JS, null)
                            view?.postDelayed({
                                view.evaluateJavascript(FOCUSABLES_JS) { rawJson ->
                                    val parsed = parseFocusables(rawJson)
                                    post { onFocusablesChanged(parsed) }
                                }
                            }, 700)
                            view?.postDelayed({
                                view.evaluateJavascript(FOCUSABLES_JS) { rawJson ->
                                    val parsed = parseFocusables(rawJson)
                                    post { onFocusablesChanged(parsed) }
                                }
                            }, 1800)
                        }

                        @Suppress("DEPRECATION")
                        override fun onReceivedError(
                            view: android.webkit.WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            webViewError = description ?: "Error de carga"
                        }

                        override fun onReceivedHttpError(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame == true) {
                                webViewError = "Error HTTP: ${errorResponse?.statusCode}"
                            }
                        }

                        override fun onRenderProcessGone(
                            view: android.webkit.WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            webViewError = "El motor WebView se reinicio"
                            view?.apply {
                                stopLoading()
                                loadUrl("about:blank")
                            }
                            return true
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest?) {
                            val resources = request?.resources ?: return
                            if (canGrantWebPermissions) {
                                request.grant(resources)
                                post { onPermissionRequest("Permiso concedido a la pagina actual") }
                            } else {
                                request.deny()
                                post { onPermissionRequest("Permiso bloqueado: autoriza camara/microfono en la pantalla inicial") }
                            }
                        }
                    }

                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onVideoDetected(url: String) {
                            val format = detectFormat(url)
                            post { onStreamDetected(url, format, "") }
                        }
                    }, "AndroidCapture")
                }
            }
        },
        update = { webView ->
            val current = webView.url ?: ""
            if (current != url && url.isNotBlank() && !url.startsWith("about:blank")) {
                webView.loadUrl(url)
            }
        }
    )
}

private class WebViewHolder {
    var webView: TvDpadWebView? = null
}

private class TvDpadWebView(context: android.content.Context) : WebView(context) {
    fun refreshFocusables(callback: (List<FocusableElement>) -> Unit) {
        evaluateJavascript(FOCUSABLES_JS) { raw ->
            callback(parseFocusables(raw))
        }
    }

    fun activateElement(element: FocusableElement) {
        val js = """
            (function() {
                var idx = ${element.index};
                var sel = window.__tvFocusables ? window.__tvFocusables[idx] : null;
                if (!sel) { window.scrollBy(0, 400); return; }
                sel.scrollIntoView({block: 'center', behavior: 'instant'});
                setTimeout(function() {
                    if (sel.tagName === 'VIDEO') { sel.play(); }
                    else if (sel.tagName === 'A' && sel.href) { window.location.href = sel.href; }
                    else if (sel.click) { sel.click(); }
                }, 60);
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
        postDelayed({
            refreshFocusables { /* updated externally */ }
        }, 500)
    }
}

private fun parseFocusables(raw: String?): List<FocusableElement> {
    if (raw.isNullOrBlank() || raw == "null") return emptyList()
    val cleaned = raw.trim().removePrefix("\"").removeSuffix("\"")
        .replace("\\\"", "\"").replace("\\\\", "\\")
    return try {
        val arr = JSONArray(cleaned)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    FocusableElement(
                        index = o.optInt("i"),
                        x = o.optDouble("x", 0.0).toFloat(),
                        y = o.optDouble("y", 0.0).toFloat(),
                        w = o.optDouble("w", 0.0).toFloat(),
                        h = o.optDouble("h", 0.0).toFloat(),
                        tag = o.optString("t", ""),
                        label = o.optString("l", ""),
                        visible = o.optBoolean("v", false)
                    )
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun android.content.Context.hasWebRuntimePermissions(): Boolean {
    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun detectFormat(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.contains(".m3u8") -> "HLS"
        lower.contains(".mp4") -> "MP4"
        lower.contains(".webm") -> "WEBM"
        lower.contains(".mkv") -> "MKV"
        lower.contains(".ts") -> "TS"
        else -> "VIDEO"
    }
}

private const val FOCUSABLES_JS = """
(function() {
    try {
        var vw = window.innerWidth;
        var vh = window.innerHeight;
        var sels = 'a, button, input, textarea, select, [role=button], [tabindex], video, iframe';
        var nodes = Array.from(document.querySelectorAll(sels));
        var result = [];
        var i = 0;
        nodes.forEach(function(n) {
            try {
                var r = n.getBoundingClientRect();
                var visible = r.width > 4 && r.height > 4 && r.bottom > 0 && r.top < vh && r.right > 0 && r.left < vw;
                if (!visible) return;
                var text = (n.innerText || n.value || n.placeholder || n.getAttribute('aria-label') || n.title || '').toString().trim().slice(0, 60);
                result.push({i: i, x: r.left, y: r.top, w: r.width, h: r.height, t: n.tagName.toLowerCase(), l: text, v: true});
                n.setAttribute('data-tv-focusable', String(i));
                i++;
            } catch (e) {}
        });
        window.__tvFocusables = nodes.filter(function(n) {
            try {
                var r = n.getBoundingClientRect();
                return r.width > 4 && r.height > 4 && r.bottom > 0 && r.top < vh && r.right > 0 && r.left < vw;
            } catch (e) { return false; }
        });
        return JSON.stringify(result);
    } catch (e) {
        return '[]';
    }
})();
"""

private const val INJECTED_JS = """
    (function() {
        function setupVideoListeners(video) {
            video.addEventListener('play', function() {
                var src = video.src || video.currentSrc || '';
                if (src && !src.startsWith('blob:') && src.length > 10) {
                    window.AndroidCapture && window.AndroidCapture.onVideoDetected(src);
                }
            });
            video.addEventListener('loadedmetadata', function() {
                var src = video.src || video.currentSrc || '';
                if (src && !src.startsWith('blob:') && src.length > 10) {
                    window.AndroidCapture && window.AndroidCapture.onVideoDetected(src);
                }
            });
        }
        var initInterval = setInterval(function() {
            if (document.readyState === 'complete') {
                clearInterval(initInterval);
                document.querySelectorAll('video').forEach(setupVideoListeners);
                document.querySelectorAll('a[href*=".m3u8"], a[href*=".mp4"], a[href*=".webm"]').forEach(function(link) {
                    link.addEventListener('click', function() {
                        var href = link.href;
                        if (href && href.length > 10) {
                            window.AndroidCapture && window.AndroidCapture.onVideoDetected(href);
                        }
                    });
                });
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(m) {
                        m.addedNodes.forEach(function(node) {
                            if (node.nodeName === 'VIDEO') setupVideoListeners(node);
                            if (node.querySelectorAll) node.querySelectorAll('video').forEach(setupVideoListeners);
                        });
                    });
                });
                observer.observe(document.body, { childList: true, subtree: true });
            }
        }, 500);
    })();
"""

private data class SiteGroup(val name: String, val sites: List<SiteItem>)
private data class SiteItem(val name: String, val url: String, val icon: String)
