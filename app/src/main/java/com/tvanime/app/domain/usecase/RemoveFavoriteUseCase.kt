package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.FavoritesRepository

/**
 * Elimina un item de favoritos.
 */
class RemoveFavoriteUseCase(private val repo: FavoritesRepository) {
    suspend operator fun invoke(contentId: String) = repo.remove(contentId)
}
