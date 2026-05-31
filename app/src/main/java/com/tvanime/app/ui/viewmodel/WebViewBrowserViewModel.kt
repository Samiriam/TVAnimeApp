package com.tvanime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.data.capture.WebViewSessionManager
import com.tvanime.app.data.capture.WebViewVideoCapture
import com.tvanime.app.ui.components.VideoStreamUI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebViewBrowserViewModel @Inject constructor(
    private val videoCapture: WebViewVideoCapture,
    private val sessionManager: WebViewSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebViewBrowserState())
    val uiState: StateFlow<WebViewBrowserState> = _uiState.asStateFlow()

    private val _detectedStream = MutableStateFlow<VideoStreamUI?>(null)
    val detectedStream: StateFlow<VideoStreamUI?> = _detectedStream.asStateFlow()

    private val _navigateToPlayer = MutableSharedFlow<PlayerParams>()
    val navigateToPlayer: SharedFlow<PlayerParams> = _navigateToPlayer.asSharedFlow()

    private var historyUrls = mutableListOf<String>()
    private var historyIndex = -1

    init {
        viewModelScope.launch {
            videoCapture.currentUrl.collect { url ->
                _uiState.update { it.copy(currentUrl = url ?: "") }
            }
        }
        viewModelScope.launch {
            videoCapture.pageTitle.collect { title ->
                _uiState.update { it.copy(pageTitle = title) }
            }
        }
        viewModelScope.launch {
            videoCapture.isPageLoading.collect { loading ->
                _uiState.update { it.copy(isLoading = loading) }
            }
        }
    }

    fun onStreamDetected(url: String, format: String, domain: String, referer: String) {
        _detectedStream.value = VideoStreamUI(
            url = url,
            format = format,
            domain = domain,
            referer = referer
        )
        _uiState.update { it.copy(showOverlay = true) }
    }

    fun playStream(url: String, headers: Map<String, String>) {
        viewModelScope.launch {
            _navigateToPlayer.emit(PlayerParams(url, headers))
        }
    }

    fun dismissOverlay() {
        _uiState.update { it.copy(showOverlay = false) }
    }

    fun addToHistory(url: String) {
        if (historyUrls.isNotEmpty() && historyUrls.last() == url) return
        historyUrls.add(url)
        historyIndex = historyUrls.lastIndex
    }

    fun getBackUrl(): String? {
        return if (historyIndex > 0) {
            historyIndex--
            historyUrls.getOrNull(historyIndex)
        } else null
    }

    fun getForwardUrl(): String? {
        return if (historyIndex < historyUrls.lastIndex) {
            historyIndex++
            historyUrls.getOrNull(historyIndex)
        } else null
    }

    fun canGoBack(): Boolean = historyIndex > 0
    fun canGoForward(): Boolean = historyIndex < historyUrls.lastIndex

    fun clearStream() {
        _detectedStream.value = null
        _uiState.update { it.copy(showOverlay = false) }
    }

    fun getCookiesForUrl(url: String): Map<String, String> {
        return sessionManager.getCookiesForUrl(url)
    }

    fun setDefaultUrl(url: String) {
        _uiState.update { it.copy(currentUrl = url) }
    }
}

data class WebViewBrowserState(
    val currentUrl: String = "https://www.animeflv.net",
    val pageTitle: String? = null,
    val isLoading: Boolean = false,
    val showOverlay: Boolean = false
)

data class PlayerParams(
    val url: String,
    val headers: Map<String, String>
)