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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvanime.app.ui.components.UrlBar
import com.tvanime.app.ui.components.VideoCaptureOverlay
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow
import com.tvanime.app.ui.viewmodel.WebViewBrowserViewModel

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
    var showSiteSelector by remember { mutableStateOf(false) }
    var webPermissionMessage by remember { mutableStateOf<String?>(null) }
    val webViewHolder = remember { WebViewHolder() }
    val webViewFocusRequester = remember { FocusRequester() }

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
        val wv = webViewHolder.webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBar(
                onBack = onBack,
                onToggleSelector = { showSiteSelector = !showSiteSelector },
                selectorOpen = showSiteSelector
            )

            AnimatedVisibility(visible = !showSiteSelector) {
                UrlBar(
                    currentUrl = currentUrl,
                    onUrlChanged = { currentUrl = it },
                    onNavigate = { url ->
                        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                        currentUrl = normalized
                        viewModel.setDefaultUrl(normalized)
                        viewModel.addToHistory(normalized)
                    },
                    onBack = { webViewHolder.webView?.goBack() },
                    onForward = { webViewHolder.webView?.goForward() },
                    onRefresh = { webViewHolder.webView?.reload() },
                    onHomeClick = {
                        currentUrl = DEFAULT_HOME_URL
                        viewModel.setDefaultUrl(DEFAULT_HOME_URL)
                    },
                    canGoBack = webViewHolder.webView?.canGoBack() == true,
                    canGoForward = webViewHolder.webView?.canGoForward() == true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showSiteSelector,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SiteSelectorPanel(
                    onSiteSelected = { url ->
                        currentUrl = url
                        viewModel.setDefaultUrl(url)
                        viewModel.addToHistory(url)
                        showSiteSelector = false
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                AndroidWebView(
                    holder = webViewHolder,
                    url = currentUrl,
                    focusRequester = webViewFocusRequester,
                    canGrantWebPermissions = context.hasWebRuntimePermissions(),
                    onUrlChanged = {
                        currentUrl = it
                        viewModel.setDefaultUrl(it)
                        viewModel.addToHistory(it)
                    },
                    onStreamDetected = { url, format, domain ->
                        viewModel.onStreamDetected(url, format, domain, currentUrl)
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

        VideoCaptureOverlay(
            stream = detectedStream,
            isVisible = uiState.showOverlay,
            onPlayVideo = { url, headers ->
                val mergedHeaders = viewModel.getPlaybackHeaders(url, detectedStream?.referer.orEmpty()) + headers
                onPlayVideo(url, mergedHeaders)
                viewModel.dismissOverlay()
            },
            onDismiss = { viewModel.dismissOverlay() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun HeaderBar(
    onBack: () -> Unit,
    onToggleSelector: () -> Unit,
    selectorOpen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
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
                "Navega con el control, reproduce en la pagina y espera el aviso de stream detectado.",
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
private fun SiteSelectorPanel(onSiteSelected: (String) -> Unit) {
    val sites = listOf(
        SiteGroup("Fuentes de prueba", listOf(
            SiteItem("Archive.org", "https://archive.org", "WEB"),
            SiteItem("Video test HLS", "https://test-streams.mux.dev", "HLS"),
            SiteItem("Google", "https://www.google.com", "BUSCAR"),
        )),
        SiteGroup("Entrada manual", listOf(
            SiteItem("Escribir URL arriba", "https://", "URL"),
            SiteItem("Buscar contenido publico", "https://www.google.com/search?q=public+domain+video", "BUSCAR"),
        ))
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).padding(horizontal = 24.dp),
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndroidWebView(
    holder: WebViewHolder,
    url: String,
    focusRequester: FocusRequester,
    canGrantWebPermissions: Boolean,
    onUrlChanged: (String) -> Unit,
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
                else SolidColor(Color.White.copy(alpha = 0.2f)),
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

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    evaluateJavascript(CLICK_ACTIVE_JS, null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    evaluateJavascript("window.scrollBy(0, -400);", null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    evaluateJavascript("window.scrollBy(0, 400);", null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    evaluateJavascript("window.scrollBy(-400, 0);", null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    evaluateJavascript("window.scrollBy(400, 0);", null)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
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

private const val CLICK_ACTIVE_JS = """
    (function() {
        var a = document.activeElement;
        if (!a || a === document.body) {
            var els = document.querySelectorAll('a, button, [role=button], video, [tabindex]');
            if (els.length > 0) {
                var best = null;
                var bestY = -1;
                for (var i = 0; i < els.length; i++) {
                    var r = els[i].getBoundingClientRect();
                    if (r.top < window.innerHeight && r.bottom > 0 && r.left < window.innerWidth && r.right > 0) {
                        if (r.top > bestY) { bestY = r.top; best = els[i]; }
                    }
                }
                if (best) {
                    if (best.tagName === 'VIDEO') { best.play(); }
                    else if (best.click) { best.click(); }
                    return;
                }
            }
            return;
        }
        if (a.tagName === 'VIDEO') { a.play(); }
        else if (a.tagName === 'A' && a.href) { window.location.href = a.href; }
        else if (a.click) { a.click(); }
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
