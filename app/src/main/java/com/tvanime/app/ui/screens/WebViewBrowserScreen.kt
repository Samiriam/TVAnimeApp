package com.tvanime.app.ui.screens

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvanime.app.ui.components.UrlBar
import com.tvanime.app.ui.components.VideoCaptureOverlay
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow
import com.tvanime.app.ui.viewmodel.WebViewBrowserViewModel

@Composable
fun WebViewBrowserScreen(
    initialUrl: String? = null,
    onBack: () -> Unit,
    onPlayVideo: (String, Map<String, String>) -> Unit,
    viewModel: WebViewBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val detectedStream by viewModel.detectedStream.collectAsState()

    var showSiteSelector by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.setDefaultUrl(initialUrl)
            showSiteSelector = false
        }
    }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var backFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .focusable()
                        .onFocusChanged { backFocused = it.isFocused }
                        .border(
                            if (backFocused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                            else BorderStroke(0.dp, Color.Transparent),
                            RoundedCornerShape(10.dp)
                        )
                        .background(if (backFocused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", Modifier.size(26.dp), tint = Color.White)
                }

                Text(
                    "Navegador Web",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                var selectorFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showSiteSelector = !showSiteSelector },
                    modifier = Modifier
                        .size(48.dp)
                        .focusable()
                        .onFocusChanged { selectorFocused = it.isFocused }
                        .border(
                            if (selectorFocused) BorderStroke(3.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                            else BorderStroke(0.dp, Color.Transparent),
                            RoundedCornerShape(10.dp)
                        )
                        .background(if (selectorFocused) FocusBg else Color.Transparent, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Sitios",
                        modifier = Modifier.size(26.dp),
                        tint = if (selectorFocused) FocusCyan else Color.White
                    )
                }
            }

            AnimatedVisibility(visible = !showSiteSelector) {
                UrlBar(
                    currentUrl = uiState.currentUrl,
                    onUrlChanged = {},
                    onNavigate = { url ->
                        webViewRef?.loadUrl(url)
                        viewModel.addToHistory(url)
                        showSiteSelector = false
                    },
                    onBack = { webViewRef?.goBack() },
                    onForward = { webViewRef?.goForward() },
                    onRefresh = { webViewRef?.reload() },
                    onHomeClick = { showSiteSelector = true },
                    canGoBack = webViewRef?.canGoBack() == true,
                    canGoForward = webViewRef?.canGoForward() == true,
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
                        webViewRef?.loadUrl(url)
                        viewModel.addToHistory(url)
                        showSiteSelector = false
                    }
                )
            }

            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                AndroidWebView(
                    url = if (showSiteSelector) "about:blank" else uiState.currentUrl,
                    onUrlChanged = { viewModel.addToHistory(it) },
                    onStreamDetected = { url, format, domain ->
                        viewModel.onStreamDetected(url, format, domain, uiState.currentUrl)
                    },
                    onPageLoading = {},
                    onTitleChanged = {},
                    onWebViewReady = { webViewRef = it }
                )

                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = FocusCyan,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        VideoCaptureOverlay(
            stream = detectedStream,
            isVisible = uiState.showOverlay,
            onPlayVideo = { url, headers ->
                onPlayVideo(url, headers)
                viewModel.dismissOverlay()
            },
            onDismiss = { viewModel.dismissOverlay() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun SiteSelectorPanel(onSiteSelected: (String) -> Unit) {
    val sites = listOf(
        SiteGroup("Anime", listOf(
            SiteItem("AnimeFLV", "https://www3.animeflv.net", "🎌"),
            SiteItem("JKAnime", "https://jkanime.net", "🎌"),
            SiteItem("AnimeFenix", "https://www.animefenix.tv", "🎌"),
            SiteItem("MonosChinos", "https://monoschinos2.com", "🎌"),
            SiteItem("TioAnime", "https://tioanime.com", "🎌"),
        )),
        SiteGroup("Peliculas", listOf(
            SiteItem("Cuevana3", "https://cuevana3.ch", "🎬"),
            SiteItem("PelisPlus", "https://pelisplus.me", "🎬"),
            SiteItem("Pelisflix", "https://pelisflix.media", "🎬"),
        )),
        SiteGroup("Series", listOf(
            SiteItem("SeriesFLV", "https://seriesflv.net", "📺"),
            SiteItem("DoramasMP4", "https://doramasmmp4.com", "🎭"),
            SiteItem("DoramasFlix", "https://doramaflix.com", "🎭"),
        )),
        SiteGroup("Otros", listOf(
            SiteItem("Archive.org", "https://archive.org", "📚"),
            SiteItem("YouTube", "https://youtube.com", "▶️"),
        ))
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(280.dp).padding(horizontal = 16.dp),
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
    var focused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.03f else 1f)
            .border(
                if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(14.dp)
            )
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (focused) 1f else 0.6f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = site.icon, style = MaterialTheme.typography.headlineMedium)
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
    url: String,
    onUrlChanged: (String) -> Unit,
    onStreamDetected: (String, String, String) -> Unit,
    onPageLoading: (Boolean) -> Unit,
    onTitleChanged: (String?) -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    var webViewError by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
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

                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

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
                }

                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                    }
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onVideoDetected(url: String) {
                        val format = detectFormat(url)
                        post { onStreamDetected(url, format, "") }
                    }
                }, "AndroidCapture")

                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                simulateCenterClick()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> { scrollBy(0, 300); true }
                            KeyEvent.KEYCODE_DPAD_UP -> { scrollBy(0, -300); true }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> { scrollBy(300, 0); true }
                            KeyEvent.KEYCODE_DPAD_LEFT -> { scrollBy(-300, 0); true }
                            else -> false
                        }
                    } else false
                }
            }.also { onWebViewReady(it) }
        },
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 3.dp,
                color = if (webViewError != null) Color.Red.copy(alpha = 0.5f) else FocusCyan.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        update = { webView ->
            if (webView.url != url && !url.startsWith("about:blank")) {
                webView.loadUrl(url)
            }
        }
    )
}

private fun WebView.simulateCenterClick() {
    evaluateJavascript("""
        (function() {
            var focused = document.activeElement;
            if (focused) {
                if (focused.tagName === 'VIDEO') {
                    focused.play();
                } else if (focused.tagName === 'A' && focused.href) {
                    window.location.href = focused.href;
                } else if (focused.click) {
                    focused.click();
                }
            }
        })();
    """.trimIndent(), null)
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