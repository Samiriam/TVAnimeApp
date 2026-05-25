package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.FavoritesRepository

/**
 * Agrega un item a favoritos.
 */
class AddFavoriteUseCase(private val repo: FavoritesRepository) {
    suspend operator fun invoke(contentId: String) = repo.add(contentId)
}
