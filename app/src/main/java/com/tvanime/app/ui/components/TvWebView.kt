package com.tvanime.app.ui.components

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow

class JsInterface(
    private val onVideoDetected: (String) -> Unit,
    private val onVideoLinkClicked: (String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onVideoDetected(url: String) { onVideoDetected(url) }

    @android.webkit.JavascriptInterface
    fun onVideoLinkClicked(url: String) { onVideoLinkClicked(url) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvWebView(
    url: String,
    webViewClient: WebViewClient,
    webChromeClient: android.webkit.WebChromeClient,
    injectedJs: String,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String?) -> Unit,
    onPageLoading: (Boolean) -> Unit,
    onStreamDetected: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }

    val jsInterface = remember {
        JsInterface(
            onVideoDetected = { url -> onStreamDetected(url, detectFormat(url), "direct") },
            onVideoLinkClicked = { url -> onStreamDetected(url, detectFormat(url), "link") }
        )
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.setGeolocationEnabled(false)

                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                setWebViewClient(webViewClient)
                setWebChromeClient(webChromeClient)

                addJavascriptInterface(jsInterface, "AndroidCapture")

                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                simulateCenterClick()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> { simulateScroll(0, 300); true }
                            KeyEvent.KEYCODE_DPAD_UP -> { simulateScroll(0, -300); true }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> { simulateScroll(300, 0); true }
                            KeyEvent.KEYCODE_DPAD_LEFT -> { simulateScroll(-300, 0); true }
                            else -> false
                        }
                    } else false
                }

                setOnFocusChangeListener { _, hasFocus ->
                    focused = hasFocus
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) BorderStroke(4.dp, Brush.linearGradient(listOf(FocusCyan, FocusGlow)))
                else BorderStroke(0.dp, Color.Transparent),
                RoundedCornerShape(12.dp)
            )
            .background(if (focused) FocusBg.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(12.dp)),
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
            webView.evaluateJavascript("""
                if (!window.captureScriptInjected) {
                    window.captureScriptInjected = true;
                    $injectedJs
                }
            """.trimIndent(), null)
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

private fun WebView.simulateScroll(dx: Int, dy: Int) {
    evaluateJavascript("window.scrollBy($dx, $dy);", null)
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