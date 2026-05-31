package com.tvanime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.domain.model.ExtractionResult
import com.tvanime.app.domain.usecase.ExtractMediaFromPageUseCase
import com.tvanime.app.ui.components.POPULAR_SITES
import com.tvanime.app.ui.components.SiteSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExtractMediaUiState(
    val pageUrl: String = "",
    val isLoading: Boolean = false,
    val result: ExtractionResult? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val showSuggestions: Boolean = true,
    val autoAnalyzeProgress: String? = null
)

@HiltViewModel
class ExtractMediaViewModel @Inject constructor(
    private val extractMediaFromPageUseCase: ExtractMediaFromPageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractMediaUiState())
    val uiState: StateFlow<ExtractMediaUiState> = _uiState.asStateFlow()

    fun updatePageUrl(value: String) {
        _uiState.value = _uiState.value.copy(
            pageUrl = value,
            error = null,
            showSuggestions = value.isBlank()
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSuggestions(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSuggestions = show)
    }

    fun selectSite(site: SiteSuggestion) {
        _uiState.value = _uiState.value.copy(
            pageUrl = site.url,
            showSuggestions = false,
            searchQuery = ""
        )
    }

    fun autoAnalyzePopularSites() {
        viewModelScope.launch {
            val sitesToAnalyze = POPULAR_SITES.take(5)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                autoAnalyzeProgress = "Analizando ${sitesToAnalyze.size} sitios populares..."
            )

            val allResults = mutableListOf<com.tvanime.app.domain.model.DetectedMedia>()

            for ((index, site) in sitesToAnalyze.withIndex()) {
                _uiState.value = _uiState.value.copy(
                    autoAnalyzeProgress = "Analizando ${index + 1}/${sitesToAnalyze.size}: ${site.name}..."
                )

                runCatching {
                    extractMediaFromPageUseCase(site.url)
                }.onSuccess { result ->
                    allResults.addAll(result.candidates.take(3))
                }

                delay(500)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                autoAnalyzeProgress = null,
                result = ExtractionResult(
                    pageUrl = "auto-analyze",
                    sourceHost = "Múltiples sitios",
                    title = "Resultados automáticos",
                    candidates = allResults
                )
            )
        }
    }

    fun extract() {
        val currentUrl = _uiState.value.pageUrl.trim()
        if (currentUrl.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Ingresa una URL para analizar.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)
            runCatching {
                extractMediaFromPageUseCase(currentUrl)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(isLoading = false, result = result)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "No se pudo analizar la pagina.")
            }
        }
    }
}
