package com.tvanime.app.domain.usecase

import com.tvanime.app.domain.model.ContentItem
import com.tvanime.app.data.repository.ContentsRepository

/**
 * Obtiene el detalle de un item por su id.
 */
class GetDetailUseCase(private val repo: ContentsRepository) {
    suspend operator fun invoke(id: String): ContentItem? = repo.getDetail(id)
}
