package com.tvanime.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.domain.usecase.GetDetailUseCase
import com.tvanime.app.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados de la pantalla de detalle.
 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Ready(val item: ContentItem, val isFavorite: Boolean) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDetailUseCase: GetDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val contentId: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("contentId")))

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val item = getDetailUseCase(contentId)
                if (item != null) {
                    _uiState.value = DetailUiState.Ready(item, isFavorite = false)
                } else {
                    _uiState.value = DetailUiState.Error("Contenido no encontrado")
                }
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun onToggleFavorite(currentIsFavorite: Boolean) {
        viewModelScope.launch {
            toggleFavoriteUseCase(contentId, !currentIsFavorite)
            if (_uiState.value is DetailUiState.Ready) {
                val current = _uiState.value as DetailUiState.Ready
                _uiState.value = current.copy(isFavorite = !currentIsFavorite)
            }
        }
    }
}
