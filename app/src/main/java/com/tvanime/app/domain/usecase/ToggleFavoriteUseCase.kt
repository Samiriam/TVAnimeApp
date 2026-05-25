package com.tvanime.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import com.tvanime.app.data.repository.FavoritesRepository

/**
 * Alterna el estado de favorito de un item.
 */
class ToggleFavoriteUseCase(private val repo: FavoritesRepository) {
    suspend operator fun invoke(contentId: String, isFavorite: Boolean) {
        if (isFavorite) repo.add(contentId) else repo.remove(contentId)
    }
}
