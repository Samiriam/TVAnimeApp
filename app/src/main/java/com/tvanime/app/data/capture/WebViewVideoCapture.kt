package com.tvanime.app.data.capture

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewVideoCapture @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _detectedStream = MutableStateFlow<VideoStream?>(null)
    val detectedStream: StateFlow<VideoStream?> = _detectedStream.asStateFlow()

    private val _isPageLoading = MutableStateFlow(false)
    val isPageLoading: StateFlow<Boolean> = _isPageLoading.asStateFlow()

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow<String?>(null)
    val pageTitle: StateFlow<String?> = _pageTitle.asStateFlow()

    fun onStreamDetected(stream: VideoStream) {
        _detectedStream.value = stream
    }

    fun clearStream() {
        _detectedStream.value = null
    }

    @SuppressLint("AddJavascriptInterface")
    fun createWebViewClient(onStreamDetected: (VideoStream) -> Unit): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                val lower = url.lowercase()

                if (lower.contains(".m3u8") || lower.contains(".mp4") ||
                    lower.contains(".webm") || lower.contains(".ts") ||
                    lower.contains(".mkv") || lower.contains("manifest") ||
                    lower.contains("segment") || lower.contains("playlist.m3u8") ||
                    lower.contains("chunk")) {

                    val host = request.url.host ?: ""
                    val referer = view?.url ?: ""
                    val format = detectFormat(lower)

                    Handler(Looper.getMainLooper()).post {
                        onStreamDetected(
                            VideoStream(
                                url = url,
                                format = format,
                                domain = host,
                                referer = referer
                            )
                        )
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                _currentUrl.value = url
                _isPageLoading.value = true
                _detectedStream.value = null
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                _isPageLoading.value = false
                _currentUrl.value = url
                _pageTitle.value = view?.title
            }
        }
    }

    @SuppressLint("AddJavascriptInterface")
    fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                _pageTitle.value = title
            }
        }
    }

    fun getVideoDetectionScript(): String = """
        (function() {
            function setupVideoListeners(video) {
                video.addEventListener('play', function() {
                    var src = video.src || video.currentSrc || '';
                    if (src && !src.startsWith('blob:') && src.length > 10) {
                        window.AndroidCapture.onVideoDetected(src);
                    }
                });

                video.addEventListener('loadedmetadata', function() {
                    var src = video.src || video.currentSrc || '';
                    if (src && !src.startsWith('blob:') && src.length > 10) {
                        window.AndroidCapture.onVideoDetected(src);
                    }
                });

                var sources = video.querySelectorAll('source');
                sources.forEach(function(src) {
                    var url = src.src || src.getAttribute('data-src');
                    if (url && !url.startsWith('blob:') && url.length > 10) {
                        window.AndroidCapture.onVideoDetected(url);
                    }
                });
            }

            function findVideos() {
                var videos = document.querySelectorAll('video');
                videos.forEach(setupVideoListeners);

                document.querySelectorAll('a[href*=".m3u8"], a[href*=".mp4"], a[href*=".webm"]').forEach(function(link) {
                    link.addEventListener('click', function(e) {
                        var href = link.href;
                        if (href && href.length > 10) {
                            window.AndroidCapture.onVideoLinkClicked(href);
                        }
                    });
                });
            }

            window.AndroidCapture = {
                onVideoDetected: function(url) {
                    if (url && !url.startsWith('blob:') && url.length > 10) {
                        window.captureStream && window.captureStream(url);
                    }
                },
                onVideoLinkClicked: function(url) {
                    if (url && url.length > 10) {
                        window.captureStream && window.captureStream(url);
                    }
                },
                getPageVideos: function() {
                    var videos = [];
                    document.querySelectorAll('video').forEach(function(v) {
                        var src = v.src || v.currentSrc || '';
                        if (src && !src.startsWith('blob:')) videos.push(src);
                    });
                    return videos;
                }
            };

            var observer = new MutationObserver(function(mutations) {
                mutations.forEach(function(m) {
                    m.addedNodes.forEach(function(node) {
                        if (node.nodeName === 'VIDEO') {
                            setupVideoListeners(node);
                        }
                        if (node.querySelectorAll) {
                            node.querySelectorAll('video').forEach(setupVideoListeners);
                        }
                    });
                });
            });

            var initInterval = setInterval(function() {
                if (document.readyState === 'complete') {
                    clearInterval(initInterval);
                    findVideos();
                    observer.observe(document.body, { childList: true, subtree: true });
                }
            }, 500);
        })();
    """.trimIndent()

    private fun detectFormat(url: String): String {
        return when {
            url.contains(".m3u8") -> "HLS"
            url.contains(".mp4") -> "MP4"
            url.contains(".webm") -> "WEBM"
            url.contains(".mkv") -> "MKV"
            url.contains(".ts") -> "TS"
            else -> "UNKNOWN"
        }
    }
}

data class VideoStream(
    val url: String,
    val format: String,
    val domain: String,
    val referer: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isPlayable: Boolean
        get() = url.isNotBlank() && (url.contains(".m3u8") || url.contains(".mp4") ||
                url.contains(".webm") || url.contains(".ts") || url.contains(".mkv"))
}