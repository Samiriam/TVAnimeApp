package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.FavoritesRepository

/**
 * Recupera la lista de favoritos del usuario.
 */
class GetFavoritesUseCase(private val favoritesRepo: FavoritesRepository) {
    operator fun invoke() = favoritesRepo.observeAll()
}
