package com.tvanime.app.data.extraction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())

    suspend fun fetchPage(url: String, timeoutMs: Long = 15_000): WebViewResult {
        return suspendCancellableCoroutine { continuation ->
            var webView: WebView? = null
            val completed = AtomicBoolean(false)

            fun complete(result: WebViewResult) {
                if (completed.compareAndSet(false, true)) {
                    handler.post {
                        try { webView?.destroy() } catch (_: Exception) {}
                    }
                    continuation.resumeWith(Result.success(result))
                }
            }

            handler.post {
                try {
                    webView = WebView(context).also { wv ->
                        setupWebView(wv, url)
                    }

                    val wv = webView!!

                    wv.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            super.onPageFinished(view, pageUrl)
                            handler.postDelayed({
                                val html = wv.evaluateJavascriptSync("document.documentElement.outerHTML")
                                val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrDefault("")
                                if (html != null && html.length > 500) {
                                    complete(WebViewResult(html = html, cookies = cookies, finalUrl = pageUrl ?: url, success = true))
                                } else if (!completed.get()) {
                                    handler.postDelayed({
                                        val retryHtml = wv.evaluateJavascriptSync("document.documentElement.outerHTML")
                                        val retryCookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrDefault("")
                                        complete(WebViewResult(
                                            html = retryHtml ?: html ?: "",
                                            cookies = retryCookies,
                                            finalUrl = pageUrl ?: url,
                                            success = (retryHtml?.length ?: 0) > 500
                                        ))
                                    }, 3000)
                                }
                            }, 2000)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                complete(WebViewResult(html = "", cookies = "", finalUrl = url, success = false, error = "HTTP ${error?.errorCode}: ${error?.description}"))
                            }
                        }
                    }

                    wv.webChromeClient = WebChromeClient()
                    wv.loadUrl(url)

                    handler.postDelayed({
                        if (!completed.get()) {
                            val html = wv.evaluateJavascriptSync("document.documentElement.outerHTML") ?: ""
                            val cookies = runCatching { CookieManager.getInstance().getCookie(url) }.getOrDefault("")
                            complete(WebViewResult(
                                html = html,
                                cookies = cookies,
                                finalUrl = url,
                                success = html.length > 500,
                                error = if (html.length <= 500) "Timeout" else null
                            ))
                        }
                    }, timeoutMs)

                } catch (e: Exception) {
                    complete(WebViewResult(html = "", cookies = "", finalUrl = url, success = false, error = e.message))
                }
            }

            continuation.invokeOnCancellation {
                handler.post {
                    try { webView?.destroy() } catch (_: Exception) {}
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView, url: String) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = CHROME_UA
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            blockNetworkImage = true
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        wv.clearCache(true)
        wv.clearHistory()
    }

    private fun WebView.evaluateJavascriptSync(script: String): String? {
        val result = mutableListOf<String?>()
        val latch = java.util.concurrent.CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            evaluateJavascript(script) { res ->
                result.add(res)
                latch.countDown()
            }
        }
        latch.await(5, TimeUnit.SECONDS)
        return result.firstOrNull()?.removeSurrounding("\"")?.replace("\\u003c", "<")
            ?.replace("\\u003e", ">")?.replace("\\u0026", "&")?.replace("\\/", "/")
    }

    data class WebViewResult(
        val html: String,
        val cookies: String,
        val finalUrl: String,
        val success: Boolean,
        val error: String? = null
    )

    companion object {
        private const val CHROME_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }
}