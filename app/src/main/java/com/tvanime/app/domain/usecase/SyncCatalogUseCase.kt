package com.tvanime.app.domain.usecase

import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.data.repository.ContentsRepository

/**
 * Sincroniza el catalogo con los items provenientes de la fuente configurada.
 */
class SyncCatalogUseCase(private val repo: ContentsRepository) {
    suspend operator fun invoke(remoteItems: List<ContentItem>) = repo.syncCatalog(remoteItems)
}
