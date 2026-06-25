package com.tvanime.app.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Build
import android.net.Uri
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

private const val DEFAULT_HOME_URL = "https://www.google.com"
private const val PREFS_NAME = "webcast_prefs"

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
    var webPermissionMessage by remember { mutableStateOf<String?>(null) }
    val webViewHolder = remember { WebViewHolder() }

    var videosFound by remember { mutableStateOf(listOf<DetectedVideo>()) }

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

    fun requestWebViewFocus() {
        webViewHolder.webView?.post {
            webViewHolder.webView?.apply {
                requestFocus()
                requestFocus(View.FOCUS_DOWN)
            }
        }
    }

    LaunchedEffect(Unit) {
        requestWebViewFocus()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AndroidWebView(
                    holder = webViewHolder,
                    url = currentUrl,
                    canGrantWebPermissions = context.hasWebRuntimePermissions(),
                    onUrlChanged = {
                        currentUrl = it
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
                        viewModel.onStreamDetected(
                            url = url,
                            format = format,
                            domain = runCatching { Uri.parse(url).host.orEmpty() }.getOrDefault(""),
                            referer = currentUrl
                        )
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

                VideoCaptureOverlay(
                    stream = detectedStream,
                    isVisible = uiState.showOverlay,
                    onPlayVideo = { url, headers ->
                        onPlayVideo(
                            url,
                            viewModel.getPlaybackHeaders(url, currentUrl) + headers
                        )
                    },
                    onDismiss = {
                        viewModel.dismissOverlay()
                        requestWebViewFocus()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )

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

data class DetectedVideo(val url: String, val format: String, val referer: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndroidWebView(
    holder: WebViewHolder,
    url: String,
    canGrantWebPermissions: Boolean,
    onUrlChanged: (String) -> Unit,
    onVideoDetected: (String, String) -> Unit,
    onPageLoading: (Boolean) -> Unit,
    onTitleChanged: (String?) -> Unit,
    onPermissionRequest: (String) -> Unit
) {
    var webViewError by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 2.dp,
                brush = if (focused) Brush.linearGradient(listOf(FocusCyan, FocusGlow))
                else SolidColor(Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
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
                    setOnFocusChangeListener { _, hasFocus -> focused = hasFocus }

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
                            view?.evaluateJavascript(TV_FOCUS_INJECT_JS, null)
                            view?.requestFocus()
                            view?.requestFocus(View.FOCUS_DOWN)
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
                    post {
                        requestFocus()
                        requestFocus(View.FOCUS_DOWN)
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
    private fun jsHandled(script: String): Boolean {
        var result: String? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        post {
            evaluateJavascript(script) { value ->
                result = value
                latch.countDown()
            }
        }
        return try {
            latch.await(200, java.util.concurrent.TimeUnit.MILLISECONDS)
            "true" == result
        } catch (e: InterruptedException) {
            false
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null || event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (canGoBack()) { goBack(); return true }
                return super.dispatchKeyEvent(event)
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (jsHandled("window.__tvMoveFocus ? window.__tvMoveFocus('up') : false")) return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (jsHandled("window.__tvMoveFocus ? window.__tvMoveFocus('down') : false")) return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (jsHandled("window.__tvMoveFocus ? window.__tvMoveFocus('left') : false")) return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (jsHandled("window.__tvMoveFocus ? window.__tvMoveFocus('right') : false")) return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (jsHandled("window.__tvActivateFocus ? window.__tvActivateFocus() : false")) return true
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

private const val TV_FOCUS_INJECT_JS = """
(function() {
    try {
        var SEL = 'a, button, input, textarea, select, [role=button], [tabindex], video, iframe, [onclick]';
        var IMG_SEL = 'img';
        var CARD_SEL = 'article, .card, .movie, .item, .poster, .video, .thumb, .tile, [class*=card], [class*=movie], [class*=item], [class*=poster], [class*=thumb], [class*=tile]';

        function makeFocusable(el) {
            if (!el || el.nodeType !== 1) return;
            if (el.hasAttribute('tabindex')) {
                if (el.getAttribute('tabindex') === '-1') el.setAttribute('tabindex', '0');
            } else {
                el.setAttribute('tabindex', '0');
            }
            try { el.style.outlineOffset = '3px'; } catch (e) {}
        }

        function processAll() {
            try {
                var nodes = document.querySelectorAll(SEL);
                for (var i = 0; i < nodes.length; i++) makeFocusable(nodes[i]);
                var cards = document.querySelectorAll(CARD_SEL);
                for (var j = 0; j < cards.length; j++) makeFocusable(cards[j]);
                var imgs = document.querySelectorAll(IMG_SEL);
                for (var k = 0; k < imgs.length; k++) {
                    var parent = imgs[k];
                    for (var p = 0; p < 4; p++) {
                        if (!parent) break;
                        if (parent.tagName === 'A' || parent.tagName === 'BUTTON' || parent.onclick) {
                            makeFocusable(parent);
                            break;
                        }
                        parent = parent.parentElement;
                    }
                }
            } catch (e) {}
        }

        function isVisible(el) {
            if (!el || el.nodeType !== 1) return false;
            var r = el.getBoundingClientRect();
            if (r.width < 8 || r.height < 8) return false;
            var s = window.getComputedStyle(el);
            if (!s || s.visibility === 'hidden' || s.display === 'none' || parseFloat(s.opacity || '1') === 0) return false;
            return r.bottom >= 0 && r.right >= 0 && r.top <= window.innerHeight && r.left <= window.innerWidth;
        }

        function candidates() {
            processAll();
            var all = Array.prototype.slice.call(document.querySelectorAll(SEL + ',' + CARD_SEL));
            var seen = [];
            return all.filter(function(el) {
                if (seen.indexOf(el) >= 0) return false;
                seen.push(el);
                return isVisible(el);
            });
        }

        function centerOf(el) {
            var r = el.getBoundingClientRect();
            return {x: r.left + r.width / 2, y: r.top + r.height / 2, r: r};
        }

        function paintFocus(el) {
            try {
                if (window.__tvFocusedElement && window.__tvFocusedElement !== el) {
                    window.__tvFocusedElement.style.outline = window.__tvPrevOutline || '';
                    window.__tvFocusedElement.style.boxShadow = window.__tvPrevBoxShadow || '';
                }
                window.__tvPrevOutline = el.style.outline || '';
                window.__tvPrevBoxShadow = el.style.boxShadow || '';
                el.style.outline = '4px solid #00E5FF';
                el.style.boxShadow = '0 0 0 4px rgba(0,229,255,0.30)';
                window.__tvFocusedElement = el;
            } catch (e) {}
        }

        function focusElement(el) {
            if (!el) return false;
            try {
                el.focus({preventScroll: true});
            } catch (e) {
                try { el.focus(); } catch (ignored) {}
            }
            try { el.scrollIntoView({block: 'center', inline: 'center', behavior: 'smooth'}); } catch (e) {}
            paintFocus(el);
            return true;
        }

        window.__tvMoveFocus = function(dir) {
            var items = candidates();
            if (!items.length) return false;
            var active = document.activeElement;
            if (!active || active === document.body || items.indexOf(active) < 0 || !isVisible(active)) {
                return focusElement(items[0]);
            }
            var a = centerOf(active);
            var best = null;
            var bestScore = Infinity;
            for (var i = 0; i < items.length; i++) {
                var el = items[i];
                if (el === active) continue;
                var c = centerOf(el);
                var dx = c.x - a.x;
                var dy = c.y - a.y;
                if (dir === 'up' && dy >= -8) continue;
                if (dir === 'down' && dy <= 8) continue;
                if (dir === 'left' && dx >= -8) continue;
                if (dir === 'right' && dx <= 8) continue;
                var primary = (dir === 'up' || dir === 'down') ? Math.abs(dy) : Math.abs(dx);
                var secondary = (dir === 'up' || dir === 'down') ? Math.abs(dx) : Math.abs(dy);
                var score = primary * 2 + secondary;
                if (score < bestScore) {
                    bestScore = score;
                    best = el;
                }
            }
            if (best) return focusElement(best);
            if (dir === 'up') window.scrollBy(0, -Math.max(240, window.innerHeight * 0.7));
            if (dir === 'down') window.scrollBy(0, Math.max(240, window.innerHeight * 0.7));
            if (dir === 'left') window.scrollBy(-Math.max(240, window.innerWidth * 0.7), 0);
            if (dir === 'right') window.scrollBy(Math.max(240, window.innerWidth * 0.7), 0);
            return true;
        };

        window.__tvActivateFocus = function() {
            var a = document.activeElement;
            if (!a || a === document.body) {
                var items = candidates();
                if (!items.length) return false;
                a = items[0];
                focusElement(a);
            }
            if (a.tagName === 'VIDEO') {
                try { if (a.paused) a.play(); else a.pause(); return true; } catch(e) {}
            }
            var target = a.closest && a.closest('a[href], button, [role=button], [onclick], article, .card, .movie, .item, .poster, .video, .thumb, .tile');
            if (target && target !== a) a = target;
            if (a.tagName === 'A' && a.href) {
                window.location.href = a.href;
                return true;
            }
            if (a.click) {
                a.click();
                return true;
            }
            return false;
        };

        processAll();
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', processAll);
        }
        window.addEventListener('load', processAll);
        try {
            var mo = new MutationObserver(function() {
                clearTimeout(window.__tvFocusTimer);
                window.__tvFocusTimer = setTimeout(processAll, 250);
            });
            mo.observe(document.body, {childList: true, subtree: true});
        } catch (e) {}
        return true;
    } catch (e) {
        return false;
    }
})();
"""
