package com.tvanime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.domain.model.ExtractionResult
import com.tvanime.app.domain.usecase.ExtractMediaFromPageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExtractMediaUiState(
    val pageUrl: String = "",
    val isLoading: Boolean = false,
    val result: ExtractionResult? = null,
    val error: String? = null
)

@HiltViewModel
class ExtractMediaViewModel @Inject constructor(
    private val extractMediaFromPageUseCase: ExtractMediaFromPageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractMediaUiState())
    val uiState: StateFlow<ExtractMediaUiState> = _uiState.asStateFlow()

    fun updatePageUrl(value: String) {
        _uiState.value = _uiState.value.copy(pageUrl = value, error = null)
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
