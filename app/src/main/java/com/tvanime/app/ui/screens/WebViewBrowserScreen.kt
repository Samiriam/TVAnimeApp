package com.tvanime.app.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvanime.app.data.capture.WebViewSessionManager
import com.tvanime.app.ui.components.VideoCaptureOverlay
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow
import com.tvanime.app.ui.viewmodel.WebViewBrowserViewModel
import org.json.JSONArray
import org.json.JSONObject

private const val DEFAULT_HOME_URL = "https://www.google.com"
private const val PREFS_NAME = "webcast_prefs"
private const val KEY_BOOKMARKS = "bookmarks_json"
private const val KEY_DRAWER_VIDEOS = "drawer_videos_open"
private const val KEY_DRAWER_BOOKMARKS = "drawer_bookmarks_open"

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
    var webPermissionMessage by remember { mutableStateOf<String?>(null) }
    val webViewHolder = remember { WebViewHolder() }
    val webViewFocusRequester = remember { FocusRequester() }
    val searchBarFocusRequester = remember { FocusRequester() }
    var isSearchBarFocused by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var bookmarks by remember { mutableStateOf(loadBookmarks(prefs)) }
    var videosFound by remember { mutableStateOf(listOf<DetectedVideo>()) }
    var showVideoDrawer by remember { mutableStateOf(prefs.getBoolean(KEY_DRAWER_VIDEOS, true)) }
    var showBookmarkDrawer by remember { mutableStateOf(prefs.getBoolean(KEY_DRAWER_BOOKMARKS, false)) }

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

    fun persistDrawers() {
        prefs.edit().apply {
            putBoolean(KEY_DRAWER_VIDEOS, showVideoDrawer)
            putBoolean(KEY_DRAWER_BOOKMARKS, showBookmarkDrawer)
            apply()
        }
    }

    BackHandler {
        when {
            isSearchBarFocused -> webViewFocusRequester.requestFocus()
            showVideoDrawer -> { showVideoDrawer = false; persistDrawers() }
            showBookmarkDrawer -> { showBookmarkDrawer = false; persistDrawers() }
            webViewHolder.webView?.canGoBack() == true -> webViewHolder.webView?.goBack()
            else -> onBack()
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showBookmarkDrawer) {
            BookmarksDrawer(
                bookmarks = bookmarks,
                onSelect = { url ->
                    urlInput = url
                    currentUrl = url
                    viewModel.setDefaultUrl(url)
                    viewModel.addToHistory(url)
                    showBookmarkDrawer = false
                    persistDrawers()
                    webViewFocusRequester.requestFocus()
                },
                onDelete = { url ->
                    bookmarks = bookmarks.filterNot { it == url }
                    saveBookmarks(prefs, bookmarks)
                },
                onClose = {
                    showBookmarkDrawer = false
                    persistDrawers()
                    webViewFocusRequester.requestFocus()
                },
                onPlayVideo = { url ->
                    val headers = mapOf("Referer" to currentUrl)
                    onPlayVideo(url, headers)
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (event.key == androidx.compose.ui.input.key.Key.Back && isSearchBarFocused) {
                        webViewFocusRequester.requestFocus()
                        true
                    } else false
                }
        ) {
            HeaderBar(
                onBack = onBack,
                onToggleVideoDrawer = {
                    showVideoDrawer = !showVideoDrawer
                    if (showVideoDrawer) showBookmarkDrawer = false
                    persistDrawers()
                },
                onToggleBookmarkDrawer = {
                    showBookmarkDrawer = !showBookmarkDrawer
                    if (showBookmarkDrawer) showVideoDrawer = false
                    persistDrawers()
                },
                onAddBookmark = {
                    if (currentUrl.isNotBlank() && currentUrl !in bookmarks) {
                        bookmarks = bookmarks + currentUrl
                        saveBookmarks(prefs, bookmarks)
                    }
                },
                videoDrawerOpen = showVideoDrawer,
                bookmarkDrawerOpen = showBookmarkDrawer
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
                    videosFound = emptyList()
                    webViewFocusRequester.requestFocus()
                },
                focusRequester = searchBarFocusRequester,
                onFocusChange = { isSearchBarFocused = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxSize()
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
                        videosFound = emptyList()
                    },
                    onVideoDetected = { url, format ->
                        val video = DetectedVideo(
                            url = url,
                            format = format,
                            referer = currentUrl
                        )
                        if (videosFound.none { it.url == url }) {
                            videosFound = videosFound + video
                        }
                    },
                    onPageLoading = {},
                    onTitleChanged = {},
                    onPermissionRequest = { message -> webPermissionMessage = message }
                )

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = FocusCyan,
                        trackColor = Color.Transparent
                    )
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

        if (showVideoDrawer) {
            VideosDrawer(
                videos = videosFound,
                onPlay = { video ->
                    val headers = mapOf(
                        "Referer" to video.referer,
                        "User-Agent" to WebViewSessionManager.USER_AGENT
                    )
                    onPlayVideo(video.url, headers)
                },
                onAddBookmark = { video ->
                    if (video.url !in bookmarks) {
                        bookmarks = bookmarks + video.url
                        saveBookmarks(prefs, bookmarks)
                    }
                },
                onClose = {
                    showVideoDrawer = false
                    persistDrawers()
                    webViewFocusRequester.requestFocus()
                }
            )
        }
    }
}

private fun loadBookmarks(prefs: SharedPreferences): List<String> {
    val json = prefs.getString(KEY_BOOKMARKS, null) ?: return defaultBookmarks()
    return try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    } catch (e: Exception) {
        defaultBookmarks()
    }
}

private fun saveBookmarks(prefs: SharedPreferences, list: List<String>) {
    val arr = JSONArray()
    list.forEach { arr.put(it) }
    prefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
}

private fun defaultBookmarks(): List<String> = listOf(
    "https://www.google.com",
    "https://archive.org",
    "https://test-streams.mux.dev"
)

@Composable
private fun HeaderBar(
    onBack: () -> Unit,
    onToggleVideoDrawer: () -> Unit,
    onToggleBookmarkDrawer: () -> Unit,
    onAddBookmark: () -> Unit,
    videoDrawerOpen: Boolean,
    bookmarkDrawerOpen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TvFocusableButton(
            onClick = onBack,
            contentDescription = "Volver",
            modifier = Modifier.size(48.dp)
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(24.dp), tint = Color.White) }

        TvFocusableButton(
            onClick = onToggleVideoDrawer,
            contentDescription = "Videos",
            modifier = Modifier.size(48.dp),
            highlighted = videoDrawerOpen
        ) {
            Icon(Icons.Default.PlayArrow, "Videos", Modifier.size(24.dp), tint = if (videoDrawerOpen) Color.Black else Color.White)
        }

        TvFocusableButton(
            onClick = onToggleBookmarkDrawer,
            contentDescription = "Bookmarks",
            modifier = Modifier.size(48.dp),
            highlighted = bookmarkDrawerOpen
        ) {
            Icon(Icons.Default.Star, "Bookmarks", Modifier.size(24.dp), tint = if (bookmarkDrawerOpen) Color.Black else Color.White)
        }

        TvFocusableButton(
            onClick = onAddBookmark,
            contentDescription = "Agregar bookmark",
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Add, "Agregar", Modifier.size(24.dp), tint = Color.White)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                singleLine = true,
                cursorBrush = SolidColor(FocusCyan),
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
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || highlighted
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .scale(if (focused) 1.08f else 1f)
            .border(
                if (active) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                RoundedCornerShape(12.dp)
            ),
        color = if (active) FocusBg else Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
private fun BookmarksDrawer(
    bookmarks: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("Bookmarks", color = FocusCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            TvFocusableButton(onClick = onClose, contentDescription = "Cerrar", modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Cerrar", Modifier.size(18.dp), tint = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(bookmarks) { url ->
                val isVideo = url.contains(".m3u8") || url.contains(".mp4") || url.contains(".webm")
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Surface(
                    onClick = { if (isVideo) onPlayVideo(url) else onSelect(url) },
                    interactionSource = interaction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                            else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            RoundedCornerShape(10.dp)
                        )
                        .focusable(),
                    color = if (focused) FocusBg else Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isVideo) Icons.Default.PlayArrow else Icons.Default.Star,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(url, color = Color.White, style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (isVideo) Text("Video", color = FocusCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideosDrawer(
    videos: List<DetectedVideo>,
    onPlay: (DetectedVideo) -> Unit,
    onAddBookmark: (DetectedVideo) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("Videos (${videos.size})", color = FocusCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            TvFocusableButton(onClick = onClose, contentDescription = "Cerrar", modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Cerrar", Modifier.size(18.dp), tint = Color.White)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (videos.isEmpty()) {
            Text(
                "Navega una pagina con videos. Apareceran aqui al detectarlos en las requests HTTP.",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(videos) { video ->
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Surface(
                        onClick = { onPlay(video) },
                        interactionSource = interaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                if (focused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                RoundedCornerShape(10.dp)
                            )
                            .focusable(),
                        color = if (focused) FocusBg else Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = FocusCyan, modifier = Modifier.size(22.dp))
                            Column(Modifier.weight(1f)) {
                                Text(video.format, color = FocusCyan, style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold)
                                Text(video.url, color = Color.White, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class DetectedVideo(val url: String, val format: String, val referer: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndroidWebView(
    holder: WebViewHolder,
    url: String,
    focusRequester: FocusRequester,
    canGrantWebPermissions: Boolean,
    onUrlChanged: (String) -> Unit,
    onVideoDetected: (String, String) -> Unit,
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
                else SolidColor(Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                if (focused) FocusBg.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        factory = { ctx ->
            NativeDpadWebView(ctx).also { wv ->
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
                    settings.userAgentString = WebViewSessionManager.USER_AGENT
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
                            if (isVideoRequest(lower)) {
                                val format = detectFormat(lower)
                                post { onVideoDetected(reqUrl, format) }
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
    var webView: NativeDpadWebView? = null
}

private class NativeDpadWebView(context: android.content.Context) : WebView(context) {
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                evaluateJavascript(
                    "(function(){var a=document.activeElement;if(!a||a===document.body){return false;}if(a.tagName==='VIDEO'){a.play();return true;}if(a.tagName==='A'&&a.href){window.location.href=a.href;return true;}if(a.click){a.click();return true;}return false;})()",
                    null
                )
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

private fun isVideoRequest(lower: String): Boolean {
    if (lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".webm") ||
        lower.contains(".ts") || lower.contains(".mkv")) return true
    if (lower.contains("/hls/") || lower.contains("m3u8")) return true
    if (lower.contains("format=mp4") || lower.contains("type=video")) return true
    if (lower.endsWith(".m3u8") || lower.endsWith(".mp4") || lower.endsWith(".webm") ||
        lower.endsWith(".ts") || lower.endsWith(".mkv")) return true
    if (lower.contains("googlevideo.com/videoplayback")) return true
    if (lower.contains("/videoplayback")) return true
    return false
}

private fun detectFormat(lower: String): String = when {
    lower.contains(".m3u8") -> "HLS"
    lower.contains(".mp4") -> "MP4"
    lower.contains(".webm") -> "WEBM"
    lower.contains(".mkv") -> "MKV"
    lower.contains(".ts") -> "TS"
    else -> "VIDEO"
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
