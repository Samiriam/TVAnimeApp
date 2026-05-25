package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.ContentsRepository

/**
 * Recupera el catalogo completo desde la fuente configurada.
 */
class GetCatalogUseCase(private val repo: ContentsRepository) {
    operator fun invoke() = repo.observeCatalog()
}
